import { TRANSPORTATION_MODES } from '../users/userTypes';
import {
  InvalidItineraryDetailsError,
  InvalidItineraryFiltersError,
} from './itineraryErrors';
import type {
  CreateActivityInput,
  CreateItineraryInput,
  CreateItineraryItemInput,
  CreateTransportationInput,
  ItineraryDetailsUpdate,
  ItineraryFilters,
  ItineraryScore,
} from './itineraryTypes';

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function addUnexpectedFields(
  value: Record<string, unknown>,
  allowedFields: readonly string[],
  path: string,
  details: string[],
): void {
  const allowed = new Set(allowedFields);
  for (const field of Object.keys(value)) {
    if (!allowed.has(field)) details.push(`Unexpected field '${path}${field}'.`);
  }
}

function requiredString(
  value: unknown,
  field: string,
  maxLength: number,
  details: string[],
): string | undefined {
  if (
    typeof value !== 'string' ||
    value.trim().length === 0 ||
    value.trim().length > maxLength
  ) {
    details.push(`${field} must contain between 1 and ${maxLength} characters.`);
    return undefined;
  }
  return value.trim();
}

function optionalString(
  value: unknown,
  field: string,
  maxLength: number,
  details: string[],
): string | undefined {
  if (value === undefined) return undefined;
  if (typeof value !== 'string' || value.length > maxLength) {
    details.push(`${field} must be a string with at most ${maxLength} characters.`);
    return undefined;
  }
  return value;
}

function timestamp(
  value: unknown,
  field: string,
  details: string[],
): string | undefined {
  if (typeof value !== 'string' || Number.isNaN(Date.parse(value))) {
    details.push(`${field} must be a valid ISO 8601 timestamp string.`);
    return undefined;
  }
  return new Date(value).toISOString();
}

function validateTimeOrder(
  startTime: string | undefined,
  endTime: string | undefined,
  path: string,
  details: string[],
): void {
  if (
    startTime !== undefined &&
    endTime !== undefined &&
    Date.parse(startTime) >= Date.parse(endTime)
  ) {
    details.push(`${path}startTime must be earlier than ${path}endTime.`);
  }
}

function parseActivity(
  value: Record<string, unknown>,
  index: number,
  details: string[],
): CreateActivityInput | undefined {
  const path = `items[${index}].`;
  addUnexpectedFields(
    value,
    ['type', 'name', 'image', 'description', 'category', 'location', 'startTime', 'endTime'],
    path,
    details,
  );

  const name = requiredString(value.name, `${path}name`, 200, details);
  const image = requiredString(value.image, `${path}image`, 2_000, details);
  const location = requiredString(value.location, `${path}location`, 500, details);
  const description = optionalString(value.description, `${path}description`, 1_000, details);
  const category = optionalString(value.category, `${path}category`, 100, details);
  const startTime = timestamp(value.startTime, `${path}startTime`, details);
  const endTime = timestamp(value.endTime, `${path}endTime`, details);
  validateTimeOrder(startTime, endTime, path, details);

  if (
    name === undefined || image === undefined ||
    location === undefined || startTime === undefined || endTime === undefined
  ) return undefined;

  return {
    type: 'activity',
    name,
    image,
    location,
    startTime,
    endTime,
    ...(description === undefined ? {} : { description }),
    ...(category === undefined ? {} : { category }),
  };
}

function parseTransportation(
  value: Record<string, unknown>,
  index: number,
  details: string[],
): CreateTransportationInput | undefined {
  const path = `items[${index}].`;
  addUnexpectedFields(
    value,
    ['type', 'mode', 'startLocation', 'endLocation', 'distanceMeters', 'startTime', 'endTime'],
    path,
    details,
  );

  const startLocation = requiredString(value.startLocation, `${path}startLocation`, 500, details);
  const endLocation = requiredString(value.endLocation, `${path}endLocation`, 500, details);
  const startTime = timestamp(value.startTime, `${path}startTime`, details);
  const endTime = timestamp(value.endTime, `${path}endTime`, details);
  validateTimeOrder(startTime, endTime, path, details);

  const mode = value.mode;
  const validMode = typeof mode === 'string' && TRANSPORTATION_MODES.includes(
    mode as (typeof TRANSPORTATION_MODES)[number],
  );
  if (!validMode) {
    details.push(`${path}mode must be one of: ${TRANSPORTATION_MODES.join(', ')}.`);
  }

  const distanceMeters = value.distanceMeters;
  const validDistance = typeof distanceMeters === 'number' &&
    Number.isFinite(distanceMeters) && distanceMeters >= 0;
  if (!validDistance) {
    details.push(`${path}distanceMeters must be a non-negative number.`);
  }

  if (
    startLocation === undefined || endLocation === undefined ||
    startTime === undefined || endTime === undefined || !validMode || !validDistance
  ) return undefined;

  return {
    type: 'transportation',
    mode: mode as (typeof TRANSPORTATION_MODES)[number],
    startLocation,
    endLocation,
    distanceMeters: distanceMeters as number,
    startTime,
    endTime,
  };
}

function parseItems(
  value: unknown,
  details: string[],
): CreateItineraryItemInput[] {
  if (!Array.isArray(value)) {
    details.push('items must be an array.');
    return [];
  }

  const items: CreateItineraryItemInput[] = [];
  value.forEach((item, index) => {
    if (!isObject(item)) {
      details.push(`items[${index}] must be an object.`);
      return;
    }

    let parsed: CreateItineraryItemInput | undefined;
    if (item.type === 'activity') parsed = parseActivity(item, index, details);
    else if (item.type === 'transportation') parsed = parseTransportation(item, index, details);
    else details.push(`items[${index}].type must be 'activity' or 'transportation'.`);

    if (parsed !== undefined) items.push(parsed);
  });
  return items;
}

function parseScore(value: unknown, details: string[]): ItineraryScore {
  if (!isObject(value)) {
    details.push('score must be an object containing numeric values.');
    return {};
  }

  const score: ItineraryScore = {};
  for (const [key, scoreValue] of Object.entries(value)) {
    if (typeof scoreValue !== 'number' || !Number.isFinite(scoreValue)) {
      details.push(`score.${key} must be a finite number.`);
    } else score[key] = scoreValue;
  }
  return score;
}

export function parseItinerary(
  value: unknown,
  userId: string,
): CreateItineraryInput {
  if (!isObject(value)) {
    throw new InvalidItineraryDetailsError(['Request body must be an Itinerary object.']);
  }

  const details: string[] = [];
  addUnexpectedFields(
    value,
    ['ownerId', 'name', 'description', 'startingLocation', 'items', 'saved', 'inRecommendation', 'score'],
    '',
    details,
  );

  const ownerId = requiredString(value.ownerId, 'ownerId', 200, details);
  if (ownerId !== undefined && ownerId !== userId) {
    details.push('ownerId must match the userId route parameter.');
  }

  const name = requiredString(value.name, 'name', 100, details);
  const description = optionalString(value.description, 'description', 1_000, details);
  const startingLocation = optionalString(value.startingLocation, 'startingLocation', 500, details);
  const items = parseItems(value.items, details);
  const score = parseScore(value.score, details);

  if (typeof value.saved !== 'boolean') details.push('saved must be a boolean.');
  if (typeof value.inRecommendation !== 'boolean') {
    details.push('inRecommendation must be a boolean.');
  }

  if (
    details.length > 0 || ownerId === undefined || name === undefined
  ) throw new InvalidItineraryDetailsError(details);

  return {
    ownerId,
    name,
    items,
    saved: value.saved as boolean,
    inRecommendation: value.inRecommendation as boolean,
    score,
    ...(description === undefined ? {} : { description }),
    ...(startingLocation === undefined ? {} : { startingLocation }),
  };
}

export function parseItineraryDetailsUpdate(
  value: unknown,
): ItineraryDetailsUpdate {
  if (!isObject(value)) {
    throw new InvalidItineraryDetailsError([
      'Request body must be an ItineraryDetailsUpdate object.',
    ]);
  }

  const details: string[] = [];
  addUnexpectedFields(
    value,
    ['name', 'description', 'startingLocation'],
    '',
    details,
  );
  const name = value.name === undefined
    ? undefined
    : requiredString(value.name, 'name', 100, details);
  const description = optionalString(
    value.description,
    'description',
    1_000,
    details,
  );
  const startingLocation = optionalString(
    value.startingLocation,
    'startingLocation',
    500,
    details,
  );

  if (
    name === undefined &&
    description === undefined &&
    startingLocation === undefined &&
    details.length === 0
  ) {
    details.push('At least one itinerary detail must be provided.');
  }

  if (details.length > 0) {
    throw new InvalidItineraryDetailsError(details);
  }

  return {
    ...(name === undefined ? {} : { name }),
    ...(description === undefined ? {} : { description }),
    ...(startingLocation === undefined ? {} : { startingLocation }),
  };
}

export function parseItineraryFilters(value: unknown): ItineraryFilters {
  if (!isObject(value)) {
    throw new InvalidItineraryFiltersError(['Query parameters must be an object.']);
  }

  const details: string[] = [];
  const parseBoolean = (field: 'saved' | 'inRecommendation'): boolean | undefined => {
    const queryValue = value[field];
    if (queryValue === undefined) return undefined;
    if (queryValue === 'true') return true;
    if (queryValue === 'false') return false;
    details.push(`${field} must be either 'true' or 'false'.`);
    return undefined;
  };

  const saved = parseBoolean('saved');
  const inRecommendation = parseBoolean('inRecommendation');
  if (details.length > 0) {
    throw new InvalidItineraryFiltersError(details);
  }

  return {
    ...(saved === undefined ? {} : { saved }),
    ...(inRecommendation === undefined ? {} : { inRecommendation }),
  };
}
