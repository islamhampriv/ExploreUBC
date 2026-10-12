// Contains event recommendation, filtering, ranking, and other business logic.

import { findNearbyUpcomingEvents } from "./event.repository";
import type {
  LiveEventCandidate,
  RecommendedLiveEvent,
} from "./event.types";

const RECOMMENDATION_THRESHOLD = 40;
const CANDIDATE_LIMIT = 200;

export interface EventRecommendationOptions {
  preferredCategories: string[];
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

/**
 * Finds and ranks events recommended for a user.
 * @param options User preferences, location, distance, and pagination.
 * @returns Recommended events for the requested page.
 */
export async function getRecommendedEvents(
  options: EventRecommendationOptions,
): Promise<RecommendedLiveEvent[]> {
  const page = options.page ?? 1;
  const limit = options.limit ?? 20;

  if (page < 1 || limit < 1 || options.maxDistanceKm <= 0) {
    throw new Error("Invalid event recommendation options");
  }

  const preferredCategories = new Set(
    options.preferredCategories.map((category) => category.trim().toLowerCase()),
  );
  const candidates = await findNearbyUpcomingEvents({
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
