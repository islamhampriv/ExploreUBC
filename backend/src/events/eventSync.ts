// Coordinates retrieving Ticketmaster events and synchronizing them with MongoDB.

import { MongoClient } from 'mongodb';

import { env } from '../config/env';
import { EventDao } from './eventDao';
import type { EventRepository } from './eventRepository';
import { fetchLiveEvents } from './ticketmaster/ticketmaster.client';

/**
 * Fetches, maps, and stores one page of Ticketmaster events.
 * @param repository Event database operations.
 * @param page Zero-based Ticketmaster page number.
 * @returns Counts describing the completed sync.
 */
export async function syncEventPage(
  repository: EventRepository,
  page = 0,
): Promise<{
  page: number;
  eventCount: number;
  matchedCount: number;
  modifiedCount: number;
  upsertedCount: number;
}> {
  const liveEvents = await fetchLiveEvents(page);
  const databaseResult = await repository.upsertEvents(liveEvents);

  return {
    page,
    eventCount: liveEvents.length,
    ...databaseResult,
  };
}

async function runManualSync(): Promise<void> {
  const mongoClient = new MongoClient(env.mongodbUri);
  await mongoClient.connect();

  try {
    const eventDao = new EventDao(mongoClient.db());
    await eventDao.createIndexes();
    const result = await syncEventPage(eventDao);
    console.log('Event sync completed:', result);
  } finally {
    await mongoClient.close();
  }
}

if (require.main === module) {
  void runManualSync().catch((error: unknown) => {
    console.error('Event sync failed:', error);
    process.exitCode = 1;
  });
}
