// Fetches Ticketmaster events and maps provider responses into internal Event objects.

import { env } from '../../config/env';
import type { LiveEvent } from '../eventTypes';

const MAX_REQUEST_ATTEMPTS = 3;

class TicketmasterRequestError extends Error {
  constructor(readonly status: number) {
    super(`Ticketmaster request failed with status ${status}`);
    this.name = 'TicketmasterRequestError';
  }
}

interface TicketmasterEvent {
  id?: string;
  name?: string;
  info?: string;
  pleaseNote?: string;
  url?: string;
  images?: Array<{
    ratio?: string;
    url?: string;
  }>;
  dates?: {
    start?: { dateTime?: string };
    end?: { dateTime?: string };
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
      location?: {
        longitude?: string;
        latitude?: string;
      };
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

export interface LiveEventPage {
  events: LiveEvent[];
  page: number;
  totalPages: number;
}

/**
 * Removes decorative asterisks and extra spaces from an event title.
 * @param title Ticketmaster event title.
 * @returns Cleaned title for display.
 */
function cleanEventTitle(title: string): string {
  return title.replace(/\*/g, '').replace(/\s+/g, ' ').trim();
}

/**
 * Waits before retrying a request.
 * @param milliseconds Time to wait.
 * @returns A promise that resolves after the delay.
 */
function wait(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

/**
 * Checks whether a failed Ticketmaster request can be retried.
 * @param error Request failure.
 * @returns Whether another attempt should be made.
 */
function isRetryableRequestError(error: unknown): boolean {
  if (error instanceof TicketmasterRequestError) {
    return error.status === 429 || error.status >= 500;
  }
  return error instanceof TypeError;
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

  for (let attempt = 1; attempt <= MAX_REQUEST_ATTEMPTS; attempt += 1) {
    try {
      const response = await fetch(
        `${env.ticketmaster.baseUrl}/events.json?${query.toString()}`,
      );
      if (!response.ok) {
        throw new TicketmasterRequestError(response.status);
      }

      const body = (await response.json()) as TicketmasterResponse;
      return {
        events: body._embedded?.events ?? [],
        page: body.page?.number ?? page,
        totalPages: body.page?.totalPages ?? 0,
        totalElements: body.page?.totalElements ?? 0,
      };
    } catch (error: unknown) {
      if (
        attempt === MAX_REQUEST_ATTEMPTS ||
        !isRetryableRequestError(error)
      ) {
        throw error;
      }
      await wait(attempt * 1000);
    }
  }

  throw new Error('Ticketmaster request failed');
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
  const rawLongitude = venue?.location?.longitude;
  const rawLatitude = venue?.location?.latitude;
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
    venue.city?.name === undefined ||
    rawLongitude === undefined ||
    rawLatitude === undefined
  ) {
    return null;
  }

  const startDateTime = new Date(rawStartDateTime);
  if (Number.isNaN(startDateTime.getTime())) {
    return null;
  }

  const rawEndDateTime = event.dates?.end?.dateTime;
  const parsedEndDateTime = rawEndDateTime === undefined
    ? undefined
    : new Date(rawEndDateTime);
  const endDateTime = parsedEndDateTime !== undefined &&
      !Number.isNaN(parsedEndDateTime.getTime()) &&
      parsedEndDateTime > startDateTime
    ? parsedEndDateTime
    : undefined;

  const longitude = Number(rawLongitude);
  const latitude = Number(rawLatitude);
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) {
    return null;
  }

  const priceRange = event.priceRanges?.[0];
  const description = (event.info ?? event.pleaseNote)?.trim();

  return {
    externalId: event.id,
    title: cleanEventTitle(event.name),
    ...(description === undefined || description === ''
      ? {}
      : { description }),
    imageUrl,
    startDateTime,
    ...(endDateTime === undefined ? {} : { endDateTime }),
    venueName: venue.name,
    city: venue.city.name,
    location: {
      type: "Point",
      coordinates: [longitude, latitude],
    },
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
 * Fetches one Ticketmaster page and maps its valid events.
 * @param page Zero-based Ticketmaster page number.
 * @returns Mapped events and pagination details.
 */
export async function fetchLiveEventPage(
  page: number,
): Promise<LiveEventPage> {
  const ticketmasterPage = await fetchTicketmasterEventPage(page);
  return {
    events: mapTicketmasterEvents(ticketmasterPage.events),
    page: ticketmasterPage.page,
    totalPages: ticketmasterPage.totalPages,
  };
}
