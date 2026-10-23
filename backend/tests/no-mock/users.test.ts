import request from 'supertest';

import { createApp } from '../../src/index';
import type { UserRepository } from '../../src/users/userRepository';
import {
  type User,
  type UserPreferences,
  type UserUpdate,
} from '../../src/users/userTypes';

const USER_ID = 'user-123';
const INITIAL_PREFERENCES: UserPreferences = {
  transportation: { mode: 'transit' },
  budget: 'balanced',
  interest: 'outdoors',
};

class TestUserRepository implements UserRepository {
  private displayName = 'Test User';
  private startLocation = 'UBC Bus Exchange';
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
      displayName: this.displayName,
      start_location: this.startLocation,
      preferences: this.preferences,
      create_at: new Date('2026-10-12T12:00:00.000Z'),
      updated_at: new Date('2026-10-12T12:00:00.000Z'),
    };
  }

  async updateUser(userId: string, update: UserUpdate): Promise<boolean> {
    if (userId !== USER_ID || this.preferences === null) {
      return false;
    }

    this.displayName = update.displayName ?? this.displayName;
    this.startLocation = update.start_location ?? this.startLocation;
    this.preferences = {
      transportation:
        update.preferences?.transportation ?? this.preferences.transportation,
      budget: update.preferences?.budget ?? this.preferences.budget,
      interest: update.preferences?.interest ?? this.preferences.interest,
    };
    return true;
  }
}

function createTestApp() {
  return createApp({
    userRepository: new TestUserRepository(),
  });
}

describe('Unmocked: GET /api/v1/users/:userId', () => {
  test('returns the complete user, including profile fields and preferences', async () => {
    const response = await request(createTestApp()).get(
      `/api/v1/users/${USER_ID}`,
    );

    expect(response.status).toBe(200);
    expect(response.body).toEqual({
      user: {
        googleSubject: USER_ID,
        email: 'test.user@student.ubc.ca',
        displayName: 'Test User',
        start_location: 'UBC Bus Exchange',
        preferences: INITIAL_PREFERENCES,
        create_at: '2026-10-12T12:00:00.000Z',
        updated_at: '2026-10-12T12:00:00.000Z',
      },
    });
  });

  test('returns 404 when the user does not exist', async () => {
    const response = await request(createTestApp()).get(
      '/api/v1/users/missing-user',
    );

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
  });
});

describe('Unmocked: PATCH /api/v1/users/:userId', () => {
  test('updates the display name and starting location', async () => {
    const app = createTestApp();
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send({
        displayName: 'Updated User',
        start_location: 'UBC Bookstore',
      });

    expect(response.status).toBe(200);
    expect(response.body.user).toMatchObject({
      displayName: 'Updated User',
      start_location: 'UBC Bookstore',
      preferences: INITIAL_PREFERENCES,
    });
  });

  test('supports a partial user update', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ start_location: 'Nitobe Memorial Garden' });

    expect(response.status).toBe(200);
    expect(response.body.user).toMatchObject({
      displayName: 'Test User',
      start_location: 'Nitobe Memorial Garden',
    });
  });

  test('updates nested preferences through the user endpoint', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ preferences: { budget: 'premium' } });

    expect(response.status).toBe(200);
    expect(response.body.user.preferences).toEqual({
      ...INITIAL_PREFERENCES,
      budget: 'premium',
    });
  });

  test('rejects empty and unexpected user updates', async () => {
    const app = createTestApp();
    const emptyResponse = await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send({});
    const unexpectedResponse = await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ email: 'changed@example.com' });

    expect(emptyResponse.status).toBe(400);
    expect(emptyResponse.body.error.code).toBe('INVALID_USER_UPDATE');
    expect(unexpectedResponse.status).toBe(400);
    expect(unexpectedResponse.body.error.details).toContain(
      "Unexpected field 'email'.",
    );
  });

  test('returns 404 when the user does not exist', async () => {
    const response = await request(createTestApp())
      .patch('/api/v1/users/missing-user')
      .send({ displayName: 'Missing User' });

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
  });
});
