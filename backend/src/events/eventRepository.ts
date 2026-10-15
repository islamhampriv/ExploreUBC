import type {
  EventUpsertResult,
  LiveEvent,
  LiveEventCandidate,
  NearbyEventQuery,
} from './eventTypes';

export interface EventRepository {
  /** Creates indexes required by event queries. */
  createIndexes(): Promise<void>;

  /** Inserts new events and updates existing events. */
  upsertEvents(events: LiveEvent[]): Promise<EventUpsertResult>;

  /** Removes events that started before the supplied time. */
  deleteExpiredEvents(before: Date): Promise<number>;

  /** Removes events that were not updated during the latest complete sync. */
  deleteEventsNotSyncedSince(syncStartedAt: Date): Promise<number>;

  /** Finds nearby upcoming events. */
  findNearbyUpcomingEvents(
    options: NearbyEventQuery,
  ): Promise<LiveEventCandidate[]>;
}
