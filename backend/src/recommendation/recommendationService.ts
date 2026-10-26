import type { Itinerary } from '../itineraries/itineraryTypes';
import type { UserRepository } from '../users/userRepository';
import {
  RecommendationProfileIncompleteError,
  RecommendationUserNotFoundError,
} from './recommendationErrors';
import { parseItineraries } from './recommendationParser';
import type { RecommendationRepository } from './recommendationRepository';
import type { ItineraryGenerator, ItineraryRanker } from './recommendationTypes';

function byDescendingInterestSimilarity(
  left: Itinerary,
  right: Itinerary,
): number {
  return (right.score.interestSimilarity ?? Number.NEGATIVE_INFINITY) -
    (left.score.interestSimilarity ?? Number.NEGATIVE_INFINITY);
}

export class RecommendationService {
  constructor(
    private readonly repository: RecommendationRepository,
    private readonly userRepository: UserRepository,
    private readonly generator: ItineraryGenerator,
    private readonly ranker: ItineraryRanker,
  ) {}

  async findLatestRecommendations(userId: string): Promise<Itinerary[]> {
    const itineraries = await this.repository.findLatestRecommendations(userId);
    return [...itineraries].sort(byDescendingInterestSimilarity);
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
    const rankedItineraries = await this.ranker.rankItineraries(
      itineraries,
      user.preferences.interest,
    );
    const recommendations = await this.repository.replaceLatestRecommendations(
      userId,
      rankedItineraries,
    );
    return [...recommendations].sort(byDescendingInterestSimilarity);
  }

  parseItineraries(itineraryJson: string) {
    return parseItineraries(itineraryJson);
  }
}
