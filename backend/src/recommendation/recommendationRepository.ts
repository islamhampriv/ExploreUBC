import type { Itinerary } from '../itineraries/itineraryTypes';
import type { RecommendedItineraryInput } from './recommendationTypes';

export interface RecommendationRepository {
  findLatestRecommendations(userId: string): Promise<Itinerary[]>;
  replaceLatestRecommendations(
    userId: string,
    itineraries: RecommendedItineraryInput[],
  ): Promise<Itinerary[]>;
}
