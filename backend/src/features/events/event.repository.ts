// Provides MongoDB operations for storing, updating, and querying events.

import { LiveEventModel } from "./event.model";
import type { LiveEvent } from "./event.types";

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
