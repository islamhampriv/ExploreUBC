import type {
  GoogleUserIdentity,
  OnboardingInput,
  User,
  UserPreferences,
  UserUpdate,
} from './userTypes';

export interface UserRepository {
  findOrCreateByGoogleIdentity(
    identity: GoogleUserIdentity,
  ): Promise<User>;
  findPreferences(userId: string): Promise<UserPreferences | null>;
  findUser(userId: string): Promise<User | null>;
  completeOnboarding(
    userId: string,
    onboarding: OnboardingInput,
    completedAt: Date,
  ): Promise<User | null>;
  updateUser(userId: string, update: UserUpdate): Promise<User | null>;
}
