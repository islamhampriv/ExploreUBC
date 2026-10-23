import {
  type Collection,
  type Db,
  type Document,
  ObjectId,
} from 'mongodb';

import type { UserRepository } from './userRepository';
import type {
  User,
  UserPreferences,
  UserUpdate,
} from './userTypes';

type UserDocument = User & {
  _id: ObjectId;
};

export class UserDao implements UserRepository {
  private readonly users: Collection<UserDocument>;

  constructor(database: Db) {
    this.users = database.collection<UserDocument>('users');
  }

  async findPreferences(userId: string): Promise<UserPreferences | null> {
    const objectId = this.parseUserId(userId);
    if (objectId === null) {
      return null;
    }

    const user = await this.users.findOne(
      { _id: objectId },
      { projection: { preferences: 1 } },
    );

    return user?.preferences ?? null;
  }

  async findUser(userId: string): Promise<User | null> {
    const objectId = this.parseUserId(userId);
    if (objectId === null) {
      return null;
    }

    const user = await this.users.findOne(
      { _id: objectId },
      { projection: { _id: 0 } },
    );

    return user ?? null;
  }

  async updateUser(userId: string, update: UserUpdate): Promise<boolean> {
    const objectId = this.parseUserId(userId);
    if (objectId === null) {
      return false;
    }

    const fields: Document = { updated_at: new Date() };
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

    const result = await this.users.updateOne(
      { _id: objectId },
      { $set: fields },
    );
    return result.matchedCount === 1;
  }

  private parseUserId(userId: string): ObjectId | null {
    return ObjectId.isValid(userId) ? new ObjectId(userId) : null;
  }
}
