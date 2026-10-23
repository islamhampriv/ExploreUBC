import { InvalidUserUpdateError } from './userErrors';
import {
  BUDGET_PREFERENCES,
  INTEREST_PREFERENCES,
  TRANSPORTATION_MODES,
  type BudgetPreference,
  type InterestPreference,
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

export function parseUserUpdate(value: unknown): UserUpdate {
  if (!isObject(value)) {
    throw new InvalidUserUpdateError(['Request body must be an object.']);
  }

  const allowedFields = new Set([
    'displayName',
    'start_location',
    'preferences',
  ]);
  const details = Object.keys(value)
    .filter((field) => !allowedFields.has(field))
    .map((field) => `Unexpected field '${field}'.`);
  const update: UserUpdate = {};

  for (const [field, maxLength] of [
    ['displayName', 200],
    ['start_location', 500],
  ] as const) {
    if (field in value) {
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

  if ('preferences' in value) {
    if (!isObject(value.preferences)) {
      details.push('preferences must be an object.');
    } else {
      const preferences = parsePreferences(value.preferences, details);
      if (Object.keys(preferences).length > 0) {
        update.preferences = preferences;
      }
    }
  }

  if (Object.keys(update).length === 0 && details.length === 0) {
    details.push('At least one user field must be provided.');
  }

  if (details.length > 0) {
    throw new InvalidUserUpdateError(details);
  }

  return update;
}
