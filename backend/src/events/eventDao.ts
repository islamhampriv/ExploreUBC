import { type Collection, type Db, ObjectId } from 'mongodb';

import type { EventRepository } from './eventRepository';
import type {
  EventUpsertResult,
  LiveEvent,
  LiveEventCandidate,
  NearbyEventQuery,
} from './eventTypes';

type EventDocument = LiveEvent & {
  _id: ObjectId;
};

export class EventDao implements EventRepository {
  private readonly events: Collection<EventDocument>;

  constructor(database: Db) {
    this.events = database.collection<EventDocument>('liveevents');
  }

  /** Creates the indexes required by event queries. */
  async createIndexes(): Promise<void> {
    await this.events.createIndex({ externalId: 1 }, { unique: true });
    await this.events.createIndex({ startDateTime: 1 });
    await this.events.createIndex({ category: 1, startDateTime: 1 });
    await this.events.createIndex({ location: '2dsphere' });
  }

  /** Inserts new events and replaces events with matching external IDs. */
  async upsertEvents(events: LiveEvent[]): Promise<EventUpsertResult> {
    if (events.length === 0) {
      return { matchedCount: 0, modifiedCount: 0, upsertedCount: 0 };
    }

    const result = await this.events.bulkWrite(
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

  /** Finds nearby upcoming events for recommendation scoring. */
  async findNearbyUpcomingEvents(
    options: NearbyEventQuery,
  ): Promise<LiveEventCandidate[]> {
    return this.events
      .aggregate<LiveEventCandidate>([
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
        { $project: { _id: 0 } },
      ])
      .toArray();
  }
}
