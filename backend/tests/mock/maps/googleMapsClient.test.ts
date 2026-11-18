import 'dotenv/config';

import { env } from '../../../src/config/env';
import { GoogleMapsClient } from '../../../src/maps/googleMapsClient';
import type { TransportationMode } from '../../../src/users/userTypes';

const ORIGIN = { latitude: 49.26, longitude: -123.25 };
const DESTINATION = { latitude: 49.27, longitude: -123.24 };

function response(body: unknown, ok = true, status = 200): Response {
  return {
    ok,
    status,
    json: async () => body,
  } as Response;
}

describe('GoogleMapsClient — controlled provider responses', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('does not send an empty geocoding request', async () => {
    const fetchSpy = jest.spyOn(global, 'fetch');

    await expect(new GoogleMapsClient().geocodeAddress('  ')).resolves.toBeNull();
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  test.each([undefined, '', 'your-geocoding-key'])(
    'rejects an invalid geocoding key value %s',
    async (apiKey) => {
      const config = env.googleMaps as {
        geocodingApiKey: string | undefined;
      };
      const original = config.geocodingApiKey;
      config.geocodingApiKey = apiKey;
      try {
        await expect(
          new GoogleMapsClient().geocodeAddress('UBC'),
        ).rejects.toThrow('GEOCODING_API_KEY is not configured');
      } finally {
        config.geocodingApiKey = original;
      }
    },
  );

  test('maps a complete geocoding result', async () => {
    const fetchSpy = jest.spyOn(global, 'fetch').mockResolvedValue(response({
      status: 'OK',
      results: [{
        place_id: 'place-1',
        formatted_address: 'UBC, Vancouver, BC',
        partial_match: true,
        geometry: { location: { lat: 49.26, lng: -123.25 } },
      }],
    }));

    await expect(
      new GoogleMapsClient().geocodeAddress(' UBC '),
    ).resolves.toEqual({
      placeId: 'place-1',
      formattedAddress: 'UBC, Vancouver, BC',
      coordinates: { latitude: 49.26, longitude: -123.25 },
      isApproximate: true,
    });
    expect(fetchSpy.mock.calls[0]?.[0]).toContain('address=UBC');
  });

  test('defaults an omitted partial-match flag to false', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue(response({
      status: 'OK',
      results: [{
        place_id: 'place-1',
        formatted_address: 'UBC',
        geometry: { location: { lat: 49.26, lng: -123.25 } },
      }],
    }));

    await expect(
      new GoogleMapsClient().geocodeAddress('UBC'),
    ).resolves.toMatchObject({ isApproximate: false });
  });

  test('returns null for zero geocoding results', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue(response({
      status: 'ZERO_RESULTS',
      results: [],
    }));

    await expect(
      new GoogleMapsClient().geocodeAddress('unknown'),
    ).resolves.toBeNull();
  });

  test('rejects HTTP and provider geocoding failures', async () => {
    const client = new GoogleMapsClient();
    const fetchSpy = jest.spyOn(global, 'fetch');
    fetchSpy.mockResolvedValueOnce(response({}, false, 503));
    await expect(client.geocodeAddress('UBC')).rejects.toThrow(
      'Geocoding request failed with status 503',
    );

    fetchSpy.mockResolvedValueOnce(response({
      status: 'REQUEST_DENIED',
      error_message: 'denied',
    }));
    await expect(client.geocodeAddress('UBC')).rejects.toThrow('denied');

    fetchSpy.mockResolvedValueOnce(response({ status: 'OVER_QUERY_LIMIT' }));
    await expect(client.geocodeAddress('UBC')).rejects.toThrow(
      'Geocoding failed: OVER_QUERY_LIMIT',
    );
  });

  test.each([
    [{}],
    [{ place_id: 'place', formatted_address: 'UBC' }],
    [{
      place_id: 'place',
      formatted_address: 'UBC',
      geometry: { location: { lng: -123.25 } },
    }],
    [{
      place_id: 'place',
      formatted_address: 'UBC',
      geometry: { location: { lat: 49.26 } },
    }],
  ])('rejects incomplete geocoding result %#', async (result) => {
    jest.spyOn(global, 'fetch').mockResolvedValue(response({
      status: 'OK',
      results: [result],
    }));

    await expect(
      new GoogleMapsClient().geocodeAddress('UBC'),
    ).rejects.toThrow('Geocoding response is missing location data');
  });

  test.each<[TransportationMode, string]>([
    ['walking', 'WALK'],
    ['cycling', 'BICYCLE'],
    ['transit', 'TRANSIT'],
    ['driving', 'DRIVE'],
    ['other', 'DRIVE'],
  ])('maps %s to Google travel mode %s', async (mode, googleMode) => {
    const fetchSpy = jest.spyOn(global, 'fetch').mockResolvedValue(response({
      routes: [{
        encodedPolyline: 'ignored',
        distanceMeters: 100,
        duration: '120s',
        polyline: { encodedPolyline: 'route' },
        legs: [{
          steps: [
            { navigationInstruction: { instructions: ' Turn left ' } },
            { navigationInstruction: { instructions: ' ' } },
            {},
          ],
        }],
      }],
    }));

    await expect(
      new GoogleMapsClient().getRoute(ORIGIN, DESTINATION, mode),
    ).resolves.toEqual({
      encodedPolyline: 'route',
      distanceMeters: 100,
      durationSeconds: 120,
      steps: [{ instruction: 'Turn left' }],
    });
    const init = fetchSpy.mock.calls[0]?.[1];
    expect(JSON.parse(String(init?.body)).travelMode).toBe(googleMode);
  });

  test('uses safe defaults for omitted route details', async () => {
    jest.spyOn(global, 'fetch').mockResolvedValue(response({
      routes: [{
        polyline: { encodedPolyline: 'route' },
        duration: 'Nots',
        legs: [{}],
      }],
    }));

    await expect(
      new GoogleMapsClient().getRoute(ORIGIN, DESTINATION, 'walking'),
    ).resolves.toEqual({
      encodedPolyline: 'route',
      distanceMeters: 0,
      durationSeconds: 0,
      steps: [],
    });
  });

  test.each([
    [{}],
    [{ routes: [] }],
    [{ routes: [{}] }],
  ])('returns null for an incomplete route response %#', async (body) => {
    jest.spyOn(global, 'fetch').mockResolvedValue(response(body));

    await expect(
      new GoogleMapsClient().getRoute(ORIGIN, DESTINATION, 'walking'),
    ).resolves.toBeNull();
  });

  test('returns null when the routes request fails', async () => {
    const client = new GoogleMapsClient();
    const errorSpy = jest.spyOn(console, 'error').mockImplementation();
    const fetchSpy = jest.spyOn(global, 'fetch');
    fetchSpy.mockResolvedValueOnce(response({}, false, 429));
    await expect(
      client.getRoute(ORIGIN, DESTINATION, 'walking'),
    ).resolves.toBeNull();

    fetchSpy.mockRejectedValueOnce(new TypeError('network unavailable'));
    await expect(
      client.getRoute(ORIGIN, DESTINATION, 'walking'),
    ).resolves.toBeNull();
    expect(errorSpy).toHaveBeenCalledTimes(2);
  });
});
