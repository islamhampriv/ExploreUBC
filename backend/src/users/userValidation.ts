import { InvalidOnboardingError, InvalidUserUpdateError } from './userErrors';
import {
  BUDGET_PREFERENCES,
  INTEREST_PREFERENCES,
  TRANSPORTATION_MODES,
  type BudgetPreference,
  type InterestPreference,
  type OnboardingInput,
  type TransportationMode,
  type UserUpdate,
} from './userTypes';

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function includesValue<T extends string>(
  allowedValues: readonly T[],
  value: unknown,
): value is T {
  return typeof value === 'string' && allowedValues.includes(value as T);
}

function parsePreferences(
  value: Record<string, unknown>,
  details: string[],
): NonNullable<UserUpdate['preferences']> {
  const allowedFields = new Set(['transportation', 'budget', 'interest']);
  const unexpectedFields = Object.keys(value).filter(
    (field) => !allowedFields.has(field),
  );
  details.push(
    ...unexpectedFields.map(
      (field) => `Unexpected field 'preferences.${field}'.`,
    ),
  );

  const update: NonNullable<UserUpdate['preferences']> = {};

  if ('transportation' in value) {
    const transportation = value.transportation;
    if (
      !isObject(transportation) ||
      Object.keys(transportation).length !== 1 ||
      !includesValue(TRANSPORTATION_MODES, transportation.mode)
    ) {
      details.push(
        `preferences.transportation must contain one valid mode: ${TRANSPORTATION_MODES.join(', ')}.`,
      );
    } else {
      update.transportation = {
        mode: transportation.mode as TransportationMode,
      };
    }
  }

  if ('budget' in value) {
    if (!includesValue(BUDGET_PREFERENCES, value.budget)) {
      details.push(
        `preferences.budget must be one of: ${BUDGET_PREFERENCES.join(', ')}.`,
      );
    } else {
      update.budget = value.budget as BudgetPreference;
    }
  }

  if ('interest' in value) {
    if (!includesValue(INTEREST_PREFERENCES, value.interest)) {
      details.push(
        `preferences.interest must be one of: ${INTEREST_PREFERENCES.join(', ')}.`,
      );
    } else {
      update.interest = value.interest as InterestPreference;
    }
  }

  if (Object.keys(value).length === 0) {
    details.push('preferences must contain at least one field.');
  }

  return update;
}

const USER_UPDATE_FIELDS = new Set([
  'displayName',
  'start_location',
  'preferences',
]);
const ONBOARDING_FIELDS = new Set(['start_location', 'preferences']);

function parseUserFields(
  value: Record<string, unknown>,
  allowedFields: ReadonlySet<string>,
): { update: UserUpdate; details: string[] } {
  const details = Object.keys(value)
    .filter((field) => !allowedFields.has(field))
    .map((field) => `Unexpected field '${field}'.`);
  const update: UserUpdate = {};

  for (const [field, maxLength] of [
    ['displayName', 200],
    ['start_location', 500],
  ] as const) {
    if (allowedFields.has(field) && field in value) {
      const fieldValue = value[field];
      if (
        typeof fieldValue !== 'string' ||
        fieldValue.trim().length === 0 ||
        fieldValue.trim().length > maxLength
      ) {
        details.push(
          `${field} must contain between 1 and ${maxLength} characters.`,
        );
      } else {
        update[field] = fieldValue.trim();
      }
    }
  }

  if (allowedFields.has('preferences') && 'preferences' in value) {
    if (!isObject(value.preferences)) {
      details.push('preferences must be an object.');
    } else {
      const preferences = parsePreferences(value.preferences, details);
      if (Object.keys(preferences).length > 0) {
        update.preferences = preferences;
      }
    }
  }

  return { update, details };
}

export function parseUserUpdate(value: unknown): UserUpdate {
  if (!isObject(value)) {
    throw new InvalidUserUpdateError(['Request body must be an object.']);
  }

  const { update, details } = parseUserFields(value, USER_UPDATE_FIELDS);

  if (Object.keys(update).length === 0 && details.length === 0) {
    details.push('At least one user field must be provided.');
  }

  if (details.length > 0) {
    throw new InvalidUserUpdateError(details);
  }

  return update;
}

export function parseOnboarding(value: unknown): OnboardingInput {
  if (!isObject(value)) {
    throw new InvalidOnboardingError(['Request body must be an object.']);
  }

  const { update, details } = parseUserFields(value, ONBOARDING_FIELDS);

  if (!('start_location' in value)) {
    details.push('start_location is required.');
  }

  if (!('preferences' in value)) {
    details.push('preferences is required.');
  } else if (isObject(value.preferences)) {
    if (!('transportation' in value.preferences)) {
      details.push('preferences.transportation is required.');
    }
    if (!('budget' in value.preferences)) {
      details.push('preferences.budget is required.');
    }
    if (!('interest' in value.preferences)) {
      details.push('preferences.interest is required.');
    }
  }

  const transportation = update.preferences?.transportation;
  const budget = update.preferences?.budget;
  const interest = update.preferences?.interest;
  if (
    details.length > 0 ||
    update.start_location === undefined ||
    transportation === undefined ||
    budget === undefined ||
    interest === undefined
  ) {
    throw new InvalidOnboardingError(details);
  }

  return {
    start_location: update.start_location,
    preferences: { transportation, budget, interest },
  };
}
