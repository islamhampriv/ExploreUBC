import type {
  AddressAutocompleteResult,
  GeocodedLocation,
  TravelOption,
} from '../../../src/maps/mapTypes';

const mockGoogleMaps = {
  mapsApiKey: 'test-maps-key',
  geocodingApiKey: 'test-geocoding-key',
  routesApiKey: 'test-routes-key',
  placesApiKey: 'test-places-key',
};

jest.mock('../../../src/config/env', () => ({
  env: { googleMaps: mockGoogleMaps },
}));

import {
  autocompleteAddresses,
  fetchPopularPlaces,
  geocodeAddress,
  getRoute,
} from '../../../src/maps/googleMaps.client';

const UBC = { latitude: 49.269, longitude: -123.259 };
const NEST = { latitude: 49.266, longitude: -123.25 };

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as Response;
}

function fetchMock(): jest.Mock {
  return global.fetch as jest.Mock;
}

beforeEach(() => {
  mockGoogleMaps.geocodingApiKey = 'test-geocoding-key';
  mockGoogleMaps.routesApiKey = 'test-routes-key';
  mockGoogleMaps.placesApiKey = 'test-places-key';
  global.fetch = jest.fn();
  jest.spyOn(console, 'error').mockImplementation(() => undefined);
});

afterEach(() => {
  jest.restoreAllMocks();
});

describe('GET https://maps.googleapis.com/maps/api/geocode/json — mocks', () => {
  test('returns coordinates for a matched address', async () => {
    fetchMock().mockResolvedValue(jsonResponse({
      status: 'OK',
      results: [{
        place_id: 'place-1',
        formatted_address: '6393 NW Marine Dr, Vancouver, BC',
        partial_match: true,
        geometry: { location: { lat: UBC.latitude, lng: UBC.longitude } },
      }],
    }));

    const location: GeocodedLocation | null = await geocodeAddress(
      '  6393 NW Marine Dr  ',
    );

    expect(location).toEqual({
      coordinates: UBC,
      formattedAddress: '6393 NW Marine Dr, Vancouver, BC',
      placeId: 'place-1',
      isApproximate: true,
    });
    const url = String(fetchMock().mock.calls[0][0]);
    expect(url).toContain('https://maps.googleapis.com/maps/api/geocode/json');
    expect(url).toContain('region=ca');
    expect(url).toContain('key=test-geocoding-key');
  });

  test('returns null when the address is blank or has no results', async () => {
    await expect(geocodeAddress('   ')).resolves.toBeNull();
    expect(fetchMock()).not.toHaveBeenCalled();

    fetchMock().mockResolvedValue(jsonResponse({ status: 'ZERO_RESULTS', results: [] }));
    await expect(geocodeAddress('zzzz-not-a-place')).resolves.toBeNull();
  });

  test('throws when Geocoding rejects the request or omits location data', async () => {
    fetchMock().mockResolvedValue(jsonResponse({
      status: 'REQUEST_DENIED',
      error_message: 'The provided API key is invalid.',
    }));
    await expect(geocodeAddress('UBC')).rejects.toThrow('The provided API key is invalid.');

    fetchMock().mockResolvedValue(jsonResponse({}, 500));
    await expect(geocodeAddress('UBC')).rejects.toThrow(
      'Geocoding request failed with status 500',
    );

    fetchMock().mockResolvedValue(jsonResponse({ status: 'OK', results: [{}] }));
    await expect(geocodeAddress('UBC')).rejects.toThrow(
      'Geocoding response is missing location data',
    );
  });

  test('throws when the Geocoding key is missing or still a placeholder', async () => {
    mockGoogleMaps.geocodingApiKey = 'your_geocoding_key';
    await expect(geocodeAddress('UBC')).rejects.toThrow(
      'GEOCODING_API_KEY is not configured',
    );
    expect(fetchMock()).not.toHaveBeenCalled();
  });
});

describe('POST https://routes.googleapis.com/directions/v2:computeRoutes — mocks', () => {
  test('returns distance, duration, polyline, and steps', async () => {
    fetchMock().mockResolvedValue(jsonResponse({
      routes: [{
        distanceMeters: 890,
        duration: '789s',
        polyline: { encodedPolyline: 'encoded-walk' },
        legs: [{
          steps: [
            { navigationInstruction: { instructions: ' Head east ' } },
            { navigationInstruction: { instructions: '   ' } },
            {},
          ],
        }],
      }],
    }));

    const route: TravelOption | null = await getRoute(UBC, NEST, 'walking');

    expect(route).toEqual({
      encodedPolyline: 'encoded-walk',
      distanceMeters: 890,
      durationSeconds: 789,
      steps: [{ instruction: 'Head east' }],
    });
    const request = fetchMock().mock.calls[0][1] as RequestInit;
    expect(fetchMock().mock.calls[0][0]).toBe(
      'https://routes.googleapis.com/directions/v2:computeRoutes',
    );
    expect(request.headers).toMatchObject({
      'X-Goog-Api-Key': 'test-routes-key',
      'X-Goog-FieldMask':
        'routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.legs.steps.navigationInstruction',
    });
    expect(JSON.parse(String(request.body))).toMatchObject({
      travelMode: 'WALK',
      origin: { location: { latLng: UBC } },
      destination: { location: { latLng: NEST } },
    });
  });

  test.each([
    ['cycling', 'BICYCLE'],
    ['transit', 'TRANSIT'],
    ['driving', 'DRIVE'],
    ['other', 'DRIVE'],
  ] as const)('requests %s as %s', async (mode, travelMode) => {
    fetchMock().mockResolvedValue(jsonResponse({
      routes: [{ polyline: { encodedPolyline: 'line' }, duration: 'not-seconds' }],
    }));

    const route = await getRoute(UBC, NEST, mode);

    expect(route?.durationSeconds).toBe(0);
    expect(JSON.parse(String((fetchMock().mock.calls[0][1] as RequestInit).body))).toMatchObject({
      travelMode,
    });
  });

  test('returns null when no route is available or the request fails', async () => {
    fetchMock().mockResolvedValue(jsonResponse({ routes: [] }));
    await expect(getRoute(UBC, NEST, 'driving')).resolves.toBeNull();

    fetchMock().mockResolvedValue(jsonResponse({ error: { message: 'denied' } }, 403));
    await expect(getRoute(UBC, NEST, 'driving')).resolves.toBeNull();

    fetchMock().mockRejectedValue(new Error('network down'));
    await expect(getRoute(UBC, NEST, 'driving')).resolves.toBeNull();
  });

  test('throws when the Routes key is missing or still a placeholder', async () => {
    mockGoogleMaps.routesApiKey = '';
    await expect(getRoute(UBC, NEST, 'driving')).rejects.toThrow(
      'ROUTES_API_KEY is not configured',
    );
  });
});

describe('POST https://places.googleapis.com/v1/places:autocomplete — mocks', () => {
  test('returns address suggestions biased toward UBC', async () => {
    fetchMock().mockResolvedValue(jsonResponse({
      suggestions: [
        {
          placePrediction: {
            placeId: 'nest-id',
            text: { text: 'UBC Nest, Vancouver, BC' },
            structuredFormat: { mainText: { text: 'UBC Nest' } },
          },
        },
        { placePrediction: { text: { text: '   ' } } },
        {
          placePrediction: {
            text: { text: 'Museum of Anthropology' },
          },
        },
      ],
    }));

    const result: AddressAutocompleteResult = await autocompleteAddresses('  ubc  ');

    expect(result.errorMessage).toBeUndefined();
    expect(result.suggestions).toEqual([
      {
        primaryText: 'UBC Nest',
        fullAddress: 'UBC Nest, Vancouver, BC',
        placeId: 'nest-id',
      },
      {
        primaryText: 'Museum of Anthropology',
        fullAddress: 'Museum of Anthropology',
        placeId: '',
      },
    ]);
    const request = fetchMock().mock.calls[0][1] as RequestInit;
    expect(fetchMock().mock.calls[0][0]).toBe(
      'https://places.googleapis.com/v1/places:autocomplete',
    );
    expect(request.headers).toMatchObject({ 'X-Goog-Api-Key': 'test-places-key' });
    expect(JSON.parse(String(request.body))).toMatchObject({
      input: 'ubc',
      includedRegionCodes: ['ca'],
      locationBias: { circle: { center: { latitude: 49.2606, longitude: -123.246 } } },
    });
  });

  test('returns no suggestions for a query shorter than three characters', async () => {
    await expect(autocompleteAddresses(' u ')).resolves.toEqual({ suggestions: [] });
    expect(fetchMock()).not.toHaveBeenCalled();
  });

  test('returns the Places error message when the request is rejected', async () => {
    fetchMock().mockResolvedValue(jsonResponse({
      error: { status: 'PERMISSION_DENIED', message: 'Places API has not been used.' },
    }, 403));

    await expect(autocompleteAddresses('UBC Nest')).resolves.toEqual({
      suggestions: [],
      errorMessage: 'Places API error (PERMISSION_DENIED): Places API has not been used.',
    });

    fetchMock().mockResolvedValue(jsonResponse({}, 500));
    await expect(autocompleteAddresses('UBC Nest')).resolves.toEqual({
      suggestions: [],
      errorMessage: 'Places request failed with status 500',
    });
  });

  test('throws when the Places key is missing or still a placeholder', async () => {
    mockGoogleMaps.placesApiKey = 'your_places_key';
    await expect(autocompleteAddresses('UBC Nest')).rejects.toThrow(
      'PLACES_API_KEY is not configured',
    );
  });
});

describe('POST https://places.googleapis.com/v1/places:searchNearby — mocks', () => {
  test('returns popular places around the requested center', async () => {
    fetchMock().mockResolvedValue(jsonResponse({
      places: [
        {
          displayName: { text: 'Museum of Anthropology' },
          formattedAddress: '6393 NW Marine Dr',
          types: ['tourist_attraction', 'museum'],
          location: { latitude: UBC.latitude, longitude: UBC.longitude },
        },
        { displayName: { text: 'Missing coordinates' } },
      ],
    }));

    const places = await fetchPopularPlaces(UBC);

    expect(places).toEqual([{
      name: 'Museum of Anthropology',
      category: 'tourist attraction',
      address: '6393 NW Marine Dr',
      coordinates: UBC,
    }]);
    const request = fetchMock().mock.calls[0][1] as RequestInit;
    expect(fetchMock().mock.calls[0][0]).toBe(
      'https://places.googleapis.com/v1/places:searchNearby',
    );
    expect(request.headers).toMatchObject({ 'X-Goog-Api-Key': 'test-places-key' });
    expect(JSON.parse(String(request.body))).toMatchObject({
      includedTypes: ['tourist_attraction'],
      maxResultCount: 8,
      locationRestriction: { circle: { center: UBC, radius: 3000 } },
    });
  });

  test('returns an empty list when Places has no matches', async () => {
    fetchMock().mockResolvedValue(jsonResponse({ places: [] }));
    await expect(fetchPopularPlaces(UBC)).resolves.toEqual([]);
  });

  test('returns null when nearby search fails', async () => {
    fetchMock().mockResolvedValue(jsonResponse({}, 403));
    await expect(fetchPopularPlaces(UBC)).resolves.toBeNull();

    fetchMock().mockRejectedValue(new Error('network down'));
    await expect(fetchPopularPlaces(UBC)).resolves.toBeNull();
  });

  test('throws when the Places key is missing or still a placeholder', async () => {
    mockGoogleMaps.placesApiKey = undefined as unknown as string;
    await expect(fetchPopularPlaces(UBC)).rejects.toThrow(
      'PLACES_API_KEY is not configured',
    );
  });
});
