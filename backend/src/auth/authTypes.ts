import type { User } from '../users/userTypes';

export type GoogleAuthResult = {
  sessionToken: string;
  expiresAt: Date;
  onboardingRequired: boolean;
  user: User;
};

export type CurrentSessionResult = {
  expiresAt: Date;
  onboardingRequired: boolean;
  user: User;
};
