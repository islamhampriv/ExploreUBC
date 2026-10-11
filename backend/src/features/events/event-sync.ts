// Coordinates retrieving Ticketmaster events and synchronizing them with MongoDB.

import {
  connectToDatabase,
  disconnectFromDatabase,
} from "../../config/database";
import { upsertEvents } from "./event.repository";
import { fetchLiveEvents } from "./ticketmaster/ticketmaster.client";

/**
 * Fetches, maps, and stores one page of Ticketmaster events.
 * @param page Zero-based Ticketmaster page number.
 * @returns Counts describing the completed sync.
 */
export async function syncEventPage(page = 0) {
  const liveEvents = await fetchLiveEvents(page);
  const databaseResult = await upsertEvents(liveEvents);

  return {
    page,
    eventCount: liveEvents.length,
    ...databaseResult,
  };
}

async function runManualSync(): Promise<void> {
  await connectToDatabase();

  try {
    const result = await syncEventPage();
    console.log("Event sync completed:", result);
  } finally {
    await disconnectFromDatabase();
  }
}

if (require.main === module) {
  void runManualSync().catch((error: unknown) => {
    console.error("Event sync failed:", error);
    process.exitCode = 1;
  });
}
