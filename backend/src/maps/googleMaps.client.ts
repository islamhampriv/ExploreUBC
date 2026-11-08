import { env } from '../config/env';
import type { TransportationMode } from '../users/userTypes';
import type {
  AddressAutocompleteResult,
  AddressSuggestion,
  GeocodedLocation,
  MapCoordinates,
  PopularPlace,
  RouteStep,
  TravelOption,
} from './mapTypes';

const GEOCODING_URL = 'https://maps.googleapis.com/maps/api/geocode/json';
const ROUTES_URL = 'https://routes.googleapis.com/directions/v2:computeRoutes';
const PLACES_AUTOCOMPLETE_URL = 'https://places.googleapis.com/v1/places:autocomplete';
const PLACES_NEARBY_URL = 'https://places.googleapis.com/v1/places:searchNearby';
const UBC_CENTER = { latitude: 49.2606, longitude: -123.246 };

interface GeocodingResponse {
  status?: string;
  error_message?: string;
  results?: Array<{
    place_id?: string;
    formatted_address?: string;
    partial_match?: boolean;
    geometry?: {
      location?: {
        lat?: number;
        lng?: number;
      };
    };
  }>;
}

interface RouteStepResponse {
  navigationInstruction?: {
    instructions?: string;
  };
}

interface RoutesResponse {
  routes?: Array<{
    distanceMeters?: number;
    duration?: string;
    polyline?: {
      encodedPolyline?: string;
    };
    legs?: Array<{
      steps?: RouteStepResponse[];
    }>;
  }>;
}

/**
 * Converts an address into coordinates and Google location details.
 * @param address Address to geocode.
 * @returns The matched location, or null when no location is found.
 */
export async function geocodeAddress(
  address: string,
): Promise<GeocodedLocation | null> {
  const trimmed = address.trim();
  if (trimmed === '') {
    return null;
  }
  const apiKey = requireApiKey(
    env.googleMaps.geocodingApiKey,
    'GEOCODING_API_KEY',
  );

  const query = new URLSearchParams({
    address: trimmed,
    region: 'ca',
    key: apiKey,
  });
  const response = await fetch(`${GEOCODING_URL}?${query.toString()}`);
  if (!response.ok) {
    throw new Error(`Geocoding request failed with status ${response.status}`);
  }

  const body = (await response.json()) as GeocodingResponse;
  if (body.status === 'ZERO_RESULTS') {
    return null;
  }
  if (body.status !== 'OK') {
    throw new Error(body.error_message ?? `Geocoding failed: ${body.status}`);
  }

  const result = body.results?.[0];
  const latitude = result?.geometry?.location?.lat;
  const longitude = result?.geometry?.location?.lng;
  if (
    result?.place_id === undefined ||
    result.formatted_address === undefined ||
    latitude === undefined ||
    longitude === undefined
  ) {
    throw new Error('Geocoding response is missing location data');
  }

  return {
    coordinates: { latitude, longitude },
    formattedAddress: result.formatted_address,
    placeId: result.place_id,
    isApproximate: result.partial_match ?? false,
  };
}

/**
 * Calculates a route between two coordinates.
 * @param origin Starting coordinates.
 * @param destination Ending coordinates.
 * @param transportationMode Requested transportation mode.
 * @returns The calculated route, or null when no route is found.
 */
export async function getRoute(
  origin: MapCoordinates,
  destination: MapCoordinates,
  transportationMode: TransportationMode,
): Promise<TravelOption | null> {
  const apiKey = requireApiKey(env.googleMaps.routesApiKey, 'ROUTES_API_KEY');

  try {
    const response = await fetch(ROUTES_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Goog-Api-Key': apiKey,
        'X-Goog-FieldMask':
          'routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.legs.steps.navigationInstruction',
      },
      body: JSON.stringify({
        origin: toWaypoint(origin),
        destination: toWaypoint(destination),
        travelMode: toGoogleTravelMode(transportationMode),
      }),
    });
    if (!response.ok) {
      console.error(`Routes request failed with status ${response.status}`);
      return null;
    }
    return parseTravelOption((await response.json()) as RoutesResponse);
  } catch (error: unknown) {
    console.error('Routes request failed', error);
    return null;
  }
}

function parseTravelOption(body: RoutesResponse): TravelOption | null {
  const route = body.routes?.[0];
  const encodedPolyline = route?.polyline?.encodedPolyline;
  if (route === undefined || encodedPolyline === undefined) {
    return null;
  }
  return {
    encodedPolyline,
    distanceMeters: route.distanceMeters ?? 0,
    durationSeconds: parseDurationSeconds(route.duration),
    steps: parseRouteSteps(route.legs),
  };
}

function parseRouteSteps(
  legs: Array<{ steps?: RouteStepResponse[] }> | undefined,
): RouteStep[] {
  if (legs === undefined) {
    return [];
  }
  const steps: RouteStep[] = [];
  for (const leg of legs) {
    for (const step of leg.steps ?? []) {
      const instruction = step.navigationInstruction?.instructions?.trim();
      if (instruction !== undefined && instruction !== '') {
        steps.push({ instruction });
      }
    }
  }
  return steps;
}

/** Returns a configured API key or throws a configuration error. */
function requireApiKey(
  apiKey: string | undefined,
  variableName: string,
): string {
  if (
    apiKey === undefined ||
    apiKey.trim() === '' ||
    apiKey.trim().startsWith('your')
  ) {
    throw new Error(`${variableName} is not configured`);
  }
  return apiKey.trim();
}

/** Converts coordinates into the waypoint format required by Routes API. */
function toWaypoint(coordinates: MapCoordinates): object {
  return {
    location: {
      latLng: coordinates,
    },
  };
}

interface PlacesAutocompleteResponse {
  error?: {
    status?: string;
    message?: string;
  };
  suggestions?: Array<{
    placePrediction?: {
      placeId?: string;
      text?: { text?: string };
      structuredFormat?: { mainText?: { text?: string } };
    };
  }>;
}

interface NearbySearchResponse {
  places?: Array<{
    displayName?: { text?: string };
    formattedAddress?: string;
    types?: string[];
    location?: { latitude?: number; longitude?: number };
  }>;
}

/**
 * Retrieves Google Places address suggestions near UBC.
 * @param query Partial address typed by the user.
 * @returns Matching suggestions, or an error message when Places rejects the request.
 */
export async function autocompleteAddresses(
  query: string,
): Promise<AddressAutocompleteResult> {
  const trimmed = query.trim();
  if (trimmed.length < 3) {
    return { suggestions: [] };
  }
  const apiKey = requireApiKey(env.googleMaps.placesApiKey, 'PLACES_API_KEY');
  const response = await fetch(PLACES_AUTOCOMPLETE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Goog-Api-Key': apiKey,
      'X-Goog-FieldMask':
        'suggestions.placePrediction.placeId,suggestions.placePrediction.text,suggestions.placePrediction.structuredFormat',
    },
    body: JSON.stringify({
      input: trimmed,
      includedRegionCodes: ['ca'],
      locationBias: {
        circle: { center: UBC_CENTER, radius: 30_000 },
      },
    }),
  });
  return parseAutocompleteResponse(response);
}

/**
 * Retrieves popular places near a map center.
 * @param center Coordinates to search around.
 * @returns Up to eight places, an empty list when none match, or null when the request fails.
 */
export async function fetchPopularPlaces(
  center: MapCoordinates,
): Promise<PopularPlace[] | null> {
  const apiKey = requireApiKey(env.googleMaps.placesApiKey, 'PLACES_API_KEY');

  try {
    const response = await fetch(PLACES_NEARBY_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Goog-Api-Key': apiKey,
        'X-Goog-FieldMask':
          'places.displayName,places.formattedAddress,places.types,places.location',
      },
      body: JSON.stringify({
        includedTypes: ['tourist_attraction'],
        maxResultCount: 8,
        locationRestriction: {
          circle: { center, radius: 3000 },
        },
      }),
    });
    if (!response.ok) {
      return null;
    }
    const body = (await response.json()) as NearbySearchResponse;
    return parsePopularPlaces(body.places ?? []);
  } catch (error: unknown) {
    console.error('Places nearby request failed', error);
    return null;
  }
}

async function parseAutocompleteResponse(
  response: Response,
): Promise<AddressAutocompleteResult> {
  const body = (await response.json()) as PlacesAutocompleteResponse;
  if (!response.ok || body.error !== undefined) {
    return {
      suggestions: [],
      errorMessage: placesErrorMessage(response.status, body.error),
    };
  }
  return { suggestions: parseSuggestions(body.suggestions ?? []) };
}

function placesErrorMessage(
  status: number,
  error: PlacesAutocompleteResponse['error'],
): string {
  if (error?.message !== undefined && error.message.trim() !== '') {
    return `Places API error (${error.status ?? 'unknown error'}): ${error.message}`;
  }
  return `Places request failed with status ${status}`;
}

function parseSuggestions(
  suggestions: NonNullable<PlacesAutocompleteResponse['suggestions']>,
): AddressSuggestion[] {
  const parsed: AddressSuggestion[] = [];
  for (const suggestion of suggestions.slice(0, 6)) {
    const prediction = suggestion.placePrediction;
    const fullAddress = prediction?.text?.text?.trim() ?? '';
    if (prediction === undefined || fullAddress === '') {
      continue;
    }
    const primary = prediction.structuredFormat?.mainText?.text?.trim();
    parsed.push({
      primaryText: primary !== undefined && primary !== '' ? primary : fullAddress,
      fullAddress,
      placeId: prediction.placeId ?? '',
    });
  }
  return parsed;
}

function parsePopularPlaces(
  results: NonNullable<NearbySearchResponse['places']>,
): PopularPlace[] {
  const places: PopularPlace[] = [];
  for (const place of results) {
    const name = place.displayName?.text?.trim() ?? '';
    const latitude = place.location?.latitude;
    const longitude = place.location?.longitude;
    if (name === '' || latitude === undefined || longitude === undefined) {
      continue;
    }
    const category = place.types?.[0]?.replaceAll('_', ' ');
    places.push({
      name,
      category: category !== undefined && category !== '' ? category : 'Place',
      address: place.formattedAddress ?? '',
      coordinates: { latitude, longitude },
    });
  }
  return places;
}

/** Converts an internal transportation mode to a Google travel mode. */
function toGoogleTravelMode(mode: TransportationMode): string {
  switch (mode) {
    case 'walking':
      return 'WALK';
    case 'cycling':
      return 'BICYCLE';
    case 'transit':
      return 'TRANSIT';
    case 'driving':
    case 'other':
      return 'DRIVE';
  }
}

/** Converts a Google duration such as `120s` into seconds. */
function parseDurationSeconds(duration: string | undefined): number {
  if (duration === undefined || !duration.endsWith('s')) {
    return 0;
  }
  const seconds = Number(duration.slice(0, -1));
  return Number.isFinite(seconds) ? seconds : 0;
}
