import 'dotenv/config';

import * as os from 'node:os';

import type { Express } from 'express';
import { Db, MongoClient, ObjectId } from 'mongodb';
import request from 'supertest';

import { createApp } from '../../../src/index';
import { UserDao } from '../../../src/users/userDao';

jest.setTimeout(30_000);

const USER_ID = new ObjectId();
const MISSING_USER_ID = new ObjectId();
const DATABASE_NAME = `exploreubc_jest_users_${new ObjectId().toHexString()}`;

let client: MongoClient;
let database: Db;
let app: Express;

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
  app = createApp({ userRepository: new UserDao(database) });
  await client.connect();
});

beforeEach(async () => {
  await database.collection('users').deleteMany({});
  await database.collection('users').insertOne({
    _id: USER_ID,
    googleSubject: 'google-subject-123',
    email: 'test.user@student.ubc.ca',
    displayName: 'Test User',
    start_location: 'UBC Bus Exchange',
    preferences: {
      transportation: { mode: 'transit' },
      budget: 'balanced',
      interest: 'outdoors',
    },
    create_at: new Date('2026-10-12T12:00:00.000Z'),
    updated_at: new Date('2026-10-12T12:00:00.000Z'),
  });
});

afterAll(async () => {
  await database.dropDatabase();
  await client.close();
});

describe('GET /api/v1/users/:userId — no mocks', () => {
  test('returns the complete user stored in MongoDB', async () => {
    // Input: existing user ID. Expect: 200 and the complete persisted profile.
    // Behavior: the real UserDao reads from MongoDB; no component is mocked.
    const response = await request(app).get(`/api/v1/users/${USER_ID}`);

    expect(response.status).toBe(200);
    expect(response.body.user).toEqual({
      googleSubject: 'google-subject-123',
      email: 'test.user@student.ubc.ca',
      displayName: 'Test User',
      start_location: 'UBC Bus Exchange',
      preferences: {
        transportation: { mode: 'transit' },
        budget: 'balanced',
        interest: 'outdoors',
      },
      create_at: '2026-10-12T12:00:00.000Z',
      updated_at: '2026-10-12T12:00:00.000Z',
    });
  });

  test('returns USER_NOT_FOUND for an absent user', async () => {
    // Input: absent valid ObjectId. Expect: 404 USER_NOT_FOUND and no mutation.
    // Behavior: the real MongoDB query returns no document; no component is mocked.
    const response = await request(app).get(`/api/v1/users/${MISSING_USER_ID}`);

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
  });

  test('returns USER_NOT_FOUND for a malformed user ID', async () => {
    // Input: malformed ID. Expect: 404 without issuing a matching MongoDB query.
    // Behavior: the real UserDao rejects the ID; no component is mocked.
    const response = await request(app).get('/api/v1/users/not-an-object-id');

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
  });
});

describe('PATCH /api/v1/users/:userId — no mocks', () => {
  test('updates profile fields and nested preferences in MongoDB', async () => {
    // Input: display name, start location, and preferences. Expect: 200 and merged user.
    // Behavior: the real UserDao updates MongoDB and the API reads the saved document back.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send({
        displayName: 'Updated User',
        start_location: 'UBC Bookstore',
        preferences: {
          transportation: { mode: 'walking' },
          budget: 'premium',
          interest: 'art',
        },
      });

    expect(response.status).toBe(200);
    expect(response.body.user).toMatchObject({
      displayName: 'Updated User',
      start_location: 'UBC Bookstore',
      preferences: {
        transportation: { mode: 'walking' },
        budget: 'premium',
        interest: 'art',
      },
    });
    await expect(database.collection('users').findOne({ _id: USER_ID }))
      .resolves.toMatchObject({ displayName: 'Updated User' });
  });

  test('preserves omitted fields during a partial update', async () => {
    // Input: only a new start location. Expect: 200 with all omitted fields unchanged.
    // Behavior: MongoDB receives a partial $set through the real UserDao.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send({ start_location: 'Nitobe Memorial Garden' });

    expect(response.status).toBe(200);
    expect(response.body.user).toMatchObject({
      displayName: 'Test User',
      start_location: 'Nitobe Memorial Garden',
      preferences: { budget: 'balanced' },
    });
  });

  test('rejects an empty or unexpected update before changing MongoDB', async () => {
    // Input: empty body and immutable email field. Expect: 400 INVALID_USER_UPDATE.
    // Behavior: validation rejects both requests; the real stored document stays unchanged.
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
    await expect(database.collection('users').findOne({ _id: USER_ID }))
      .resolves.toMatchObject({ email: 'test.user@student.ubc.ca' });
  });

  test('returns USER_NOT_FOUND without inserting an absent user', async () => {
    // Input: update for an absent valid ObjectId. Expect: 404 USER_NOT_FOUND.
    // Behavior: the real MongoDB update matches zero documents and inserts nothing.
    const response = await request(app)
      .patch(`/api/v1/users/${MISSING_USER_ID}`)
      .send({ displayName: 'Missing User' });

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
    await expect(
      database.collection('users').countDocuments({ _id: MISSING_USER_ID }),
    ).resolves.toBe(0);
  });

  test.each([
    [['not-an-object']],
    [{ displayName: 42 }],
    [{ displayName: '   ' }],
    [{ displayName: 'a'.repeat(201) }],
    [{ start_location: 'a'.repeat(501) }],
    [{ preferences: null }],
    [{ preferences: {} }],
    [{ preferences: { unknown: true } }],
    [{ preferences: { transportation: null } }],
    [{ preferences: { transportation: {} } }],
    [{ preferences: { transportation: { mode: 'flying' } } }],
    [{ preferences: { budget: 'free' } }],
    [{ preferences: { interest: 'sports' } }],
  ])('rejects invalid update payload %#', async (body) => {
    // Input: malformed field or preference. Expect: 400 INVALID_USER_UPDATE.
    // Behavior: live request validation rejects it before the real database changes.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_USER_UPDATE');
  });

  test('rejects a valid update sent to a malformed user ID', async () => {
    // Input: malformed route ID and valid body. Expect: 404 USER_NOT_FOUND.
    // Behavior: the real UserDao returns false without updating MongoDB.
    const response = await request(app)
      .patch('/api/v1/users/not-an-object-id')
      .send({ displayName: 'Valid Name' });

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
  });
});
