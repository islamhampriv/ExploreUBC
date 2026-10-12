import type { PreferencesUpdate, UserPreferences, User } from './userTypes';

export interface UserRepository {
  findPreferences(userId: string): Promise<UserPreferences | null>;
  updatePreferences(
    userId: string,
    update: PreferencesUpdate,
  ): Promise<boolean>;
  findUser(userId: string): Promise<User | null>;
}
