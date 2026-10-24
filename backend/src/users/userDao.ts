import {
  type Collection,
  type Db,
  type Document,
  MongoServerError,
  ObjectId,
} from 'mongodb';

import type { UserRepository } from './userRepository';
import type {
  GoogleUserIdentity,
  OnboardingInput,
  User,
  UserPreferences,
  UserUpdate,
} from './userTypes';

type UserDocument = Omit<User, 'id'> & {
  _id: ObjectId;
};

const NEW_USER_PREFERENCES: UserPreferences = {
  transportation: { mode: 'walking' },
  budget: 'economical',
  interest: 'food',
};

export class UserDao implements UserRepository {
  private readonly users: Collection<UserDocument>;

  constructor(database: Db) {
    this.users = database.collection<UserDocument>('users');
  }

  async findOrCreateByGoogleIdentity(
    identity: GoogleUserIdentity,
  ): Promise<User> {
    const now = new Date();

    try {
      const result = await this.users.findOneAndUpdate(
        { googleSubject: identity.googleSubject },
        {
          $setOnInsert: {
            _id: new ObjectId(),
            googleSubject: identity.googleSubject,
            email: identity.email,
            displayName: identity.displayName,
            profileImageUrl: identity.profileImageUrl,
            start_location: '',
            preferences: NEW_USER_PREFERENCES,
            onboardingCompletedAt: null,
            create_at: now,
            updated_at: now,
          },
        },
        {
          upsert: true,
          returnDocument: 'after',
          includeResultMetadata: true,
        },
      );

      if (result.value === null) {
        throw new Error('MongoDB did not return the found or created user.');
      }

      return this.toUser(result.value);
    } catch (error: unknown) {
      if (!(error instanceof MongoServerError) || error.code !== 11000) {
        throw error;
      }

      const existingUser = await this.users.findOne({
        googleSubject: identity.googleSubject,
      });
      if (existingUser === null) {
        throw error;
      }

      return this.toUser(existingUser);
    }
  }

  async findPreferences(userId: string): Promise<UserPreferences | null> {
    const user = await this.findUserDocument(userId, { preferences: 1 });

    return user?.preferences ?? null;
  }

  async findUser(userId: string): Promise<User | null> {
    const user = await this.findUserDocument(userId);

    return user === null ? null : this.toUser(user);
  }

  async completeOnboarding(
    userId: string,
    onboarding: OnboardingInput,
    completedAt: Date,
  ): Promise<User | null> {
    return this.saveUserUpdate(userId, onboarding, completedAt);
  }

  async updateUser(userId: string, update: UserUpdate): Promise<User | null> {
    return this.saveUserUpdate(userId, update);
  }

  private async saveUserUpdate(
    userId: string,
    update: UserUpdate,
    onboardingCompletedAt?: Date,
  ): Promise<User | null> {
    const objectId = this.parseUserId(userId);
    if (objectId === null) {
      return null;
    }

    const fields: Document = {
      updated_at: onboardingCompletedAt ?? new Date(),
    };
    if (onboardingCompletedAt !== undefined) {
      fields.onboardingCompletedAt = onboardingCompletedAt;
    }
    if (update.displayName !== undefined) {
      fields.displayName = update.displayName;
    }
    if (update.start_location !== undefined) {
      fields.start_location = update.start_location;
    }
    if (update.preferences?.transportation !== undefined) {
      fields['preferences.transportation'] = update.preferences.transportation;
    }
    if (update.preferences?.budget !== undefined) {
      fields['preferences.budget'] = update.preferences.budget;
    }
    if (update.preferences?.interest !== undefined) {
      fields['preferences.interest'] = update.preferences.interest;
    }

    const user = await this.users.findOneAndUpdate(
      { _id: objectId },
      { $set: fields },
      { returnDocument: 'after' },
    );
    return user === null ? null : this.toUser(user);
  }

  private parseUserId(userId: string): ObjectId | null {
    return ObjectId.isValid(userId) ? new ObjectId(userId) : null;
  }

  private async findUserDocument(
    userId: string,
    projection?: Document,
  ): Promise<UserDocument | null> {
    const objectId = this.parseUserId(userId);
    if (objectId === null) {
      return null;
    }

    return this.users.findOne(
      { _id: objectId },
      projection === undefined ? {} : { projection },
    );
  }

  private toUser(document: UserDocument): User {
    return {
      id: document._id.toHexString(),
      googleSubject: document.googleSubject,
      email: document.email,
      displayName: document.displayName,
      profileImageUrl: document.profileImageUrl,
      start_location: document.start_location,
      preferences: document.preferences,
      onboardingCompletedAt: document.onboardingCompletedAt,
      create_at: document.create_at,
      updated_at: document.updated_at,
    };
  }
}
