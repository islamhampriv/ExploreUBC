export const TRANSPORTATION_MODES = [
  'walking',
  'cycling',
  'transit',
  'driving',
  'other',
] as const;

export const BUDGET_PREFERENCES = [
  'economical',
  'balanced',
  'premium',
] as const;

export const INTEREST_PREFERENCES = [
  'food',
  'art',
  'outdoors',
  'markets',
  'live_music',
  'gaming',
] as const;

export type TransportationMode = (typeof TRANSPORTATION_MODES)[number];
export type BudgetPreference = (typeof BUDGET_PREFERENCES)[number];
export type InterestPreference = (typeof INTEREST_PREFERENCES)[number];

export type UserPreferences = {
  transportation: {
    mode: TransportationMode;
  };
  budget: BudgetPreference;
  interest: InterestPreference;
};

export type User = {
  googleSubject: string;
  email: string;
  displayName: string;
  start_location: string;
  preferences: UserPreferences;
  create_at: Date;
  updated_at: Date;
};

export type PreferencesUpdate = {
  transportation?: {
    mode: TransportationMode;
  };
  budget?: BudgetPreference;
  interest?: InterestPreference;
};
