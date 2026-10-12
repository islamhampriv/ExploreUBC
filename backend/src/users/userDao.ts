import {
  type Collection,
  type Db,
  type Document,
  ObjectId,
} from 'mongodb';

import type { UserRepository } from './userRepository';
import type {
  PreferencesUpdate,
  User,
  UserPreferences,
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

  async updatePreferences(
    userId: string,
    update: PreferencesUpdate,
  ): Promise<boolean> {
    const objectId = this.parseUserId(userId);
    if (objectId === null) {
      return false;
    }

    const fields: Document = { updated_at: new Date() };

    if (update.transportation !== undefined) {
      fields['preferences.transportation'] = update.transportation;
    }
    if (update.budget !== undefined) {
      fields['preferences.budget'] = update.budget;
    }
    if (update.interest !== undefined) {
      fields['preferences.interest'] = update.interest;
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
