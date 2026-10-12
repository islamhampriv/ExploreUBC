// Provides MongoDB operations for storing, updating, and querying events.

import { LiveEventModel } from "./event.model";
import type { LiveEvent, LiveEventCandidate } from "./event.types";

export interface NearbyEventQuery {
  longitude: number;
  latitude: number;
  maxDistanceKm: number;
  candidateLimit?: number;
}

/**
 * Inserts new events and replaces existing events with the same external ID.
 * @param events Events to store.
 * @returns Counts of matched, modified, and inserted events.
 */
export async function upsertEvents(events: LiveEvent[]) {
  if (events.length === 0) {
    return { matchedCount: 0, modifiedCount: 0, upsertedCount: 0 };
  }

  const result = await LiveEventModel.bulkWrite(
    events.map((event) => ({
      replaceOne: {
        filter: { externalId: event.externalId },
        replacement: event,
        upsert: true,
      },
    })),
  );

  return {
    matchedCount: result.matchedCount,
    modifiedCount: result.modifiedCount,
    upsertedCount: result.upsertedCount,
  };
}

/**
 * Finds nearby upcoming events for recommendation scoring.
 * @param options User location, distance limit, and candidate limit.
 * @returns Upcoming events with their distance from the user.
 */
export async function findNearbyUpcomingEvents(
  options: NearbyEventQuery,
): Promise<LiveEventCandidate[]> {
  return LiveEventModel.aggregate<LiveEventCandidate>([
    {
      $geoNear: {
        near: {
          type: "Point",
          coordinates: [options.longitude, options.latitude],
        },
        distanceField: "distanceKm",
        distanceMultiplier: 0.001,
        maxDistance: options.maxDistanceKm * 1000,
        spherical: true,
        query: { startDateTime: { $gte: new Date() } },
      },
    },
    { $limit: options.candidateLimit ?? 200 },
    { $project: { _id: 0, __v: 0 } },
  ]).exec();
}
