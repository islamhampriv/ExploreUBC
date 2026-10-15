import type {
  EventUpsertResult,
  LiveEvent,
  LiveEventCandidate,
  NearbyEventQuery,
} from './eventTypes';

export interface EventRepository {
  createIndexes(): Promise<void>;

  upsertEvents(events: LiveEvent[]): Promise<EventUpsertResult>;

  findNearbyUpcomingEvents(
    options: NearbyEventQuery,
  ): Promise<LiveEventCandidate[]>;
}
