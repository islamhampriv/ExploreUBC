import {
  autocompleteAddresses,
  fetchPopularPlaces,
  geocodeAddress,
  getRoute,
} from '../../../src/maps/googleMaps.client';

jest.setTimeout(30_000);

const MUSEUM = '6393 NW Marine Dr, Vancouver, BC, Canada';
const NEST = '6138 Student Union Blvd, Vancouver, BC, Canada';

describe('GET https://maps.googleapis.com/maps/api/geocode/json — no mocks', () => {
  test('geocodes a UBC address inside Vancouver', async () => {
    const location = await geocodeAddress(MUSEUM);

    expect(location).not.toBeNull();
    expect(location?.formattedAddress.toLowerCase()).toContain('vancouver');
    expect(location?.placeId).toEqual(expect.any(String));
    expect(location?.coordinates.latitude).toBeGreaterThan(49);
    expect(location?.coordinates.latitude).toBeLessThan(49.4);
    expect(location?.coordinates.longitude).toBeGreaterThan(-123.4);
    expect(location?.coordinates.longitude).toBeLessThan(-123);
  });

  test('returns null for a blank address or an address Google cannot match', async () => {
    await expect(geocodeAddress('   ')).resolves.toBeNull();
    await expect(
      geocodeAddress('zzzz-not-a-real-place-xyzzy-99999'),
    ).resolves.toBeNull();
  });
});

describe('POST https://routes.googleapis.com/directions/v2:computeRoutes — no mocks', () => {
  test('returns walking and driving routes between two UBC stops', async () => {
    const origin = await geocodeAddress(MUSEUM);
    const destination = await geocodeAddress(NEST);
    expect(origin).not.toBeNull();
    expect(destination).not.toBeNull();
    if (origin === null || destination === null) {
      return;
    }

    const walking = await getRoute(origin.coordinates, destination.coordinates, 'walking');
    const driving = await getRoute(origin.coordinates, destination.coordinates, 'driving');

    expect(walking?.encodedPolyline.length).toBeGreaterThan(0);
    expect(walking?.distanceMeters).toBeGreaterThan(0);
    expect(walking?.durationSeconds).toBeGreaterThan(0);
    expect(walking?.steps.length).toBeGreaterThan(0);
    expect(driving?.encodedPolyline.length).toBeGreaterThan(0);
    expect(driving?.distanceMeters).toBeGreaterThan(0);
    expect(driving?.durationSeconds).toBeGreaterThan(0);
    expect(driving?.steps.length).toBeGreaterThan(0);
  });
});

describe('POST https://places.googleapis.com/v1/places:autocomplete — no mocks', () => {
  test('returns Canadian address suggestions for a UBC query', async () => {
    const result = await autocompleteAddresses('UBC Nest');

    expect(result.errorMessage).toBeUndefined();
    expect(result.suggestions.length).toBeGreaterThan(0);
    expect(result.suggestions[0]?.fullAddress.toLowerCase()).toMatch(/vancouver|ubc|canada/);
    expect(result.suggestions[0]?.placeId).toEqual(expect.any(String));
  });

  test('returns no suggestions before the query is long enough to send', async () => {
    await expect(autocompleteAddresses('ub')).resolves.toEqual({ suggestions: [] });
  });
});

describe('POST https://places.googleapis.com/v1/places:searchNearby — no mocks', () => {
  test('returns tourist attractions near the UBC campus', async () => {
    const places = await fetchPopularPlaces({ latitude: 49.2606, longitude: -123.246 });

    expect(places).not.toBeNull();
    expect(places?.length).toBeGreaterThan(0);
    expect(places?.[0]?.name).toEqual(expect.any(String));
    expect(places?.[0]?.coordinates.latitude).toBeGreaterThan(49);
    expect(places?.[0]?.coordinates.longitude).toBeLessThan(-123);
  });
});
