// Coordinates retrieving Ticketmaster events and synchronizing them with MongoDB.

import { MongoClient } from 'mongodb';

import { env } from '../config/env';
import { EventDao } from './eventDao';
import type { EventRepository } from './eventRepository';
import { fetchLiveEventPage } from './ticketmaster/ticketmaster.client';

const MAX_TICKETMASTER_RESULTS = 1000;
const REQUEST_DELAY_MS = 500;

/**
 * Waits before the next Ticketmaster request.
 * @param milliseconds Time to wait.
 * @returns A promise that resolves after the delay.
 */
function wait(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

/**
 * Fetches, maps, and stores all available Ticketmaster event pages.
 * @param repository Event database operations.
 * @returns Counts describing the completed sync.
 */
export async function syncEvents(
  repository: EventRepository,
): Promise<{
  pageCount: number;
  eventCount: number;
  matchedCount: number;
  modifiedCount: number;
  upsertedCount: number;
  expiredCount: number;
  staleCount: number;
}> {
  const syncStartedAt = new Date();
  const pageLimit = Math.ceil(
    MAX_TICKETMASTER_RESULTS / env.ticketmaster.pageSize,
  );
  let pagesToFetch = 1;
  let page = 0;
  let eventCount = 0;
  let matchedCount = 0;
  let modifiedCount = 0;
  let upsertedCount = 0;
  let ticketmasterTotalPages = 0;

  while (page < pagesToFetch) {
    const liveEventPage = await fetchLiveEventPage(page);
    const databaseResult = await repository.upsertEvents(liveEventPage.events);

    eventCount += liveEventPage.events.length;
    matchedCount += databaseResult.matchedCount;
    modifiedCount += databaseResult.modifiedCount;
    upsertedCount += databaseResult.upsertedCount;
    ticketmasterTotalPages = liveEventPage.totalPages;
    pagesToFetch = Math.min(liveEventPage.totalPages, pageLimit);
    page += 1;

    if (page < pagesToFetch) {
      await wait(REQUEST_DELAY_MS);
    }
  }

  const expiredCount = await repository.deleteExpiredEvents(new Date());
  const completeCatalogFetched =
    ticketmasterTotalPages > 0 &&
    ticketmasterTotalPages <= pageLimit &&
    page >= ticketmasterTotalPages;
  const staleCount = completeCatalogFetched
    ? await repository.deleteEventsNotSyncedSince(syncStartedAt)
    : 0;

  return {
    pageCount: page,
    eventCount,
    matchedCount,
    modifiedCount,
    upsertedCount,
    expiredCount,
    staleCount,
  };
}

async function runManualSync(): Promise<void> {
  const mongoClient = new MongoClient(env.mongodbUri);
  await mongoClient.connect();

  try {
    const eventDao = new EventDao(mongoClient.db());
    await eventDao.createIndexes();
    const result = await syncEvents(eventDao);
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
