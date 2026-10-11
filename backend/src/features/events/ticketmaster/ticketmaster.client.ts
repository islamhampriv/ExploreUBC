// Fetches Ticketmaster events and maps provider responses into internal Event objects.

import { env } from "../../../config/env";
import type { LiveEvent } from "../event.types";

interface TicketmasterEvent {
  id?: string;
  name?: string;
  url?: string;
  images?: Array<{
    ratio?: string;
    url?: string;
  }>;
  dates?: {
    start?: { dateTime?: string };
  };
  classifications?: Array<{
    primary?: boolean;
    segment?: { name?: string };
  }>;
  priceRanges?: Array<{
    min?: number;
    max?: number;
    currency?: string;
  }>;
  _embedded?: {
    venues?: Array<{
      name?: string;
      city?: { name?: string };
    }>;
  };
}

interface TicketmasterResponse {
  _embedded?: { events?: TicketmasterEvent[] };
  page?: {
    number?: number;
    totalPages?: number;
    totalElements?: number;
  };
}

interface TicketmasterEventPage {
  events: TicketmasterEvent[];
  page: number;
  totalPages: number;
  totalElements: number;
}

/**
 * Fetches one page of raw events from Ticketmaster.
 * @param page Zero-based page number.
 * @returns Raw events and pagination details.
 * @throws If configuration is invalid or the request fails.
 */
async function fetchTicketmasterEventPage(
  page = 0,
): Promise<TicketmasterEventPage> {
  if (env.ticketmaster.apiKey === undefined || env.ticketmaster.apiKey === "") {
    throw new Error("TICKETMASTER_API_KEY is not configured");
  }

  if (!Number.isInteger(page) || page < 0) {
    throw new Error(`Invalid Ticketmaster page: ${page}`);
  }

  const query = new URLSearchParams({
    apikey: env.ticketmaster.apiKey,
    city: env.ticketmaster.city,
    stateCode: env.ticketmaster.stateCode,
    countryCode: env.ticketmaster.countryCode,
    size: String(env.ticketmaster.pageSize),
    page: String(page),
    sort: "date,asc",
  });

  const response = await fetch(
    `${env.ticketmaster.baseUrl}/events.json?${query.toString()}`,
  );
  if (!response.ok) {
    throw new Error(
      `Ticketmaster request failed with status ${response.status}`,
    );
  }

  const body = (await response.json()) as TicketmasterResponse;

  return {
    events: body._embedded?.events ?? [],
    page: body.page?.number ?? page,
    totalPages: body.page?.totalPages ?? 0,
    totalElements: body.page?.totalElements ?? 0,
  };
}

/**
 * Maps one Ticketmaster event to a LiveEvent.
 * @param event Raw Ticketmaster event.
 * @returns The mapped event, or null if required data is missing.
 */
function mapTicketmasterEvent(event: TicketmasterEvent): LiveEvent | null {
  const venue = event._embedded?.venues?.[0];
  const imageUrl =
    event.images?.find((image) => image.ratio === "16_9")?.url ??
    event.images?.[0]?.url;
  const rawStartDateTime = event.dates?.start?.dateTime;
  const classification =
    event.classifications?.find((item) => item.primary) ??
    event.classifications?.[0];

  if (
    event.id === undefined ||
    event.name === undefined ||
    event.url === undefined ||
    imageUrl === undefined ||
    rawStartDateTime === undefined ||
    venue?.name === undefined ||
    venue.city?.name === undefined
  ) {
    return null;
  }

  const startDateTime = new Date(rawStartDateTime);
  if (Number.isNaN(startDateTime.getTime())) {
    return null;
  }

  const priceRange = event.priceRanges?.[0];

  return {
    externalId: event.id,
    title: event.name.trim(),
    imageUrl,
    startDateTime,
    venueName: venue.name,
    city: venue.city.name,
    category: classification?.segment?.name ?? "Other",
    ticketUrl: event.url,
    ...(priceRange?.min === undefined ? {} : { priceMin: priceRange.min }),
    ...(priceRange?.max === undefined ? {} : { priceMax: priceRange.max }),
    ...(priceRange?.currency === undefined
      ? {}
      : { currency: priceRange.currency }),
    lastSyncedAt: new Date(),
  };
}

/**
 * Maps valid Ticketmaster events to LiveEvents.
 * @param events Raw Ticketmaster events.
 * @returns Successfully mapped LiveEvents.
 */
function mapTicketmasterEvents(events: TicketmasterEvent[]): LiveEvent[] {
  return events
    .map(mapTicketmasterEvent)
    .filter((event): event is LiveEvent => event !== null);
}

/**
 * Fetches one page of Ticketmaster events as LiveEvents.
 * @param page Zero-based Ticketmaster page number.
 * @returns Valid events mapped to the internal type.
 */
export async function fetchLiveEvents(page = 0): Promise<LiveEvent[]> {
  const ticketmasterPage = await fetchTicketmasterEventPage(page);
  return mapTicketmasterEvents(ticketmasterPage.events);
}
