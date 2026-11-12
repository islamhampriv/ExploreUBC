import 'dotenv/config';

import * as os from 'node:os';

import type { Express } from 'express';
import { Db, MongoClient, ObjectId } from 'mongodb';
import request from 'supertest';

import { env } from '../../../src/config/env';
import { createApp } from '../../../src/index';
import { RecommendationDao } from '../../../src/recommendation/recommendationDao';
import {
  InvalidRecommendationOwnerError,
  InvalidRecommendationResponseError,
  RecommendationGenerationError,
} from '../../../src/recommendation/recommendationErrors';
import { parseItineraries } from '../../../src/recommendation/recommendationParser';
import { RecommendationService } from '../../../src/recommendation/recommendationService';
import { SoclaasClient } from '../../../src/recommendation/soclaasClient';
import {
  cleanTextForRanking,
  UniversalSentenceEncoderItineraryRanker,
} from '../../../src/recommendation/universalSentenceEncoderRanker';
import { UserDao } from '../../../src/users/userDao';

// SoCLaaS can take a full minute and may occasionally return unusable generated
// output. Leave enough time for one retry plus ranking and MongoDB work.
jest.setTimeout(210_000);

const USER_ID = new ObjectId();
const MISSING_USER_ID = new ObjectId();
const DATABASE_NAME = `eubc_r_${new ObjectId().toHexString()}`;

let client: MongoClient;
let database: Db;
let app: Express;
let recommendationService: RecommendationService;

function validCandidate(index = 1) {
  return {
    name: `Candidate ${index}`,
    description: `Art and museums ${index}`,
    items: [
      {
        type: 'activity',
        name: `Gallery ${index}`,
        image: '',
        description: 'View local art.',
        category: 'art',
        location: 'UBC Art Gallery',
        startTime: `2027-11-0${index}T10:00:00.000Z`,
        endTime: `2027-11-0${index}T11:00:00.000Z`,
      },
      {
        type: 'transportation',
        mode: 'transit',
        startLocation: 'UBC Bus Exchange',
        endLocation: 'UBC Art Gallery',
        distanceMeters: 800,
        startTime: `2027-11-0${index}T09:30:00.000Z`,
        endTime: `2027-11-0${index}T09:50:00.000Z`,
      },
    ],
  };
}

function validPayload() {
  return { itineraries: [1, 2, 3, 4].map(validCandidate) };
}

function storedRecommendation(
  name: string,
  interestSimilarity: number,
  options: { saved?: boolean; inRecommendation?: boolean } = {},
) {
  return {
    _id: new ObjectId(),
    ownerId: USER_ID,
    name,
    description: `${name} description`,
    startingLocation: 'UBC Bus Exchange',
    items: [],
    createdAt: new Date('2026-11-01T12:00:00.000Z'),
    updatedAt: new Date('2026-11-01T12:00:00.000Z'),
    saved: options.saved ?? false,
    inRecommendation: options.inRecommendation ?? true,
    score: { interestSimilarity },
  };
}

beforeAll(async () => {
  const uri = process.env.MONGODB_URI;
  if (uri === undefined || uri.trim() === '') {
    throw new Error('MONGODB_URI is required for no-mock tests.');
  }

  client = new MongoClient(uri, {
    runtimeAdapters: { os },
    serverSelectionTimeoutMS: 10_000,
  });
  database = client.db(DATABASE_NAME);
  const repository = new RecommendationDao(database);
  const userRepository = new UserDao(database);
  const generator = new SoclaasClient(env.soclaas);
  const ranker = new UniversalSentenceEncoderItineraryRanker();
  recommendationService = new RecommendationService(
    repository,
    userRepository,
    generator,
    ranker,
  );
  app = createApp({
    recommendationGenerator: generator,
    recommendationRanker: ranker,
    recommendationRepository: repository,
    userRepository,
  });
  await client.connect();
});

beforeEach(async () => {
  await database.collection('users').deleteMany({});
  await database.collection('itineraries').deleteMany({});
  await database.collection('users').insertOne({
    _id: USER_ID,
    googleSubject: 'recommendation-test-subject',
    email: 'recommendation.test@student.ubc.ca',
    displayName: 'Recommendation Test User',
    start_location: 'UBC Bus Exchange',
    preferences: {
      transportation: { mode: 'transit' },
      budget: 'balanced',
      interest: 'art',
    },
    create_at: new Date('2026-11-01T00:00:00.000Z'),
    updated_at: new Date('2026-11-01T00:00:00.000Z'),
  });
});

afterAll(async () => {
  await database.dropDatabase();
  await client.close();
});

describe('GET /api/v1/users/:userId/recommendations — no mocks', () => {
  test('returns only the latest four active recommendations in score order', async () => {
    await database.collection('itineraries').insertMany([
      storedRecommendation('Medium', 0.5),
      storedRecommendation('Highest', 0.95),
      storedRecommendation('High', 0.8),
      { ...storedRecommendation('No score', 0), score: {} },
      storedRecommendation('Saved', 1, { saved: true }),
      storedRecommendation('Inactive', 1, { inRecommendation: false }),
    ]);

    const response = await request(app)
      .get(`/api/v1/users/${USER_ID}/recommendations`);

    expect(response.status).toBe(200);
    expect(response.body.itineraries).toHaveLength(4);
    expect(response.body.itineraries.map((itinerary: { name: string }) =>
      itinerary.name,
    )).toEqual(['Highest', 'High', 'Medium', 'No score']);
    expect(response.body.itineraries).toEqual(expect.arrayContaining([
      expect.objectContaining({
        ownerId: USER_ID.toHexString(),
        saved: false,
        inRecommendation: true,
      }),
    ]));
  });

  test('returns an empty list for a malformed user ID', async () => {
    const response = await request(app)
      .get('/api/v1/users/not-an-object-id/recommendations');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ itineraries: [] });
  });

  test('maps documents without optional fields and rejects an invalid owner', async () => {
    const repository = new RecommendationDao(database);

    await expect(repository.replaceLatestRecommendations('invalid-owner', []))
      .rejects.toBeInstanceOf(InvalidRecommendationOwnerError);

    const recommendations = await repository.replaceLatestRecommendations(
      USER_ID.toHexString(),
      [{ name: 'Minimal', items: [], score: { interestSimilarity: 0.4 } }],
    );

    expect(recommendations).toHaveLength(1);
    expect(recommendations[0]).not.toHaveProperty('description');
    expect(recommendations[0]).not.toHaveProperty('startingLocation');
  });

  test('returns an internal error when the real database connection is closed', async () => {
    const closedClient = new MongoClient(env.mongodbUri, {
      runtimeAdapters: { os },
      serverSelectionTimeoutMS: 10_000,
    });
    await closedClient.connect();
    const closedDatabase = closedClient.db(`eubc_closed_${new ObjectId().toHexString()}`);
    await closedClient.close();
    const disconnectedApp = createApp({
      recommendationGenerator: new SoclaasClient(env.soclaas),
      recommendationRanker: new UniversalSentenceEncoderItineraryRanker(),
      recommendationRepository: new RecommendationDao(closedDatabase),
      userRepository: new UserDao(closedDatabase),
    });

    const response = await request(disconnectedApp)
      .get(`/api/v1/users/${USER_ID}/recommendations`);

    expect(response.status).toBe(500);
    expect(response.body.error.code).toBe('INTERNAL_SERVER_ERROR');
  });
});

describe('POST /api/v1/users/:userId/recommendations — no mocks', () => {
  test('generates, ranks, persists, and returns four recommendations', async () => {
    let response = await request(app)
      .post(`/api/v1/users/${USER_ID}/recommendations`)
      .send({});

    if (
      response.status === 502 &&
      response.body.error?.code === 'RECOMMENDATION_GENERATION_FAILED'
    ) {
      response = await request(app)
        .post(`/api/v1/users/${USER_ID}/recommendations`)
        .send({});
    }

    expect(response.status).toBe(201);
    expect(response.body.itineraries).toHaveLength(4);
    expect(response.body.itineraries).toEqual(expect.arrayContaining([
      expect.objectContaining({
        ownerId: USER_ID.toHexString(),
        startingLocation: 'UBC Bus Exchange',
        saved: false,
        inRecommendation: true,
        score: expect.objectContaining({
          interestSimilarity: expect.any(Number),
          interestSimilarityOutOfFive: expect.any(Number),
        }),
      }),
    ]));

    const scores = response.body.itineraries.map(
      (itinerary: { score: { interestSimilarity: number } }) =>
        itinerary.score.interestSimilarity,
    );
    expect(scores).toEqual([...scores].sort((left, right) => right - left));
    await expect(database.collection('itineraries').countDocuments({
      ownerId: USER_ID,
      saved: false,
      inRecommendation: true,
    })).resolves.toBe(4);

    const fetched = await request(app)
      .get(`/api/v1/users/${USER_ID}/recommendations`);
    expect(fetched.status).toBe(200);
    expect(fetched.body.itineraries).toEqual(response.body.itineraries);
  });

  test('returns USER_NOT_FOUND without calling the LLM for an absent user', async () => {
    const response = await request(app)
      .post(`/api/v1/users/${MISSING_USER_ID}/recommendations`)
      .send({});

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
    await expect(database.collection('itineraries').countDocuments({}))
      .resolves.toBe(0);
  });

  test('returns INCOMPLETE_PROFILE before calling the LLM', async () => {
    await database.collection('users').updateOne(
      { _id: USER_ID },
      { $set: { start_location: '   ' } },
    );

    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/recommendations`)
      .send({});

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INCOMPLETE_PROFILE');
  });

  test('returns RECOMMENDATION_NOT_CONFIGURED for missing LLM settings', async () => {
    const notConfiguredApp = createApp({
      recommendationGenerator: new SoclaasClient({
        endpoint: ' ',
        apiKey: undefined,
        model: '',
        maxOutputTokens: 1,
      }),
      recommendationRanker: new UniversalSentenceEncoderItineraryRanker(),
      recommendationRepository: new RecommendationDao(database),
      userRepository: new UserDao(database),
    });

    const response = await request(notConfiguredApp)
      .post(`/api/v1/users/${USER_ID}/recommendations`)
      .send({});

    expect(response.status).toBe(500);
    expect(response.body.error.code).toBe('RECOMMENDATION_NOT_CONFIGURED');
  });

  test('maps a real provider connection failure to a 502 response', async () => {
    const unavailableProviderApp = createApp({
      recommendationGenerator: new SoclaasClient({
        endpoint: 'http://127.0.0.1:65534',
        apiKey: 'no-mock-test-key',
        model: 'no-mock-test-model',
        maxOutputTokens: 1,
      }),
      recommendationRanker: new UniversalSentenceEncoderItineraryRanker(),
      recommendationRepository: new RecommendationDao(database),
      userRepository: new UserDao(database),
    });

    const response = await request(unavailableProviderApp)
      .post(`/api/v1/users/${USER_ID}/recommendations`)
      .send({});

    expect(response.status).toBe(502);
    expect(response.body.error.code).toBe('RECOMMENDATION_GENERATION_FAILED');
  });
});

describe('Recommendation response parsing — no mocks', () => {
  test('accepts fenced JSON, trims strings, and normalizes optional fields', () => {
    const payload = validPayload();
    payload.itineraries[0]!.name = '  Trimmed name  ';
    delete (payload.itineraries[0]!.items[0] as Record<string, unknown>).image;
    delete (payload.itineraries[0]!.items[0] as Record<string, unknown>).description;
    delete (payload.itineraries[0]!.items[0] as Record<string, unknown>).category;
    delete (payload.itineraries[0] as Record<string, unknown>).description;

    const parsed = recommendationService.parseItineraries(
      `\`\`\`json\n${JSON.stringify(payload)}\n\`\`\``,
    );

    expect(parsed).toHaveLength(4);
    expect(parsed[0]).toMatchObject({ name: 'Trimmed name' });
    expect(parsed[0]).not.toHaveProperty('description');
    expect(parsed[0]!.items[0]).toMatchObject({ image: '' });
    expect(parsed[0]!.items[0]).not.toHaveProperty('description');
    expect(parsed[0]!.items[0]).not.toHaveProperty('category');
  });

  test('extracts a JSON object from surrounding provider text', () => {
    const parsed = parseItineraries(
      `Generated result:\n${JSON.stringify(validPayload())}\nEnd result.`,
    );

    expect(parsed).toHaveLength(4);
  });

  test.each([
    ['non-JSON text', 'not JSON at all'],
    ['malformed embedded JSON', 'prefix {not-json} suffix'],
    ['a non-object payload', 'null'],
    ['the wrong itinerary count', JSON.stringify({ itineraries: [] })],
  ])('rejects %s', (_scenario, text) => {
    expect(() => parseItineraries(text))
      .toThrow(InvalidRecommendationResponseError);
  });

  test.each([
    ['a primitive candidate', 42],
    ['an empty name', { ...validCandidate(), name: ' ' }],
    ['empty items', { ...validCandidate(), items: [] }],
    ['a primitive item', { ...validCandidate(), items: [null] }],
    ['an unknown item type', {
      ...validCandidate(),
      items: [{ type: 'lodging' }],
    }],
    ['invalid activity fields', {
      ...validCandidate(),
      items: [{
        type: 'activity',
        name: '',
        location: '',
        startTime: 'not-a-date',
        endTime: null,
      }],
    }],
    ['an invalid activity time range', {
      ...validCandidate(),
      items: [
        {
          ...(validCandidate().items[0] as Record<string, unknown>),
          startTime: '2027-11-01T12:00:00.000Z',
          endTime: '2027-11-01T11:00:00.000Z',
        },
        validCandidate().items[1],
      ],
    }],
    ['invalid transportation fields', {
      ...validCandidate(),
      items: [
        validCandidate().items[0],
        {
          type: 'transportation',
          mode: 'flying',
          startLocation: '',
          endLocation: null,
          distanceMeters: -1,
          startTime: 'not-a-date',
          endTime: null,
        },
      ],
    }],
    ['an invalid transportation time range', {
      ...validCandidate(),
      items: [
        validCandidate().items[0],
        {
          ...(validCandidate().items[1] as Record<string, unknown>),
          startTime: '2027-11-01T12:00:00.000Z',
          endTime: '2027-11-01T11:00:00.000Z',
        },
      ],
    }],
  ])('rejects %s', (_scenario, invalidCandidate) => {
    const payload = validPayload();
    payload.itineraries[0] = invalidCandidate as ReturnType<typeof validCandidate>;

    expect(() => parseItineraries(JSON.stringify(payload)))
      .toThrow(InvalidRecommendationResponseError);
  });

  test('uses domain defaults and handles text with no rankable words', () => {
    expect(new RecommendationGenerationError().message)
      .toBe('The itinerary generation service is unavailable.');
    expect(new InvalidRecommendationResponseError(['bad']).details)
      .toEqual(['bad']);
    expect(cleanTextForRanking('... !!! ___')).toBe('');
  });

  test('maps a real connection failure to a generation error', async () => {
    const unreachableClient = new SoclaasClient({
      endpoint: 'http://127.0.0.1:65534',
      apiKey: 'no-mock-test-key',
      model: 'no-mock-test-model',
      maxOutputTokens: 1,
    });

    await expect(unreachableClient.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: {
        transportation: { mode: 'transit' },
        budget: 'balanced',
        interest: 'art',
      },
    })).rejects.toBeInstanceOf(RecommendationGenerationError);
  });
});
