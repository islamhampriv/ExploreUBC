import { UserNotFoundError } from './userErrors';
import type { UserRepository } from './userRepository';
import type {
  User,
  OnboardingInput,
  UserUpdate,
} from './userTypes';

export class UserService {
  constructor(
    private readonly repository: UserRepository,
    private readonly clock: () => Date = () => new Date(),
  ) {}

  async getUser(userId: string): Promise<User> {
    const user = await this.repository.findUser(userId);
    if (user === null) {
      throw new UserNotFoundError(userId);
    }

    return user;
  }

  async updateUser(userId: string, update: UserUpdate): Promise<User> {
    const user = await this.repository.updateUser(userId, update);
    if (user === null) {
      throw new UserNotFoundError(userId);
    }

    return user;
  }

  async completeOnboarding(
    userId: string,
    onboarding: OnboardingInput,
  ): Promise<User> {
    const user = await this.repository.completeOnboarding(
      userId,
      onboarding,
      this.clock(),
    );
    if (user === null) {
      throw new UserNotFoundError(userId);
    }

    return user;
  }
}
