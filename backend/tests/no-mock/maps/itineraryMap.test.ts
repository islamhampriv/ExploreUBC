import 'dotenv/config';

import * as os from 'node:os';

import type { Express } from 'express';
import { type Db, MongoClient, ObjectId } from 'mongodb';

import {
  createApp as createBackendApp,
  type AppDependencies,
} from '../../../src/index';
import { ItineraryDao } from '../../../src/itineraries/itineraryDao';
import { ItineraryMapDao } from '../../../src/maps/itineraryMapDao';
import {
  authenticatedAgent,
  MongoTestSessions,
} from '../../support/authTestUtils';

jest.setTimeout(60_000);

const USER_ID = new ObjectId();
const OTHER_USER_ID = new ObjectId();
const ITINERARY_ID = new ObjectId();
const MISSING_ITINERARY_ID = new ObjectId();
const DATABASE_NAME = `eubc_map_${new ObjectId().toHexString()}`;
const MAP_PATH =
  `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/map`;

let client: MongoClient;
let database: Db;
let app: Express;
let mapDao: ItineraryMapDao;
let testSessions: MongoTestSessions;

function createApp(dependencies: Omit<AppDependencies, 'sessionService'>): Express {
  return createBackendApp({
    ...dependencies,
    sessionService: testSessions.service,
  });
}

const request = (expressApp: Express) => authenticatedAgent(
  expressApp,
  (userId) =>
    testSessions.tokenFor(userId) ?? testSessions.tokenFor(USER_ID.toHexString()),
);

function seededItinerary() {
  return {
    _id: ITINERARY_ID,
    ownerId: USER_ID,
    name: 'Live map itinerary',
    startingLocation: 'UBC Bus Exchange, Vancouver, BC',
    items: [{
      id: 'activity-1',
      type: 'activity',
      name: 'UBC Bookstore',
      image: '',
      category: 'other',
      location: '6200 University Blvd, Vancouver, BC',
      startTime: '2027-11-30T17:00:00.000Z',
      endTime: '2027-11-30T18:00:00.000Z',
    }],
    createdAt: new Date('2026-10-12T12:00:00.000Z'),
    updatedAt: new Date('2026-10-12T12:00:00.000Z'),
    saved: true,
    inRecommendation: false,
    score: {},
  };
}

beforeAll(async () => {
  for (const variable of [
    'MONGODB_URI',
    'GEOCODING_API_KEY',
    'ROUTES_API_KEY',
  ]) {
    if (process.env[variable] === undefined || process.env[variable]!.trim() === '') {
      throw new Error(`${variable} is required for no-mock map tests.`);
    }
  }

  client = new MongoClient(process.env.MONGODB_URI!, {
    runtimeAdapters: { os },
    serverSelectionTimeoutMS: 10_000,
  });
  database = client.db(DATABASE_NAME);
  testSessions = new MongoTestSessions(database);
  mapDao = new ItineraryMapDao(database);
  app = createApp({
    itineraryRepository: new ItineraryDao(database),
    itineraryMapRepository: mapDao,
  });
  await client.connect();
  await mapDao.createIndexes();
});

beforeEach(async () => {
  await testSessions.reset([
    USER_ID.toHexString(),
    OTHER_USER_ID.toHexString(),
  ]);
  await database.collection('itineraries').deleteMany({});
  await database.collection('itineraryMaps').deleteMany({});
  await database.collection('itineraries').insertOne(seededItinerary());
});

afterAll(async () => {
  await database.dropDatabase();
  await client.close();
});

describe('GET /api/v1/users/:userId/itineraries/:itineraryId/map — no mocks', () => {
  test('generates and persists map data using MongoDB and Google Maps', async () => {
    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap).toMatchObject({
      itineraryId: ITINERARY_ID.toHexString(),
      ownerId: USER_ID.toHexString(),
      itineraryUpdatedAt: '2026-10-12T12:00:00.000Z',
      unmappedActivities: [],
    });
    expect(response.body.itineraryMap.markers.map(
      (marker: { activityId: string }) => marker.activityId,
    )).toEqual(['starting-location', 'activity-1']);
    expect(response.body.itineraryMap.routes).toHaveLength(2);
    for (const route of response.body.itineraryMap.routes) {
      expect(route.walking).toMatchObject({
        encodedPolyline: expect.any(String),
        distanceMeters: expect.any(Number),
        durationSeconds: expect.any(Number),
      });
      expect(route.driving).toMatchObject({
        encodedPolyline: expect.any(String),
        distanceMeters: expect.any(Number),
        durationSeconds: expect.any(Number),
      });
    }

    await expect(mapDao.findByItineraryId(
      ITINERARY_ID.toHexString(),
    )).resolves.toEqual(response.body.itineraryMap);
  });

  test('returns a current persisted cache with API-safe IDs and dates', async () => {
    const itinerary = seededItinerary();
    await database.collection('itineraryMaps').insertOne({
      _id: new ObjectId(),
      itineraryId: ITINERARY_ID,
      ownerId: USER_ID,
      itineraryUpdatedAt: itinerary.updatedAt,
      markers: [
        {
          activityId: 'starting-location',
          title: 'Starting location',
          address: itinerary.startingLocation,
          placeId: 'cached-start',
          coordinates: { latitude: 49.265, longitude: -123.249 },
          isApproximate: false,
        },
        {
          activityId: 'activity-1',
          title: 'UBC Bookstore',
          address: itinerary.items[0]!.location,
          placeId: 'cached-activity',
          coordinates: { latitude: 49.267, longitude: -123.25 },
          isApproximate: false,
        },
      ],
      routes: [{
        fromActivityId: 'activity-1',
        toActivityId: 'starting-location',
        transportationMode: 'driving',
        encodedPolyline: 'cached',
        distanceMeters: 100,
        durationSeconds: 60,
        walking: null,
        driving: null,
      }],
      generatedAt: new Date('2026-10-12T12:05:00.000Z'),
    });

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap).toMatchObject({
      itineraryId: ITINERARY_ID.toHexString(),
      ownerId: USER_ID.toHexString(),
      generatedAt: '2026-10-12T12:05:00.000Z',
      unmappedActivities: [],
    });
  });

  test('stores an empty map for an itinerary without locations', async () => {
    await database.collection('itineraries').updateOne(
      { _id: ITINERARY_ID },
      {
        $unset: { startingLocation: '' },
        $set: { items: [], updatedAt: new Date('2026-10-13T12:00:00.000Z') },
      },
    );

    const response = await request(app).get(MAP_PATH);

    expect(response.status).toBe(200);
    expect(response.body.itineraryMap).toMatchObject({
      markers: [],
      routes: [],
      unmappedActivities: [],
    });
    await expect(database.collection('itineraryMaps').countDocuments({
      itineraryId: ITINERARY_ID,
    })).resolves.toBe(1);
  });

  test('returns not found for an absent or differently owned itinerary', async () => {
    for (const path of [
      `/api/v1/users/${USER_ID}/itineraries/${MISSING_ITINERARY_ID}/map`,
      `/api/v1/users/${OTHER_USER_ID}/itineraries/${ITINERARY_ID}/map`,
    ]) {
      const response = await request(app).get(path);
      expect(response.status).toBe(404);
      expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
    }
  });
});

describe('Itinerary map persistence guards — no mocks', () => {
  test('returns safe results for malformed or missing IDs', async () => {
    await expect(mapDao.findByItineraryId('not-an-id')).resolves.toBeNull();
    await expect(mapDao.findByItineraryId(
      MISSING_ITINERARY_ID.toHexString(),
    )).resolves.toBeNull();
    await expect(mapDao.deleteByItineraryId('not-an-id')).resolves.toBe(false);
    await expect(mapDao.deleteByItineraryId(
      MISSING_ITINERARY_ID.toHexString(),
    )).resolves.toBe(false);
  });

  test.each([
    ['not-an-id', USER_ID.toHexString()],
    [ITINERARY_ID.toHexString(), 'not-an-id'],
  ])('rejects invalid persisted IDs (%s, %s)', async (itineraryId, ownerId) => {
    await expect(mapDao.upsertMapData({
      itineraryId,
      ownerId,
      itineraryUpdatedAt: '2026-10-12T12:00:00.000Z',
      markers: [],
      routes: [],
      unmappedActivities: [],
      generatedAt: '2026-10-12T12:05:00.000Z',
    })).rejects.toThrow('Itinerary map contains an invalid ID');
  });

  test('replaces and deletes persisted map data', async () => {
    const mapData = {
      itineraryId: ITINERARY_ID.toHexString(),
      ownerId: USER_ID.toHexString(),
      itineraryUpdatedAt: '2026-10-12T12:00:00.000Z',
      markers: [],
      routes: [],
      unmappedActivities: [],
      generatedAt: '2026-10-12T12:05:00.000Z',
    };
    await mapDao.upsertMapData(mapData);
    await mapDao.upsertMapData({
      ...mapData,
      generatedAt: '2026-10-12T12:06:00.000Z',
    });

    await expect(
      mapDao.findByItineraryId(ITINERARY_ID.toHexString()),
    ).resolves.toMatchObject({
      generatedAt: '2026-10-12T12:06:00.000Z',
    });
    await expect(
      mapDao.deleteByItineraryId(ITINERARY_ID.toHexString()),
    ).resolves.toBe(true);
  });
});
