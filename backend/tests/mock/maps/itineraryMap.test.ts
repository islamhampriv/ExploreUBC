import express, { type Express } from 'express';

import {
  createApp as createBackendApp,
  type AppDependencies,
} from '../../../src/index';
import type { ItineraryRepository } from '../../../src/itineraries/itineraryRepository';
import type { Itinerary } from '../../../src/itineraries/itineraryTypes';
import type { ItineraryMapRepository } from '../../../src/maps/itineraryMapRepository';
import { createItineraryMapRouter } from '../../../src/maps/itineraryMapRouter';
import type { ItineraryMapService } from '../../../src/maps/itineraryMapService';
import type { MapProvider } from '../../../src/maps/mapProvider';
import type {
  GeocodedLocation,
  ItineraryMap,
  TravelOption,
} from '../../../src/maps/mapTypes';
import {
  authenticatedAgent,
  createPathSessionValidator,
} from '../../support/authTestUtils';

const USER_ID = '6705f0000000000000000001';
const ITINERARY_ID = '6705f1000000000000000001';
const MISSING_ITINERARY_ID = '6705f1000000000000000002';
const MAP_PATH =
  `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/map`;

const BASE_ITINERARY: Itinerary = {
  id: ITINERARY_ID,
  ownerId: USER_ID,
  name: 'Map test itinerary',
  items: [],
  createdAt: '2026-10-12T12:00:00.000Z',
  updatedAt: '2026-10-12T12:00:00.000Z',
  saved: true,
  inRecommendation: false,
  score: {},
};

const CACHED_MAP: ItineraryMap = {
  itineraryId: ITINERARY_ID,
  ownerId: USER_ID,
  itineraryUpdatedAt: BASE_ITINERARY.updatedAt,
  markers: [],
  routes: [],
  unmappedActivities: [],
  generatedAt: '2026-10-12T12:05:00.000Z',
};

function createApp(dependencies: Omit<AppDependencies, 'sessionService'>): Express {
  return createBackendApp({
    ...dependencies,
    sessionService: createPathSessionValidator(),
  });
}

const request = (app: Express) => authenticatedAgent(app);

function createItineraryRepository(): jest.Mocked<ItineraryRepository> {
  return {
    insertItinerary: jest.fn(),
    findItineraryDetails: jest.fn(),
    findItinerary: jest.fn(),
    findItineraries: jest.fn(),
    updateItineraryDetails: jest.fn(),
    insertEmbeddedActivity: jest.fn(),
    insertEmbeddedTransportationSegment: jest.fn(),
    removeEmbeddedActivity: jest.fn(),
    removeEmbeddedTransportationSegment: jest.fn(),
    updateEmbeddedActivity: jest.fn(),
    updateEmbeddedTransportationSegment: jest.fn(),
    removeItinerary: jest.fn(),
  };
}

function createMapRepository(
  cachedMap: ItineraryMap | null = null,
): jest.Mocked<ItineraryMapRepository> {
  return {
    findByItineraryId: jest.fn().mockResolvedValue(cachedMap),
    upsertMapData: jest.fn().mockResolvedValue(undefined),
    deleteByItineraryId: jest.fn().mockResolvedValue(false),
  };
}

function createMapProvider(): jest.Mocked<MapProvider> {
  return {
    geocodeAddress: jest.fn(),
    getRoute: jest.fn(),
  };
}

describe('GET /api/v1/users/:userId/itineraries/:itineraryId/map — mocks', () => {
  test('returns a current cached map without calling the provider', async () => {
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(BASE_ITINERARY);
    const maps = createMapRepository(CACHED_MAP);
    const provider = createMapProvider();
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap).toEqual(CACHED_MAP);
    expect(provider.geocodeAddress).not.toHaveBeenCalled();
    expect(provider.getRoute).not.toHaveBeenCalled();
    expect(maps.upsertMapData).not.toHaveBeenCalled();
  });

  test('accepts a cache with an unresolved starting location and no activities', async () => {
    const itinerary = {
      ...BASE_ITINERARY,
      startingLocation: 'Unknown origin',
    };
    const cachedMap: ItineraryMap = {
      ...CACHED_MAP,
      unmappedActivities: [{
        activityId: 'starting-location',
        title: 'Starting location',
        address: 'Unknown origin',
      }],
    };
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(itinerary);
    const maps = createMapRepository(cachedMap);
    const provider = createMapProvider();
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap).toEqual(cachedMap);
    expect(provider.geocodeAddress).not.toHaveBeenCalled();
  });

  test('accepts a cache when the final activity has no marker', async () => {
    const itinerary: Itinerary = {
      ...BASE_ITINERARY,
      startingLocation: 'Known origin',
      items: [{
        id: 'unmapped-activity',
        type: 'activity',
        name: 'Unmapped',
        image: '',
        category: 'other',
        location: 'Unknown',
        startTime: '2027-11-30T17:00:00.000Z',
        endTime: '2027-11-30T18:00:00.000Z',
      }],
    };
    const cachedMap: ItineraryMap = {
      ...CACHED_MAP,
      markers: [{
        activityId: 'starting-location',
        title: 'Starting location',
        address: 'Known origin',
        placeId: 'origin',
        coordinates: { latitude: 49.26, longitude: -123.25 },
        isApproximate: false,
      }],
      unmappedActivities: [{
        activityId: 'unmapped-activity',
        title: 'Unmapped',
        address: 'Unknown',
      }],
    };
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(itinerary);
    const provider = createMapProvider();
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: createMapRepository(cachedMap),
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap).toEqual(cachedMap);
    expect(provider.geocodeAddress).not.toHaveBeenCalled();
  });

  test('regenerates a legacy cache without both travel options', async () => {
    const legacyMap = {
      ...CACHED_MAP,
      routes: [{
        fromActivityId: 'activity-1',
        toActivityId: 'activity-2',
        transportationMode: 'driving',
        encodedPolyline: 'legacy',
        distanceMeters: 100,
        durationSeconds: 60,
        driving: travelOption('driving'),
      }],
    } as unknown as ItineraryMap;
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(BASE_ITINERARY);
    const maps = createMapRepository(legacyMap);
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: createMapProvider(),
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap.routes).toEqual([]);
    expect(maps.upsertMapData).toHaveBeenCalled();
  });

  test('regenerates a cache missing the return route', async () => {
    const itinerary: Itinerary = {
      ...BASE_ITINERARY,
      startingLocation: 'Origin',
      items: [{
        id: 'activity-1',
        type: 'activity',
        name: 'Stop',
        image: '',
        category: 'other',
        location: 'Stop address',
        startTime: '2027-11-30T17:00:00.000Z',
        endTime: '2027-11-30T18:00:00.000Z',
      }],
    };
    const marker = (activityId: string, address: string) => ({
      activityId,
      title: activityId,
      address,
      placeId: activityId,
      coordinates: { latitude: 49.26, longitude: -123.25 },
      isApproximate: false,
    });
    const incompleteMap: ItineraryMap = {
      ...CACHED_MAP,
      markers: [
        marker('starting-location', 'Origin'),
        marker('activity-1', 'Stop address'),
      ],
    };
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(itinerary);
    const maps = createMapRepository(incompleteMap);
    const provider = createMapProvider();
    provider.geocodeAddress.mockImplementation(async (address) =>
      geocodedLocation(address),
    );
    provider.getRoute.mockResolvedValue(null);
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(provider.geocodeAddress).toHaveBeenCalled();
    expect(maps.upsertMapData).toHaveBeenCalled();
  });

  test('generates markers and both travel options, then caches the map', async () => {
    const itinerary: Itinerary = {
      ...BASE_ITINERARY,
      startingLocation: 'UBC Bus Exchange',
      items: [
        {
          id: 'activity-2',
          type: 'activity',
          name: 'Second stop',
          image: '',
          category: 'outdoors',
          location: 'Main Mall',
          startTime: '2027-11-30T19:00:00.000Z',
          endTime: '2027-11-30T20:00:00.000Z',
        },
        {
          id: 'transport-1',
          type: 'transportation',
          mode: 'walking',
          startLocation: 'Museum',
          endLocation: 'Main Mall',
          distanceMeters: 500,
          startTime: '2027-11-30T18:00:00.000Z',
          endTime: '2027-11-30T18:30:00.000Z',
        },
        {
          id: 'activity-1',
          type: 'activity',
          name: 'First stop',
          image: '',
          category: 'art',
          location: 'Museum',
          startTime: '2027-11-30T17:00:00.000Z',
          endTime: '2027-11-30T18:00:00.000Z',
        },
      ],
    };
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(itinerary);
    const maps = createMapRepository();
    const provider = createMapProvider();
    provider.geocodeAddress.mockImplementation(async (address) =>
      geocodedLocation(address),
    );
    provider.getRoute.mockImplementation(async (_origin, _destination, mode) =>
      travelOption(mode),
    );
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap.markers.map(
      (marker: { activityId: string }) => marker.activityId,
    )).toEqual(['starting-location', 'activity-1', 'activity-2']);
    expect(response.body.itineraryMap.routes).toHaveLength(3);
    expect(response.body.itineraryMap.routes[1]).toMatchObject({
      fromActivityId: 'activity-1',
      toActivityId: 'activity-2',
      transportationMode: 'walking',
      encodedPolyline: 'walking-polyline',
    });
    expect(response.body.itineraryMap.routes[2]).toMatchObject({
      fromActivityId: 'activity-2',
      toActivityId: 'starting-location',
      transportationMode: 'driving',
    });
    expect(provider.getRoute).toHaveBeenCalledTimes(6);
    expect(maps.upsertMapData).toHaveBeenCalledWith(
      expect.objectContaining({
        itineraryId: ITINERARY_ID,
        ownerId: USER_ID,
      }),
    );
  });

  test('reports an unresolved activity and omits routes to it', async () => {
    const itinerary: Itinerary = {
      ...BASE_ITINERARY,
      items: [{
        id: 'unmapped',
        type: 'activity',
        name: 'Unknown stop',
        image: '',
        category: 'other',
        location: 'No matching address',
        startTime: '2027-11-30T17:00:00.000Z',
        endTime: '2027-11-30T18:00:00.000Z',
      }],
    };
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(itinerary);
    const maps = createMapRepository();
    const provider = createMapProvider();
    provider.geocodeAddress.mockResolvedValue(null);
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap.markers).toEqual([]);
    expect(response.body.itineraryMap.routes).toEqual([]);
    expect(response.body.itineraryMap.unmappedActivities).toEqual([{
      activityId: 'unmapped',
      title: 'Unknown stop',
      address: 'No matching address',
    }]);
    expect(provider.getRoute).not.toHaveBeenCalled();
  });

  test('falls back to stale cached data when regeneration fails', async () => {
    const itinerary = {
      ...BASE_ITINERARY,
      updatedAt: '2026-10-13T12:00:00.000Z',
      startingLocation: 'UBC',
    };
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(itinerary);
    const maps = createMapRepository(CACHED_MAP);
    const provider = createMapProvider();
    provider.geocodeAddress.mockRejectedValue(new Error('provider unavailable'));
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap).toEqual(CACHED_MAP);
    expect(maps.upsertMapData).not.toHaveBeenCalled();
  });

  test('returns not found when the itinerary does not exist', async () => {
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(null);
    const maps = createMapRepository();
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: createMapProvider(),
    });

    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${MISSING_ITINERARY_ID}/map`,
    );

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
    expect(maps.findByItineraryId).not.toHaveBeenCalled();
  });

  test('returns an internal error when generation fails without a cache', async () => {
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue({
      ...BASE_ITINERARY,
      startingLocation: 'UBC',
    });
    const maps = createMapRepository();
    const provider = createMapProvider();
    provider.geocodeAddress.mockRejectedValue(new Error('provider unavailable'));
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: maps,
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(500);
    expect(response.body.error.code).toBe('INTERNAL_SERVER_ERROR');
  });

  test('keeps unresolved origins and skips route segments without markers', async () => {
    const itinerary: Itinerary = {
      ...BASE_ITINERARY,
      startingLocation: 'Unknown origin',
      items: [{
        id: 'activity-1',
        type: 'activity',
        name: 'Known stop',
        image: '',
        category: 'other',
        location: 'Known address',
        startTime: '2027-11-30T17:00:00.000Z',
        endTime: '2027-11-30T18:00:00.000Z',
      }],
    };
    const itineraries = createItineraryRepository();
    itineraries.findItinerary.mockResolvedValue(itinerary);
    const provider = createMapProvider();
    provider.geocodeAddress.mockImplementation(async (address) =>
      address === 'Unknown origin' ? null : geocodedLocation(address),
    );
    const app = createApp({
      itineraryRepository: itineraries,
      itineraryMapRepository: createMapRepository(),
      mapProvider: provider,
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap.unmappedActivities[0]).toMatchObject({
      activityId: 'starting-location',
    });
    expect(response.body.itineraryMap.routes).toEqual([]);
  });

  test.each([
    ['walking', null, travelOption('driving'), 'driving-polyline'],
    ['driving', travelOption('walking'), null, 'walking-polyline'],
    ['walking', null, null, undefined],
  ] as const)(
    'uses available route fallback for preferred mode %s',
    async (transportationMode, walking, driving, encodedPolyline) => {
      const itinerary: Itinerary = {
        ...BASE_ITINERARY,
        items: [
          {
            id: 'activity-1',
            type: 'activity',
            name: 'First',
            image: '',
            category: 'other',
            location: 'First address',
            startTime: '2027-11-30T17:00:00.000Z',
            endTime: '2027-11-30T18:00:00.000Z',
          },
          {
            id: 'transport',
            type: 'transportation',
            mode: transportationMode,
            startLocation: 'First',
            endLocation: 'Second',
            distanceMeters: 100,
            startTime: '2027-11-30T18:00:00.000Z',
            endTime: '2027-11-30T18:30:00.000Z',
          },
          {
            id: 'activity-2',
            type: 'activity',
            name: 'Second',
            image: '',
            category: 'other',
            location: 'Second address',
            startTime: '2027-11-30T19:00:00.000Z',
            endTime: '2027-11-30T20:00:00.000Z',
          },
        ],
      };
      const itineraries = createItineraryRepository();
      itineraries.findItinerary.mockResolvedValue(itinerary);
      const provider = createMapProvider();
      provider.geocodeAddress.mockImplementation(async (address) =>
        geocodedLocation(address),
      );
      provider.getRoute.mockImplementation(async (_origin, _destination, mode) =>
        mode === 'walking' ? walking : driving,
      );
      const app = createApp({
        itineraryRepository: itineraries,
        itineraryMapRepository: createMapRepository(),
        mapProvider: provider,
      });

      const response = await request(app).get(MAP_PATH);

      expect(response.status).toBe(200);
      if (encodedPolyline === undefined) {
        expect(response.body.itineraryMap.routes).toEqual([]);
      } else {
        expect(response.body.itineraryMap.routes[0]).toMatchObject({
          transportationMode,
          encodedPolyline,
          walking,
          driving,
        });
      }
    },
  );
});

describe('Itinerary map router parameter validation — mocks', () => {
  const service = {
    getItineraryMap: jest.fn(),
  } as unknown as ItineraryMapService;

  test('rejects a missing user parameter', async () => {
    const app = express();
    app.use('/map', createItineraryMapRouter(service));

    const response = await authenticatedAgent(app).get('/map');

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_USER_ID');
  });

  test('rejects a missing itinerary parameter', async () => {
    const app = express();
    app.use('/users/:userId/map', createItineraryMapRouter(service));

    const response = await authenticatedAgent(app).get(`/users/${USER_ID}/map`);

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

function geocodedLocation(address: string): GeocodedLocation {
  const offset = address.length / 1000;
  return {
    coordinates: {
      latitude: 49.26 + offset,
      longitude: -123.25 - offset,
    },
    formattedAddress: address,
    placeId: `place-${address}`,
    isApproximate: false,
  };
}

function travelOption(mode: string): TravelOption {
  return {
    encodedPolyline: `${mode}-polyline`,
    distanceMeters: mode === 'walking' ? 500 : 900,
    durationSeconds: mode === 'walking' ? 600 : 300,
    steps: [{ instruction: `Continue by ${mode}` }],
  };
}
