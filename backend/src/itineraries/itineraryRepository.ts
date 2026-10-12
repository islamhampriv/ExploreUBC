import type {
  CreateItineraryInput,
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

  removeItinerary(userId: string, itineraryId: string): Promise<boolean>;
}
