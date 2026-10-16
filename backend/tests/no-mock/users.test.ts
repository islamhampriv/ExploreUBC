import request from 'supertest';

import { createApp } from '../../src/index';
import type { UserRepository } from '../../src/users/userRepository';
import {
  INTEREST_PREFERENCES,
  type PreferencesUpdate,
  type User,
  type UserPreferences,
} from '../../src/users/userTypes';

const USER_ID = 'user-123';
const INITIAL_PREFERENCES: UserPreferences = {
  transportation: { mode: 'transit' },
  budget: 'balanced',
  interest: 'outdoors',
};

class TestUserRepository implements UserRepository {
  private preferences: UserPreferences | null = {
    ...INITIAL_PREFERENCES,
    transportation: { ...INITIAL_PREFERENCES.transportation },
  };

  async findPreferences(userId: string): Promise<UserPreferences | null> {
    return userId === USER_ID ? this.preferences : null;
  }

  async findUser(userId: string): Promise<User | null> {
    if (userId !== USER_ID || this.preferences === null) {
      return null;
    }

    return {
      googleSubject: USER_ID,
      email: 'test.user@student.ubc.ca',
      displayName: 'Test User',
      start_location: 'UBC Bus Exchange',
      preferences: this.preferences,
      create_at: new Date('2026-10-12T12:00:00.000Z'),
      updated_at: new Date('2026-10-12T12:00:00.000Z'),
    };
  }

  async updatePreferences(
    userId: string,
    update: PreferencesUpdate,
  ): Promise<boolean> {
    if (userId !== USER_ID || this.preferences === null) {
      return false;
    }

    this.preferences = {
      transportation:
        update.transportation ?? this.preferences.transportation,
      budget: update.budget ?? this.preferences.budget,
      interest: update.interest ?? this.preferences.interest,
    };
    return true;
  }
}

function createTestApp() {
  return createApp({
    userRepository: new TestUserRepository(),
  });
}

describe('Unmocked: GET /api/v1/users/:userId/preferences', () => {
  test('returns the saved user preferences', async () => {
    const response = await request(createTestApp()).get(
      `/api/v1/users/${USER_ID}/preferences`,
    );

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ preferences: INITIAL_PREFERENCES });
  });

  test('returns 404 when the user does not exist', async () => {
    const response = await request(createTestApp()).get(
      '/api/v1/users/missing-user/preferences',
    );

    expect(response.status).toBe(404);
    expect(response.body).toEqual({
      error: {
        code: 'USER_NOT_FOUND',
        message: "User 'missing-user' was not found.",
      },
    });
  });
});

describe('Unmocked: PATCH /api/v1/users/:userId/preferences', () => {
  test.each(INTEREST_PREFERENCES)(
    'updates the interest preference to %s',
    async (interest) => {
      const app = createTestApp();

      const response = await request(app)
        .patch(`/api/v1/users/${USER_ID}/preferences`)
        .send({ interest });

      expect(response.status).toBe(200);
      expect(response.body).toEqual({
        preferences: {
          ...INITIAL_PREFERENCES,
          interest,
        },
      });
    },
  );

  test('preserves the interest when another preference changes', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}/preferences`)
      .send({ transportation: { mode: 'cycling' }, budget: 'economical' });

    expect(response.status).toBe(200);
    expect(response.body).toEqual({
      preferences: {
        transportation: { mode: 'cycling' },
        budget: 'economical',
        interest: INITIAL_PREFERENCES.interest,
      },
    });
  });

  test('rejects an unsupported interest', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}/preferences`)
      .send({ interest: 'sports' });

    expect(response.status).toBe(400);
    expect(response.body.error).toMatchObject({
      code: 'INVALID_PREFERENCES_UPDATE',
      message: 'The preference update is invalid.',
    });
    expect(response.body.error.details).toContain(
      'interest must be one of: food, art, outdoors, markets, live_music, gaming.',
    );
  });

  test('rejects an empty update', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}/preferences`)
      .send({});

    expect(response.status).toBe(400);
    expect(response.body.error.details).toContain(
      'At least one preference must be provided.',
    );
  });

  test('rejects unexpected fields', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}/preferences`)
      .send({ interests: ['food'] });

    expect(response.status).toBe(400);
    expect(response.body.error.details).toContain(
      "Unexpected field 'interests'.",
    );
  });

  test('does not change stored preferences after a rejected update', async () => {
    const app = createTestApp();

    await request(app)
      .patch(`/api/v1/users/${USER_ID}/preferences`)
      .send({ interest: 'sports' })
      .expect(400);

    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/preferences`,
    );
    expect(response.body).toEqual({ preferences: INITIAL_PREFERENCES });
  });

  test('returns 404 when the user does not exist', async () => {
    const response = await request(createTestApp())
      .patch('/api/v1/users/missing-user/preferences')
      .send({ interest: 'food' });

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
  });
});
