import type { PreferencesUpdate, UserPreferences } from './userTypes';

export interface UserRepository {
  findPreferences(userId: string): Promise<UserPreferences | null>;
  updatePreferences(
    userId: string,
    update: PreferencesUpdate,
  ): Promise<boolean>;
}
