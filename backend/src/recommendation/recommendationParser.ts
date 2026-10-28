import { TRANSPORTATION_MODES, type TransportationMode } from '../users/userTypes';
import type {
  CreateItineraryItemInput,
  CreateActivityInput,
  CreateTransportationInput,
} from '../itineraries/itineraryTypes';
import { InvalidRecommendationResponseError } from './recommendationErrors';
import type { GeneratedItineraryInput } from './recommendationTypes';

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function requiredString(
  value: unknown,
  path: string,
  details: string[],
): string | undefined {
  if (typeof value !== 'string' || value.trim() === '') {
    details.push(`${path} must be a non-empty string.`);
    return undefined;
  }
  return value.trim();
}

function optionalString(value: unknown): string | undefined {
  return typeof value === 'string' ? value.trim() : undefined;
}

function isoTime(
  value: unknown,
  path: string,
  details: string[],
): string | undefined {
  if (typeof value !== 'string' || Number.isNaN(Date.parse(value))) {
    details.push(`${path} must be a valid ISO-8601 timestamp.`);
    return undefined;
  }
  return new Date(value).toISOString();
}

function parseActivity(
  item: Record<string, unknown>,
  path: string,
  details: string[],
): CreateActivityInput | undefined {
  const name = requiredString(item.name, `${path}.name`, details);
  const location = requiredString(item.location, `${path}.location`, details);
  const startTime = isoTime(item.startTime, `${path}.startTime`, details);
  const endTime = isoTime(item.endTime, `${path}.endTime`, details);
  if (
    startTime !== undefined && endTime !== undefined &&
    Date.parse(startTime) >= Date.parse(endTime)
  ) {
    details.push(`${path}.startTime must be before endTime.`);
  }
  if (
    name === undefined || location === undefined ||
    startTime === undefined || endTime === undefined
  ) return undefined;

  return {
    type: 'activity',
    name,
    image: typeof item.image === 'string' ? item.image : '',
    location,
    startTime,
    endTime,
    ...(optionalString(item.description) === undefined
      ? {}
      : { description: optionalString(item.description)! }),
    ...(optionalString(item.category) === undefined
      ? {}
      : { category: optionalString(item.category)! }),
  };
}

function parseTransportation(
  item: Record<string, unknown>,
  path: string,
  details: string[],
): CreateTransportationInput | undefined {
  const startLocation = requiredString(
    item.startLocation,
    `${path}.startLocation`,
    details,
  );
  const endLocation = requiredString(item.endLocation, `${path}.endLocation`, details);
  const startTime = isoTime(item.startTime, `${path}.startTime`, details);
  const endTime = isoTime(item.endTime, `${path}.endTime`, details);
  const mode = item.mode;
  const validMode = typeof mode === 'string' && TRANSPORTATION_MODES.includes(
    mode as TransportationMode,
  );
  if (!validMode) details.push(`${path}.mode is invalid.`);
  const validDistance = typeof item.distanceMeters === 'number' &&
    Number.isFinite(item.distanceMeters) && item.distanceMeters >= 0;
  if (!validDistance) details.push(`${path}.distanceMeters must be non-negative.`);
  if (
    startTime !== undefined && endTime !== undefined &&
    Date.parse(startTime) >= Date.parse(endTime)
  ) {
    details.push(`${path}.startTime must be before endTime.`);
  }
  if (
    startLocation === undefined || endLocation === undefined ||
    startTime === undefined || endTime === undefined ||
    !validMode || !validDistance
  ) return undefined;

  return {
    type: 'transportation',
    mode: mode as TransportationMode,
    startLocation,
    endLocation,
    distanceMeters: item.distanceMeters as number,
    startTime,
    endTime,
  };
}

function parseItems(
  value: unknown,
  itineraryIndex: number,
  details: string[],
): CreateItineraryItemInput[] {
  if (!Array.isArray(value) || value.length === 0) {
    details.push(`itineraries[${itineraryIndex}].items must be a non-empty array.`);
    return [];
  }

  const items: CreateItineraryItemInput[] = [];
  value.forEach((item, itemIndex) => {
    const path = `itineraries[${itineraryIndex}].items[${itemIndex}]`;
    if (!isObject(item)) {
      details.push(`${path} must be an object.`);
      return;
    }
    if (item.type === 'activity') {
      const activity = parseActivity(item, path, details);
      if (activity !== undefined) items.push(activity);
      return;
    }
    if (item.type === 'transportation') {
      const transportation = parseTransportation(item, path, details);
      if (transportation !== undefined) items.push(transportation);
      return;
    }
    details.push(`${path}.type must be activity or transportation.`);
  });
  return items;
}

function jsonPayload(text: string): unknown {
  const trimmed = text.trim().replace(/^```(?:json)?\s*/iu, '').replace(/\s*```$/u, '');
  try {
    return JSON.parse(trimmed) as unknown;
  } catch {
    const objectStart = trimmed.indexOf('{');
    const objectEnd = trimmed.lastIndexOf('}');
    if (objectStart >= 0 && objectEnd > objectStart) {
      try {
        return JSON.parse(trimmed.slice(objectStart, objectEnd + 1)) as unknown;
      } catch {
        // Fall through to the domain-specific error below.
      }
    }
    throw new InvalidRecommendationResponseError(['Response is not valid JSON.']);
  }
}

export function parseItineraries(text: string): GeneratedItineraryInput[] {
  const payload = jsonPayload(text);
  const value = isObject(payload) ? payload.itineraries : undefined;
  if (!Array.isArray(value) || value.length !== 4) {
    throw new InvalidRecommendationResponseError([
      'Response must contain exactly four itineraries.',
    ]);
  }

  const details: string[] = [];
  const itineraries = value.flatMap((candidate, index) => {
    if (!isObject(candidate)) {
      details.push(`itineraries[${index}] must be an object.`);
      return [];
    }
    const name = requiredString(candidate.name, `itineraries[${index}].name`, details);
    const items = parseItems(candidate.items, index, details);
    if (!items.some((item) => item.type === 'activity')) {
      details.push(`itineraries[${index}] must include an activity.`);
    }
    if (!items.some((item) => item.type === 'transportation')) {
      details.push(`itineraries[${index}] must include transportation.`);
    }
    if (name === undefined || items.length === 0) {
      return [];
    }
    const itinerary: GeneratedItineraryInput = {
      name,
      items,
      ...(optionalString(candidate.description) === undefined
        ? {}
        : { description: optionalString(candidate.description)! }),
    };
    return [itinerary];
  });

  if (details.length > 0 || itineraries.length !== 4) {
    throw new InvalidRecommendationResponseError(details);
  }
  return itineraries;
}
