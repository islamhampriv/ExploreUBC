import { ItineraryNotFoundError } from '../itineraries/itineraryErrors';
import type { ItineraryRepository } from '../itineraries/itineraryRepository';
import type {
  ActivityItem,
  ItineraryItem,
  TransportationItem,
} from '../itineraries/itineraryTypes';
import type { TransportationMode } from '../users/userTypes';
import { geocodeAddress, getRoute } from './googleMaps.client';
import type { ItineraryMapRepository } from './itineraryMapRepository';
import type {
  ItineraryMap,
  MapMarker,
  MapRoute,
  TravelOption,
  UnmappedActivity,
} from './mapTypes';

type MarkerGenerationResult = {
  markers: MapMarker[];
  unmappedActivities: UnmappedActivity[];
};

const STARTING_LOCATION_ID = 'starting-location';
const STARTING_LOCATION_TITLE = 'Starting location';

export class ItineraryMapService {
  constructor(
    private readonly itineraryRepository: ItineraryRepository,
    private readonly itineraryMapRepository: ItineraryMapRepository,
  ) {}

  /**
   * Returns cached or newly generated map data for an itinerary.
   * @param userId Itinerary owner's ID.
   * @param itineraryId Itinerary ID.
   * @returns Markers and routes for the itinerary.
   */
  async getItineraryMap(
    userId: string,
    itineraryId: string,
  ): Promise<ItineraryMap> {
    const itinerary = await this.itineraryRepository.findItinerary(
      userId,
      itineraryId,
    );
    if (itinerary === null) {
      throw new ItineraryNotFoundError(itineraryId);
    }

    const cachedMap =
      await this.itineraryMapRepository.findByItineraryId(itineraryId);
    if (
      cachedMap?.itineraryUpdatedAt === itinerary.updatedAt &&
      hasWalkingAndDriving(cachedMap) &&
      hasExpectedStartingLocation(cachedMap, itinerary.startingLocation)
    ) {
      return cachedMap;
    }

    try {
      const orderedItems = orderItemsByStartTime(itinerary.items);
      const { markers, unmappedActivities } =
        await createMarkers(orderedItems, itinerary.startingLocation);
      const routes = await createRoutes(
        orderedItems,
        markers,
        itinerary.startingLocation,
      );
      const mapData: ItineraryMap = {
        itineraryId: itinerary.id,
        ownerId: itinerary.ownerId,
        itineraryUpdatedAt: itinerary.updatedAt,
        markers,
        routes,
        unmappedActivities,
        generatedAt: new Date().toISOString(),
      };

      await this.itineraryMapRepository.upsertMapData(mapData);
      return mapData;
    } catch (error: unknown) {
      if (cachedMap !== null) {
        return cachedMap;
      }
      throw error;
    }
  }

  /** Deletes cached map data after its itinerary is deleted. */
  async deleteItineraryMap(itineraryId: string): Promise<void> {
    await this.itineraryMapRepository.deleteByItineraryId(itineraryId);
  }
}

/** Sorts itinerary items into chronological order. */
function orderItemsByStartTime(items: ItineraryItem[]): ItineraryItem[] {
  return [...items].sort(
    (first, second) =>
      new Date(first.startTime).getTime() - new Date(second.startTime).getTime(),
  );
}

/** Geocodes activities into markers and records unresolved addresses. */
async function createMarkers(
  items: ItineraryItem[],
  startingLocation?: string,
): Promise<MarkerGenerationResult> {
  const markers: MapMarker[] = [];
  const unmappedActivities: UnmappedActivity[] = [];
  if (startingLocation?.trim()) {
    const location = await geocodeAddress(startingLocation);
    if (location === null) {
      unmappedActivities.push({
        activityId: STARTING_LOCATION_ID,
        title: STARTING_LOCATION_TITLE,
        address: startingLocation,
      });
    } else {
      markers.push({
        activityId: STARTING_LOCATION_ID,
        title: STARTING_LOCATION_TITLE,
        address: location.formattedAddress,
        placeId: location.placeId,
        coordinates: location.coordinates,
        isApproximate: location.isApproximate,
      });
    }
  }
  for (const item of items) {
    if (item.type !== 'activity') {
      continue;
    }
    const location = await geocodeAddress(item.location);
    if (location === null) {
      unmappedActivities.push({
        activityId: item.id,
        title: item.name,
        address: item.location,
      });
      continue;
    }
    markers.push({
      activityId: item.id,
      title: item.name,
      address: location.formattedAddress,
      placeId: location.placeId,
      coordinates: location.coordinates,
      isApproximate: location.isApproximate,
    });
  }
  return { markers, unmappedActivities };
}

/** Calculates route segments between consecutive activity markers. */
async function createRoutes(
  items: ItineraryItem[],
  markers: MapMarker[],
  startingLocation?: string,
): Promise<MapRoute[]> {
  const routes: MapRoute[] = [];
  const activities = items.filter(
    (item): item is ActivityItem => item.type === 'activity',
  );
  const stopIds = [
    ...(startingLocation?.trim() ? [STARTING_LOCATION_ID] : []),
    ...activities.map((activity) => activity.id),
  ];
  for (let index = 0; index < stopIds.length - 1; index += 1) {
    const originId = stopIds[index];
    const destinationId = stopIds[index + 1];
    if (originId === undefined || destinationId === undefined) {
      continue;
    }
    const origin = markers.find(
      (marker) => marker.activityId === originId,
    );
    const destination = markers.find(
      (marker) => marker.activityId === destinationId,
    );
    if (origin === undefined || destination === undefined) {
      continue;
    }
    const transportationMode = originId === STARTING_LOCATION_ID
      ? 'driving'
      : findTransportationMode(items, originId, destinationId);
    const mapRoute = await buildMapRoute(
      originId,
      destinationId,
      origin,
      destination,
      transportationMode,
    );
    if (mapRoute !== null) {
      routes.push(mapRoute);
    }
  }
  return routes;
}

async function buildMapRoute(
  originId: string,
  destinationId: string,
  origin: MapMarker,
  destination: MapMarker,
  transportationMode: TransportationMode,
): Promise<MapRoute | null> {
  const [walking, driving] = await Promise.all([
    getRoute(origin.coordinates, destination.coordinates, 'walking'),
    getRoute(origin.coordinates, destination.coordinates, 'driving'),
  ]);
  const preferred = preferredTravelOption(
    transportationMode,
    walking,
    driving,
  );
  if (preferred === null) {
    return null;
  }
  return {
    fromActivityId: originId,
    toActivityId: destinationId,
    transportationMode,
    encodedPolyline: preferred.encodedPolyline,
    distanceMeters: preferred.distanceMeters,
    durationSeconds: preferred.durationSeconds,
    walking,
    driving,
  };
}

function preferredTravelOption(
  transportationMode: TransportationMode,
  walking: TravelOption | null,
  driving: TravelOption | null,
): TravelOption | null {
  if (transportationMode === 'walking') {
    return walking ?? driving;
  }
  return driving ?? walking;
}

function hasWalkingAndDriving(map: ItineraryMap): boolean {
  return map.routes.every(
    (route) => route.walking !== undefined && route.driving !== undefined,
  );
}

/** Checks whether cached map data matches the itinerary origin. */
function hasExpectedStartingLocation(
  map: ItineraryMap,
  startingLocation?: string,
): boolean {
  const hasStartingLocation = map.markers.some(
    (marker) => marker.activityId === STARTING_LOCATION_ID,
  ) || map.unmappedActivities.some(
    (activity) => activity.activityId === STARTING_LOCATION_ID,
  );
  return hasStartingLocation === Boolean(startingLocation?.trim());
}

/** Finds the transportation mode between two activities. */
function findTransportationMode(
  items: ItineraryItem[],
  fromActivityId: string,
  toActivityId: string,
): TransportationMode {
  const startIndex = items.findIndex((item) => item.id === fromActivityId);
  const endIndex = items.findIndex((item) => item.id === toActivityId);
  if (startIndex < 0 || endIndex <= startIndex) {
    return 'driving';
  }

  const transportation = items
    .slice(startIndex + 1, endIndex)
    .find((item): item is TransportationItem => item.type === 'transportation');
  return transportation?.mode ?? 'driving';
}
