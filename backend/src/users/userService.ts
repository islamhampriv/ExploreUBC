import { UserNotFoundError } from './userErrors';
import type { UserRepository } from './userRepository';
import type { User, UserUpdate } from './userTypes';

export class UserService {
  constructor(private readonly repository: UserRepository) {}

  async getUser(userId: string): Promise<User> {
    const user = await this.repository.findUser(userId);
    if (user === null) {
      throw new UserNotFoundError(userId);
    }

    return user;
  }

  async updateUser(userId: string, update: UserUpdate): Promise<User> {
    const updated = await this.repository.updateUser(userId, update);
    if (!updated) {
      throw new UserNotFoundError(userId);
    }

    return this.getUser(userId);
  }
}
