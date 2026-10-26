import supertest from 'supertest';

import { InvalidSessionError } from '../../../src/session/sessionError';
import type { Session } from '../../../src/session/sessionTypes';
import { createApp } from '../../../src/index';
import type { UserRepository } from '../../../src/users/userRepository';
import {
  INTEREST_PREFERENCES,
  type GoogleUserIdentity,
  type OnboardingInput,
  type User,
  type UserPreferences,
  type UserUpdate,
} from '../../../src/users/userTypes';

const USER_ID = 'user-123';
const SESSION_TOKEN = 'valid-test-session';
const INITIAL_PREFERENCES: UserPreferences = {
  transportation: { mode: 'transit' },
  budget: 'balanced',
  interest: 'outdoors',
};

class TestUserRepository implements UserRepository {
  private displayName = 'Test User';
  private startLocation = 'UBC Bus Exchange';
  private onboardingCompletedAt: Date | null = null;
  private preferences: UserPreferences | null = {
    ...INITIAL_PREFERENCES,
    transportation: { ...INITIAL_PREFERENCES.transportation },
  };

  async findOrCreateByGoogleIdentity(
    identity: GoogleUserIdentity,
  ): Promise<User> {
    return {
      id: USER_ID,
      googleSubject: identity.googleSubject,
      email: identity.email,
      displayName: identity.displayName,
      profileImageUrl: identity.profileImageUrl,
      start_location: this.startLocation,
      preferences: this.preferences ?? INITIAL_PREFERENCES,
      onboardingCompletedAt: this.onboardingCompletedAt,
      create_at: new Date('2026-10-12T12:00:00.000Z'),
      updated_at: new Date('2026-10-12T12:00:00.000Z'),
    };
  }

  async findPreferences(userId: string): Promise<UserPreferences | null> {
    return userId === USER_ID ? this.preferences : null;
  }

  async findUser(userId: string): Promise<User | null> {
    if (userId !== USER_ID || this.preferences === null) {
      return null;
    }

    return {
      id: USER_ID,
      googleSubject: USER_ID,
      email: 'test.user@student.ubc.ca',
      displayName: this.displayName,
      profileImageUrl: null,
      start_location: this.startLocation,
      preferences: this.preferences,
      onboardingCompletedAt: this.onboardingCompletedAt,
      create_at: new Date('2026-10-12T12:00:00.000Z'),
      updated_at: new Date('2026-10-12T12:00:00.000Z'),
    };
  }

  async updateUser(userId: string, update: UserUpdate): Promise<User | null> {
    if (userId !== USER_ID || this.preferences === null) {
      return null;
    }

    this.displayName = update.displayName ?? this.displayName;
    this.startLocation = update.start_location ?? this.startLocation;
    this.preferences = {
      transportation:
        update.preferences?.transportation ?? this.preferences.transportation,
      budget: update.preferences?.budget ?? this.preferences.budget,
      interest: update.preferences?.interest ?? this.preferences.interest,
    };
    return this.findUser(userId);
  }

  async completeOnboarding(
    userId: string,
    onboarding: OnboardingInput,
    completedAt: Date,
  ): Promise<User | null> {
    if (userId !== USER_ID || this.preferences === null) {
      return null;
    }

    this.startLocation = onboarding.start_location;
    this.preferences = onboarding.preferences;
    this.onboardingCompletedAt = completedAt;
    return this.findUser(userId);
  }
}

const sessionService = {
  async validateSession(token: string): Promise<Session> {
    if (token !== SESSION_TOKEN) {
      throw new InvalidSessionError();
    }

    return {
      id: 'session-123',
      userId: USER_ID,
      tokenHash: 'test-hash',
      createdAt: new Date('2026-10-12T12:00:00.000Z'),
      expiresAt: new Date('2026-11-11T12:00:00.000Z'),
    };
  },
};

function request(app: Parameters<typeof supertest>[0]) {
  return supertest
    .agent(app)
    .set('Authorization', `Bearer ${SESSION_TOKEN}`);
}

function createTestApp() {
  return createApp({
    sessionService,
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
        id: USER_ID,
        googleSubject: USER_ID,
        email: 'test.user@student.ubc.ca',
        displayName: 'Test User',
        profileImageUrl: null,
        start_location: 'UBC Bus Exchange',
        preferences: INITIAL_PREFERENCES,
        onboardingCompletedAt: null,
        create_at: '2026-10-12T12:00:00.000Z',
        updated_at: '2026-10-12T12:00:00.000Z',
      },
    });
  });

  test('returns 403 when requesting a different user', async () => {
    const response = await request(createTestApp()).get(
      '/api/v1/users/missing-user',
    );

    expect(response.status).toBe(403);
    expect(response.body.error.code).toBe('FORBIDDEN');
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

  test('returns 403 when updating a different user', async () => {
    const response = await request(createTestApp())
      .patch('/api/v1/users/missing-user')
      .send({ displayName: 'Missing User' });

    expect(response.status).toBe(403);
    expect(response.body.error.code).toBe('FORBIDDEN');
  });
});

describe('Unmocked: PATCH /api/v1/users/:userId with preferences', () => {
  test.each(INTEREST_PREFERENCES)(
    'updates the interest preference to %s',
    async (interest) => {
      const app = createTestApp();

      const response = await request(app)
        .patch(`/api/v1/users/${USER_ID}`)
        .send({ preferences: { interest } });

      expect(response.status).toBe(200);
      expect(response.body).toMatchObject({
        user: {
          preferences: {
            ...INITIAL_PREFERENCES,
            interest,
          },
        },
      });
    },
  );

  test('preserves the interest when another preference changes', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}`)
      .send({
        preferences: {
          transportation: { mode: 'cycling' },
          budget: 'economical',
        },
      });

    expect(response.status).toBe(200);
    expect(response.body).toMatchObject({
      user: {
        preferences: {
          transportation: { mode: 'cycling' },
          budget: 'economical',
          interest: INITIAL_PREFERENCES.interest,
        },
      },
    });
  });

  test('rejects an unsupported interest', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ preferences: { interest: 'sports' } });

    expect(response.status).toBe(400);
    expect(response.body.error).toMatchObject({
      code: 'INVALID_USER_UPDATE',
      message: 'The user update is invalid.',
    });
    expect(response.body.error.details).toContain(
      'preferences.interest must be one of: food, art, outdoors, markets, live_music, gaming.',
    );
  });

  test('rejects an empty update', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ preferences: {} });

    expect(response.status).toBe(400);
    expect(response.body.error.details).toContain(
      'preferences must contain at least one field.',
    );
  });

  test('rejects unexpected fields', async () => {
    const response = await request(createTestApp())
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ preferences: { interests: ['food'] } });

    expect(response.status).toBe(400);
    expect(response.body.error.details).toContain(
      "Unexpected field 'preferences.interests'.",
    );
  });

  test('does not change stored preferences after a rejected update', async () => {
    const app = createTestApp();

    await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ preferences: { interest: 'sports' } })
      .expect(400);

    const response = await request(app).get(`/api/v1/users/${USER_ID}`);
    expect(response.body.user.preferences).toEqual(INITIAL_PREFERENCES);
  });

  test('returns 403 when updating another user\'s preferences', async () => {
    const response = await request(createTestApp())
      .patch('/api/v1/users/missing-user')
      .send({ preferences: { interest: 'food' } });

    expect(response.status).toBe(403);
    expect(response.body.error.code).toBe('FORBIDDEN');
  });
});
