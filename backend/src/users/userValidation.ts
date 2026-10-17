import { InvalidPreferencesUpdateError } from './userErrors';
import {
  BUDGET_PREFERENCES,
  INTEREST_PREFERENCES,
  TRANSPORTATION_MODES,
  type BudgetPreference,
  type InterestPreference,
  type PreferencesUpdate,
  type TransportationMode,
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

export function parsePreferencesUpdate(value: unknown): PreferencesUpdate {
  if (!isObject(value)) {
    throw new InvalidPreferencesUpdateError(['Request body must be an object.']);
  }

  const allowedFields = new Set(['transportation', 'budget', 'interest']);
  const unexpectedFields = Object.keys(value).filter(
    (field) => !allowedFields.has(field),
  );
  const details: string[] = unexpectedFields.map(
    (field) => `Unexpected field '${field}'.`,
  );

  const update: PreferencesUpdate = {};

  if ('transportation' in value) {
    const transportation = value.transportation;
    if (
      !isObject(transportation) ||
      Object.keys(transportation).length !== 1 ||
      !includesValue(TRANSPORTATION_MODES, transportation.mode)
    ) {
      details.push(
        `transportation must contain one valid mode: ${TRANSPORTATION_MODES.join(', ')}.`,
      );
    } else {
      update.transportation = {
        mode: transportation.mode as TransportationMode,
      };
    }
  }

  if ('budget' in value) {
    if (!includesValue(BUDGET_PREFERENCES, value.budget)) {
      details.push(`budget must be one of: ${BUDGET_PREFERENCES.join(', ')}.`);
    } else {
      update.budget = value.budget as BudgetPreference;
    }
  }

  if ('interest' in value) {
    if (!includesValue(INTEREST_PREFERENCES, value.interest)) {
      details.push(
        `interest must be one of: ${INTEREST_PREFERENCES.join(', ')}.`,
      );
    } else {
      update.interest = value.interest as InterestPreference;
    }
  }

  if (Object.keys(update).length === 0 && details.length === 0) {
    details.push('At least one preference must be provided.');
  }

  if (details.length > 0) {
    throw new InvalidPreferencesUpdateError(details);
  }

  return update;
}

