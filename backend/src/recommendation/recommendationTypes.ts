import type { CreateItineraryInput, Itinerary } from '../itineraries/itineraryTypes';
import type { UserPreferences } from '../users/userTypes';

export type RecommendationProfile = {
  start_location: string;
  preferences: UserPreferences;
};

export type RecommendedItineraryInput = Omit<
  CreateItineraryInput,
  'ownerId' | 'saved' | 'inRecommendation' | 'score'
>;

export type RecommendationSet = Itinerary[];

export interface ItineraryGenerator {
  generateItineraries(profile: RecommendationProfile): Promise<string>;
}

export type SoclaasResponsesApiResponse = {
  status?: string;
  output_text?: string;
  output?: Array<{
    content?: Array<{
      type?: string;
      text?: string;
    }>;
  }>;
  error?: string | { message?: string };
};
