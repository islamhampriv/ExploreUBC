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
  UnmappedActivity,
} from './mapTypes';

type MarkerGenerationResult = {
  markers: MapMarker[];
  unmappedActivities: UnmappedActivity[];
};

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
    if (cachedMap?.itineraryUpdatedAt === itinerary.updatedAt) {
      return cachedMap;
    }

    try {
      const orderedItems = orderItemsByStartTime(itinerary.items);
      const { markers, unmappedActivities } =
        await createMarkers(orderedItems);
      const routes = await createRoutes(orderedItems, markers);
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
): Promise<MarkerGenerationResult> {
  const markers: MapMarker[] = [];
  const unmappedActivities: UnmappedActivity[] = [];
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
): Promise<MapRoute[]> {
  const routes: MapRoute[] = [];
  const activities = items.filter(
    (item): item is ActivityItem => item.type === 'activity',
  );
  for (let index = 0; index < activities.length - 1; index += 1) {
    const originActivity = activities[index];
    const destinationActivity = activities[index + 1];
    if (originActivity === undefined || destinationActivity === undefined) {
      continue;
    }
    const origin = markers.find(
      (marker) => marker.activityId === originActivity.id,
    );
    const destination = markers.find(
      (marker) => marker.activityId === destinationActivity.id,
    );
    if (origin === undefined || destination === undefined) {
      continue;
    }
    const transportationMode = findTransportationMode(
      items,
      originActivity.id,
      destinationActivity.id,
    );
    const calculatedRoute = await getRoute(
      origin.coordinates,
      destination.coordinates,
      transportationMode,
    );
    if (calculatedRoute === null) {
      continue;
    }
    routes.push({
      fromActivityId: originActivity.id,
      toActivityId: destinationActivity.id,
      ...calculatedRoute,
    });
  }
  return routes;
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
