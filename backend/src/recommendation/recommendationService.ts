import type { Itinerary } from '../itineraries/itineraryTypes';
import type { UserRepository } from '../users/userRepository';
import {
  RecommendationProfileIncompleteError,
  RecommendationUserNotFoundError,
} from './recommendationErrors';
import { parseItineraries } from './recommendationParser';
import type { RecommendationRepository } from './recommendationRepository';
import type { ItineraryGenerator } from './recommendationTypes';

export class RecommendationService {
  constructor(
    private readonly repository: RecommendationRepository,
    private readonly userRepository: UserRepository,
    private readonly generator: ItineraryGenerator,
  ) {}

  async findLatestRecommendations(userId: string): Promise<Itinerary[]> {
    return this.repository.findLatestRecommendations(userId);
  }

  async generateRecommendations(userId: string): Promise<Itinerary[]> {
    const user = await this.userRepository.findUser(userId);
    if (user === null) throw new RecommendationUserNotFoundError();
    if (user.start_location.trim() === '') {
      throw new RecommendationProfileIncompleteError();
    }

    const itineraryJson = await this.generator.generateItineraries({
      start_location: user.start_location,
      preferences: user.preferences,
    });
    const itineraries = parseItineraries(itineraryJson).map((itinerary) => ({
      ...itinerary,
      startingLocation: user.start_location,
    }));
    return this.repository.replaceLatestRecommendations(userId, itineraries);
  }

  parseItineraries(itineraryJson: string) {
    return parseItineraries(itineraryJson);
  }
}
