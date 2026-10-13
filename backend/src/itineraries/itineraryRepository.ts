import type {
  CreateItineraryInput,
  ActivityItem,
  TransportationItem,
  Itinerary,
  ItineraryDetails,
  ItineraryDetailsUpdate,
  ItineraryFilters,
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

  removeItinerary(userId: string, itineraryId: string): Promise<boolean>;
}
