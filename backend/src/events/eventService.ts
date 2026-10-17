// Contains event recommendation, filtering, ranking, and other business logic.

import type { EventRepository } from './eventRepository';
import type {
  LiveEventCandidate,
  RecommendedLiveEvent,
} from './eventTypes';
import { UserNotFoundError } from '../users/userErrors';
import type { UserRepository } from '../users/userRepository';
import type { InterestPreference } from '../users/userTypes';

const RECOMMENDATION_THRESHOLD = 40;
const CANDIDATE_LIMIT = 200;

const INTEREST_CATEGORIES: Record<InterestPreference, string[]> = {
  food: ['Miscellaneous'],
  art: ['Arts & Theatre'],
  outdoors: ['Sports'],
  markets: ['Miscellaneous'],
  live_music: ['Music'],
  gaming: ['Sports'],
};

export interface EventRecommendationOptions {
  longitude: number;
  latitude: number;
  maxDistanceKm: number;
  page?: number;
  limit?: number;
}

/**
 * Calculates an event's recommendation score.
 * @param event Candidate event with its distance from the user.
 * @param preferredCategories Normalized category preferences.
 * @param maxDistanceKm Maximum allowed distance.
 * @returns A score from 0 to 100.
 */
function calculateRecommendationScore(
  event: LiveEventCandidate,
  preferredCategories: Set<string>,
  maxDistanceKm: number,
): number {
  const categoryMatches =
    preferredCategories.size === 0 ||
    preferredCategories.has(event.category.toLowerCase());
  const categoryScore = categoryMatches ? 60 : 0;
  const distanceScore = Math.max(
    0,
    25 * (1 - event.distanceKm / maxDistanceKm),
  );
  const daysUntilEvent = Math.max(
    0,
    (event.startDateTime.getTime() - Date.now()) / (1000 * 60 * 60 * 24),
  );
  const timeScore = Math.max(0, 15 * (1 - daysUntilEvent / 30));

  return Math.round(categoryScore + distanceScore + timeScore);
}

export class EventService {
  constructor(
    private readonly eventRepository: EventRepository,
    private readonly userRepository: UserRepository,
  ) {}

  /**
   * Finds and ranks events using a user's stored interest.
   * @param userId User whose preferences are used.
   * @param options Current location, distance, and pagination.
   * @returns Recommended events for the requested page.
   * @throws UserNotFoundError If the user does not exist.
   */
  async getRecommendedEvents(
    userId: string,
    options: EventRecommendationOptions,
  ): Promise<RecommendedLiveEvent[]> {
    const page = options.page ?? 1;
    const limit = options.limit ?? 20;

    if (page < 1 || limit < 1 || options.maxDistanceKm <= 0) {
      throw new Error('Invalid event recommendation options');
    }

    const preferences = await this.userRepository.findPreferences(userId);
    if (preferences === null) {
      throw new UserNotFoundError(userId);
    }

    const preferredCategories = new Set(
      INTEREST_CATEGORIES[preferences.interest].map((category) =>
        category.toLowerCase(),
      ),
    );
    const candidates = await this.eventRepository.findNearbyUpcomingEvents({
      longitude: options.longitude,
      latitude: options.latitude,
      maxDistanceKm: options.maxDistanceKm,
      candidateLimit: CANDIDATE_LIMIT,
    });

    const recommendedEvents = candidates
      .map((event) => ({
        ...event,
        recommendationScore: calculateRecommendationScore(
          event,
          preferredCategories,
          options.maxDistanceKm,
        ),
      }))
      .filter((event) => event.recommendationScore >= RECOMMENDATION_THRESHOLD)
      .sort(
        (left, right) =>
          right.recommendationScore - left.recommendationScore ||
          left.startDateTime.getTime() - right.startDateTime.getTime(),
      );

    const startIndex = (page - 1) * limit;
    return recommendedEvents.slice(startIndex, startIndex + limit);
  }
}
