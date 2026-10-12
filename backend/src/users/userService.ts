import { UserNotFoundError } from './userErrors';
import type { UserRepository } from './userRepository';
import type { PreferencesUpdate, UserPreferences } from './userTypes';

export class UserService {
  constructor(private readonly repository: UserRepository) {}

  async getPreferences(userId: string): Promise<UserPreferences> {
    const preferences = await this.repository.findPreferences(userId);
    if (preferences === null) {
      throw new UserNotFoundError(userId);
    }

    return preferences;
  }

  async updatePreferences(
    userId: string,
    update: PreferencesUpdate,
  ): Promise<UserPreferences> {
    const updated = await this.repository.updatePreferences(userId, update);
    if (!updated) {
      throw new UserNotFoundError(userId);
    }

    return this.getPreferences(userId);
  }
}
