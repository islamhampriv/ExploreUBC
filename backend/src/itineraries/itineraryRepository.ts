import type {
  ActivityUpdate,
  CreateItineraryInput,
  ActivityItem,
  TransportationItem,
  TransportationSegmentUpdate,
  Itinerary,
  ItineraryDetails,
  ItineraryDetailsUpdate,
  ItineraryFilters,
  ItineraryItem,
} from './itineraryTypes';

export interface ItineraryRepository {
  insertItinerary(
    userId: string,
    itinerary: CreateItineraryInput,
  ): Promise<Itinerary>;

  findItineraryDetails(
    userId: string,
    itineraryId: string,
  ): Promise<ItineraryDetails | null>;

  findItinerary(userId: string, itineraryId: string): Promise<Itinerary | null>;

  findItineraries(
    userId: string,
    filters: ItineraryFilters,
  ): Promise<Itinerary[]>;

  updateItineraryDetails(
    userId: string,
    itineraryId: string,
    details: ItineraryDetailsUpdate,
  ): Promise<boolean>;

  insertEmbeddedActivity(
    userId: string,
    itineraryId: string,
    activity: ActivityItem,
  ): Promise<boolean>;

  insertEmbeddedTransportationSegment(
    userId: string,
    itineraryId: string,
    segment: TransportationItem,
  ): Promise<boolean>;

  removeEmbeddedActivity(
    userId: string,
    itineraryId: string,
    activityId: string,
  ): Promise<boolean>;

  removeEmbeddedTransportationSegment(
    userId: string,
    itineraryId: string,
    segmentId: string,
  ): Promise<boolean>;

  updateEmbeddedActivity(
    userId: string,
    itineraryId: string,
    activityId: string,
    update: ActivityUpdate,
  ): Promise<boolean>;

  updateEmbeddedTransportationSegment(
    userId: string,
    itineraryId: string,
    segmentId: string,
    update: TransportationSegmentUpdate,
  ): Promise<boolean>;

  replaceEmbeddedItems?(
    userId: string,
    itineraryId: string,
    removedItemIds: string[],
    replacement: ItineraryItem,
  ): Promise<boolean>;

  removeItinerary(userId: string, itineraryId: string): Promise<boolean>;
}
