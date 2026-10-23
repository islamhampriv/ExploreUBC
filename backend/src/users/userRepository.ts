import type {
  User,
  UserPreferences,
  UserUpdate,
} from './userTypes';

export interface UserRepository {
  findPreferences(userId: string): Promise<UserPreferences | null>;
  findUser(userId: string): Promise<User | null>;
  updateUser(userId: string, update: UserUpdate): Promise<boolean>;
}
