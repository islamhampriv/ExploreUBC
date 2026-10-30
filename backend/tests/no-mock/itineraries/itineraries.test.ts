import 'dotenv/config';

import * as os from 'node:os';

import type { Express } from 'express';
import { Db, MongoClient, ObjectId } from 'mongodb';
import request from 'supertest';

import { createApp } from '../../../src/index';
import { ItineraryDao } from '../../../src/itineraries/itineraryDao';
import type { ActivityItem } from '../../../src/itineraries/itineraryTypes';

jest.setTimeout(30_000);

const USER_ID = new ObjectId();
const OTHER_USER_ID = new ObjectId();
const ITINERARY_ID = new ObjectId();
const MISSING_ITINERARY_ID = new ObjectId();
const ACTIVITY_ID = 'activity-1';
const SEGMENT_ID = 'transportation-1';
const DATABASE_NAME = `exploreubc_jest_itineraries_${new ObjectId().toHexString()}`;

const ACTIVITY_INPUT = {
  type: 'activity',
  name: 'Nitobe Memorial Garden',
  description: 'Walk through the Japanese garden.',
  category: 'outdoors',
  location: '1895 Lower Mall, Vancouver, BC',
  startTime: '2027-11-30T22:00:00.000Z',
  endTime: '2027-11-30T23:00:00.000Z',
};

const TRANSPORTATION_INPUT = {
  type: 'transportation',
  mode: 'walking',
  startLocation: 'UBC Nest',
  endLocation: 'UBC MCLD',
  distanceMeters: 400,
  startTime: '2027-11-30T20:00:00.000Z',
  endTime: '2027-11-30T20:30:00.000Z',
};

const CREATE_ITINERARY_INPUT = {
  ownerId: USER_ID.toHexString(),
  name: 'UBC afternoon',
  description: 'A complete itinerary created through the API.',
  startingLocation: 'UBC Bus Exchange',
  items: [ACTIVITY_INPUT, TRANSPORTATION_INPUT],
  saved: true,
  inRecommendation: false,
  score: {},
};

let client: MongoClient;
let database: Db;
let app: Express;
let itineraryDao: ItineraryDao;

function seededItinerary() {
  return {
    _id: ITINERARY_ID,
    ownerId: USER_ID,
    name: 'Existing itinerary',
    description: 'Stored in the real test database.',
    startingLocation: 'UBC Bus Exchange',
    items: [
      {
        id: ACTIVITY_ID,
        type: 'activity',
        name: 'Museum of Anthropology',
        image: 'https://example.com/moa.jpg',
        description: 'Explore the museum.',
        category: 'art',
        location: '6393 NW Marine Drive, Vancouver, BC',
        startTime: '2027-11-30T17:00:00.000Z',
        endTime: '2027-11-30T18:00:00.000Z',
      },
      {
        id: SEGMENT_ID,
        type: 'transportation',
        mode: 'walking',
        startLocation: 'Museum of Anthropology',
        endLocation: 'UBC Nest',
        distanceMeters: 1200,
        startTime: '2027-11-30T18:00:00.000Z',
        endTime: '2027-11-30T18:30:00.000Z',
      },
    ],
    createdAt: new Date('2026-10-12T12:00:00.000Z'),
    updatedAt: new Date('2026-10-12T12:00:00.000Z'),
    saved: true,
    inRecommendation: false,
    score: {},
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
  itineraryDao = new ItineraryDao(database);
  app = createApp({ itineraryRepository: itineraryDao });
  await client.connect();
});

beforeEach(async () => {
  await database.collection('itineraries').deleteMany({});
  await database.collection('itineraries').insertOne(seededItinerary());
});

afterAll(async () => {
  await database.dropDatabase();
  await client.close();
});

describe('Itinerary repository malformed-ID guards — no mocks', () => {
  test('returns safe not-found results before issuing MongoDB writes', async () => {
    // Input: malformed owner IDs for each repository operation. Expect: null/false.
    // Behavior: the real DAO rejects invalid ObjectIds without calling a mocked dependency.
    const activity: ActivityItem = {
      ...ACTIVITY_INPUT,
      type: 'activity',
      id: 'invalid-owner-activity',
      image: '',
    };

    await expect(
      itineraryDao.findItinerary('invalid-owner', ITINERARY_ID.toHexString()),
    ).resolves.toBeNull();
    await expect(
      itineraryDao.updateItineraryDetails(
        'invalid-owner',
        ITINERARY_ID.toHexString(),
        { name: 'Ignored' },
      ),
    ).resolves.toBe(false);
    await expect(
      itineraryDao.insertEmbeddedActivity(
        'invalid-owner',
        ITINERARY_ID.toHexString(),
        activity,
      ),
    ).resolves.toBe(false);
    await expect(
      itineraryDao.removeEmbeddedActivity(
        'invalid-owner',
        ITINERARY_ID.toHexString(),
        activity.id,
      ),
    ).resolves.toBe(false);
    await expect(
      itineraryDao.updateEmbeddedActivity(
        'invalid-owner',
        ITINERARY_ID.toHexString(),
        activity.id,
        { name: 'Ignored' },
      ),
    ).resolves.toBe(false);
  });
});

describe('POST /api/v1/users/:userId/itineraries — no mocks', () => {
  test('creates a complete itinerary in MongoDB', async () => {
    // Input: valid itinerary with two item types. Expect: 201, Location, and generated IDs.
    // Behavior: the real ItineraryDao inserts and reads the MongoDB document.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries`)
      .send(CREATE_ITINERARY_INPUT);

    expect(response.status).toBe(201);
    expect(response.headers.location).toBe(
      `/api/v1/users/${USER_ID}/itineraries/${response.body.itinerary.id}`,
    );
    expect(response.body.itinerary).toMatchObject({
      ownerId: USER_ID.toHexString(),
      name: 'UBC afternoon',
      saved: true,
    });
    expect(response.body.itinerary.items).toHaveLength(2);
    expect(response.body.itinerary.items[0].id).toEqual(expect.any(String));
    await expect(database.collection('itineraries').countDocuments({
      _id: new ObjectId(response.body.itinerary.id),
    })).resolves.toBe(1);
  });

  test('rejects a body owner that conflicts with the route', async () => {
    // Input: different ownerId. Expect: 400 INVALID_ITINERARY_DETAILS, no insert.
    // Behavior: validation rejects the request before the real database is changed.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries`)
      .send({ ...CREATE_ITINERARY_INPUT, ownerId: OTHER_USER_ID.toHexString() });

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_DETAILS');
    expect(response.body.error.details).toContain(
      'ownerId must match the userId route parameter.',
    );
  });

  test('creates a minimal itinerary without optional details', async () => {
    // Input: valid itinerary with omitted optional strings. Expect: 201 without them.
    // Behavior: the real DAO persists and maps the minimal MongoDB document.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries`)
      .send({
        ownerId: USER_ID.toHexString(),
        name: 'Minimal itinerary',
        items: [],
        saved: false,
        inRecommendation: false,
        score: {},
      });

    expect(response.status).toBe(201);
    expect(response.body.itinerary).not.toHaveProperty('description');
    expect(response.body.itinerary).not.toHaveProperty('startingLocation');

    const detailsResponse = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${response.body.itinerary.id}/details`,
    );
    expect(detailsResponse.body.details).toEqual({ name: 'Minimal itinerary' });
  });

  test('creates an itinerary with finite score values', async () => {
    // Input: valid finite score values. Expect: 201 with the scores persisted.
    // Behavior: live validation accepts each numeric score and the real DAO stores it.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries`)
      .send({
        ...CREATE_ITINERARY_INPUT,
        name: 'Scored itinerary',
        score: { relevance: 0.9, distance: 12 },
      });

    expect(response.status).toBe(201);
    expect(response.body.itinerary.score).toEqual({
      relevance: 0.9,
      distance: 12,
    });
  });

  test.each([
    [[]],
    [{
      ...CREATE_ITINERARY_INPUT,
      extra: true,
      ownerId: ' ',
      name: 'a'.repeat(101),
      description: 42,
      startingLocation: 'a'.repeat(501),
      items: 'not-an-array',
      saved: 'true',
      inRecommendation: 1,
      score: null,
    }],
    [{ ...CREATE_ITINERARY_INPUT, items: [null, { type: 'event' }] }],
    [{
      ...CREATE_ITINERARY_INPUT,
      items: [{
        type: 'activity',
        name: 1,
        image: 2,
        description: 'a'.repeat(1001),
        category: ' ',
        location: 'a'.repeat(501),
        startTime: 'bad-date',
        endTime: null,
      }],
    }],
    [{
      ...CREATE_ITINERARY_INPUT,
      items: [{ ...ACTIVITY_INPUT, id: 'client-id', endTime: ACTIVITY_INPUT.startTime }],
    }],
    [{
      ...CREATE_ITINERARY_INPUT,
      items: [{
        ...TRANSPORTATION_INPUT,
        mode: 3,
        startLocation: '',
        endLocation: 4,
        distanceMeters: -1,
        startTime: 'invalid',
      }],
    }],
    [{ ...CREATE_ITINERARY_INPUT, score: { relevance: 'high' } }],
  ])('rejects malformed itinerary payload %#', async (body) => {
    // Input: malformed itinerary structure or field. Expect: 400 details error.
    // Behavior: the live validation path rejects it without inserting into MongoDB.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_DETAILS');
  });

  test('rejects a valid body when the route owner ID is malformed', async () => {
    // Input: malformed route owner ID with matching body owner. Expect: 400 INVALID_USER_ID.
    // Behavior: the real ItineraryDao rejects the owner before inserting.
    const response = await request(app)
      .post('/api/v1/users/not-an-object-id/itineraries')
      .send({ ...CREATE_ITINERARY_INPUT, ownerId: 'not-an-object-id' });

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_USER_ID');
  });
});

describe('GET /api/v1/users/:userId/itineraries — no mocks', () => {
  test('returns only itineraries matching the saved filter', async () => {
    // Input: saved=true. Expect: 200 with only the owner’s saved itineraries.
    // Behavior: the real DAO applies owner and saved filters in MongoDB.
    await database.collection('itineraries').insertOne({
      ...seededItinerary(), _id: new ObjectId(), name: 'Unsaved', saved: false,
    });
    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries?saved=true`,
    );

    expect(response.status).toBe(200);
    expect(response.body.itineraries).toHaveLength(1);
    expect(response.body.itineraries[0].name).toBe('Existing itinerary');
  });

  test('rejects a non-boolean saved filter', async () => {
    // Input: saved=occasionally. Expect: 400 INVALID_ITINERARY_FILTERS.
    // Behavior: query validation rejects the input; no external component is mocked.
    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries?saved=occasionally`,
    );
    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_FILTERS');
  });

  test.each([
    ['?saved=false', 0],
    ['?inRecommendation=false', 1],
    ['?inRecommendation=true', 0],
    ['?saved=true&inRecommendation=false', 1],
  ])('supports Boolean filter combination %s', async (query, count) => {
    // Input: valid Boolean filter. Expect: 200 and the matching number of documents.
    // Behavior: filter parsing and the real MongoDB query both run without mocks.
    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries${query}`,
    );

    expect(response.status).toBe(200);
    expect(response.body.itineraries).toHaveLength(count);
  });

  test('returns an empty list for a malformed owner ID', async () => {
    // Input: malformed route owner ID. Expect: 200 with an empty list.
    // Behavior: the real DAO safely rejects the owner ID.
    const response = await request(app).get(
      '/api/v1/users/not-an-object-id/itineraries',
    );

    expect(response.status).toBe(200);
    expect(response.body.itineraries).toEqual([]);
  });
});

describe('GET /api/v1/users/:userId/itineraries/:itineraryId/details — no mocks', () => {
  test('returns only the stored itinerary details', async () => {
    // Input: owner and itinerary IDs. Expect: 200 with details and no items.
    // Behavior: the real DAO uses a MongoDB projection.
    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/details`,
    );
    expect(response.status).toBe(200);
    expect(response.body.details).toEqual({
      name: 'Existing itinerary',
      description: 'Stored in the real test database.',
      startingLocation: 'UBC Bus Exchange',
    });
    expect(response.body.details).not.toHaveProperty('items');
  });

  test('does not expose another owner’s details', async () => {
    // Input: wrong owner with existing itinerary ID. Expect: 404 ITINERARY_NOT_FOUND.
    // Behavior: the real MongoDB query enforces owner scoping.
    const response = await request(app).get(
      `/api/v1/users/${OTHER_USER_ID}/itineraries/${ITINERARY_ID}/details`,
    );
    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });

  test('returns not found for malformed detail IDs', async () => {
    // Input: malformed owner and itinerary IDs. Expect: 404 ITINERARY_NOT_FOUND.
    // Behavior: the real DAO rejects invalid ObjectIds without querying MongoDB.
    const response = await request(app).get(
      '/api/v1/users/not-an-id/itineraries/not-an-id/details',
    );

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

describe('GET /api/v1/users/:userId/itineraries/:itineraryId — no mocks', () => {
  test('returns the complete owner-scoped itinerary', async () => {
    // Input: matching owner and itinerary IDs. Expect: 200 with details and items.
    // Behavior: the real ItineraryDao maps the MongoDB document to the API type.
    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`,
    );
    expect(response.status).toBe(200);
    expect(response.body.itinerary).toMatchObject({
      id: ITINERARY_ID.toHexString(), ownerId: USER_ID.toHexString(),
    });
    expect(response.body.itinerary.items).toHaveLength(2);
  });

  test('returns not found for an absent itinerary', async () => {
    // Input: absent valid ID. Expect: 404 ITINERARY_NOT_FOUND.
    // Behavior: the real MongoDB lookup returns no document.
    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${MISSING_ITINERARY_ID}`,
    );
    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

describe('PATCH /api/v1/users/:userId/itineraries/:itineraryId — no mocks', () => {
  test('updates details while preserving stored items', async () => {
    // Input: new name and location. Expect: 200 with updated details and original items.
    // Behavior: the real DAO performs a partial MongoDB $set.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .send({ name: 'Updated itinerary', startingLocation: 'UBC Nest' });
    expect(response.status).toBe(200);
    expect(response.body.itinerary).toMatchObject({
      name: 'Updated itinerary', startingLocation: 'UBC Nest',
    });
    expect(response.body.itinerary.items).toHaveLength(2);
  });

  test('rejects an empty details update', async () => {
    // Input: empty body. Expect: 400 INVALID_ITINERARY_DETAILS and no mutation.
    // Behavior: validation stops the request before the real DAO runs.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .send({});
    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_DETAILS');
  });

  test('updates every supported itinerary detail field', async () => {
    // Input: all mutable detail fields. Expect: 200 with all persisted values.
    // Behavior: every real DAO partial-update branch writes to MongoDB.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .send({
        name: 'All details',
        description: 'All fields changed.',
        startingLocation: 'Main Mall',
        saved: false,
        inRecommendation: true,
      });

    expect(response.status).toBe(200);
    expect(response.body.itinerary).toMatchObject({
      name: 'All details',
      description: 'All fields changed.',
      startingLocation: 'Main Mall',
      saved: false,
      inRecommendation: true,
    });
  });

  test('updates only the optional description', async () => {
    // Input: description without a name. Expect: 200 with the stored name preserved.
    // Behavior: validation takes the omitted-name branch and the real DAO updates one field.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .send({ description: 'Description-only update.' });

    expect(response.status).toBe(200);
    expect(response.body.itinerary).toMatchObject({
      name: 'Existing itinerary',
      description: 'Description-only update.',
    });
  });

  test.each([
    [[]],
    [{ unknown: true }],
    [{ name: '' }],
    [{ name: 'a'.repeat(101) }],
    [{ description: 1 }],
    [{ startingLocation: 'a'.repeat(501) }],
    [{ saved: 'true' }],
    [{ inRecommendation: 'false' }],
  ])('rejects malformed itinerary detail update %#', async (body) => {
    // Input: malformed detail update. Expect: 400 INVALID_ITINERARY_DETAILS.
    // Behavior: validation rejects it before the real database changes.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_DETAILS');
  });

  test('returns not found when updating an absent itinerary', async () => {
    // Input: absent itinerary and valid detail. Expect: 404 ITINERARY_NOT_FOUND.
    // Behavior: the real MongoDB update matches no document.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${MISSING_ITINERARY_ID}`)
      .send({ name: 'Missing' });

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

describe('DELETE /api/v1/users/:userId/itineraries/:itineraryId — no mocks', () => {
  test('deletes the owner-scoped itinerary from MongoDB', async () => {
    // Input: matching owner and itinerary IDs. Expect: 204 and no stored document.
    // Behavior: the real DAO deletes exactly one MongoDB document.
    const response = await request(app).delete(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`,
    );
    expect(response.status).toBe(204);
    await expect(database.collection('itineraries').countDocuments({
      _id: ITINERARY_ID,
    })).resolves.toBe(0);
  });

  test.each([
    [`/api/v1/users/${OTHER_USER_ID}/itineraries/${ITINERARY_ID}`],
    [`/api/v1/users/${USER_ID}/itineraries/${MISSING_ITINERARY_ID}`],
    ['/api/v1/users/not-an-id/itineraries/not-an-id'],
  ])('returns not found instead of deleting through %s', async (path) => {
    // Input: wrong owner, absent itinerary, or malformed IDs. Expect: 404.
    // Behavior: the real DAO deletes no MongoDB document.
    const response = await request(app).delete(path);

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

describe('POST /api/v1/users/:userId/itineraries/:itineraryId/activities — no mocks', () => {
  test('adds an activity with an optional blank image', async () => {
    // Input: valid activity without image. Expect: 201, blank image, generated ID.
    // Behavior: the service and real DAO append the activity in MongoDB.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities`)
      .send(ACTIVITY_INPUT);
    expect(response.status).toBe(201);
    expect(response.body.activity).toMatchObject({
      ...ACTIVITY_INPUT, image: '', id: expect.any(String),
    });
    await expect(database.collection('itineraries').findOne({
      _id: ITINERARY_ID, 'items.id': response.body.activity.id,
    })).resolves.not.toBeNull();
  });

  test('returns a conflict for an overlapping activity', async () => {
    // Input: activity overlapping a stored item. Expect: 409 ITINERARY_TIME_CONFLICT.
    // Behavior: the service compares against items loaded from real MongoDB.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities`)
      .send({
        ...ACTIVITY_INPUT,
        startTime: '2027-11-30T17:30:00.000Z',
        endTime: '2027-11-30T18:15:00.000Z',
      });
    expect(response.status).toBe(409);
    expect(response.body.error.code).toBe('ITINERARY_TIME_CONFLICT');
  });

  test('identifies a conflicting transportation segment', async () => {
    // Input: activity overlapping only stored transportation. Expect: 409 conflict.
    // Behavior: the service takes the transportation-name branch using real stored items.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities`)
      .send({
        ...ACTIVITY_INPUT,
        startTime: '2027-11-30T18:05:00.000Z',
        endTime: '2027-11-30T18:20:00.000Z',
      });

    expect(response.status).toBe(409);
    expect(response.body.error.message).toContain('transportation segment');
  });

  test.each([
    [[]],
    [{ ...ACTIVITY_INPUT, type: 'transportation' }],
    [{ ...ACTIVITY_INPUT, id: 'client-id' }],
    [{ ...ACTIVITY_INPUT, name: '' }],
    [{ ...ACTIVITY_INPUT, name: 'a'.repeat(201) }],
    [{ ...ACTIVITY_INPUT, image: 1 }],
    [{ ...ACTIVITY_INPUT, image: 'a'.repeat(2001) }],
    [{ ...ACTIVITY_INPUT, description: 1 }],
    [{ ...ACTIVITY_INPUT, category: '' }],
    [{ ...ACTIVITY_INPUT, location: 1 }],
    [{ ...ACTIVITY_INPUT, startTime: 'invalid' }],
    [{ ...ACTIVITY_INPUT, endTime: ACTIVITY_INPUT.startTime }],
  ])('rejects malformed activity input %#', async (body) => {
    // Input: malformed activity field. Expect: 400 INVALID_ITINERARY_ITEM.
    // Behavior: request validation rejects it without mutating real MongoDB.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_ITEM');
  });

  test('returns not found when adding an activity to an absent itinerary', async () => {
    // Input: valid activity and absent itinerary. Expect: 404 ITINERARY_NOT_FOUND.
    // Behavior: the real DAO lookup returns no itinerary.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${MISSING_ITINERARY_ID}/activities`)
      .send(ACTIVITY_INPUT);

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

describe('PATCH /api/v1/users/:userId/itineraries/:itineraryId/activities/:activityId — no mocks', () => {
  test('partially updates an activity in MongoDB', async () => {
    // Input: existing activity and new name/category. Expect: 200, omitted fields preserved.
    // Behavior: the real DAO updates the matching embedded activity.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities/${ACTIVITY_ID}`)
      .send({ name: 'Edited activity', category: 'outdoors' });
    expect(response.status).toBe(200);
    expect(response.body.activity).toMatchObject({
      id: ACTIVITY_ID,
      name: 'Edited activity',
      category: 'outdoors',
      location: '6393 NW Marine Drive, Vancouver, BC',
    });
  });

  test('rejects a partial update with an invalid merged time range', async () => {
    // Input: end before stored start. Expect: 400 INVALID_ITINERARY_ITEM.
    // Behavior: the service validates merged stored and submitted values.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities/${ACTIVITY_ID}`)
      .send({ endTime: '2027-11-30T16:00:00.000Z' });
    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_ITEM');
  });

  test('updates every supported activity field', async () => {
    // Input: all mutable fields with a valid range. Expect: 200 with all changes.
    // Behavior: all activity-update validation and real embedded-update branches run.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities/${ACTIVITY_ID}`)
      .send({
        name: 'Every field',
        image: '',
        description: 'Updated description',
        category: 'outdoors',
        location: 'Main Mall',
        startTime: '2027-11-30T16:00:00.000Z',
        endTime: '2027-11-30T16:30:00.000Z',
      });

    expect(response.status).toBe(200);
    expect(response.body.activity).toMatchObject({
      name: 'Every field', location: 'Main Mall', image: '',
    });
  });

  test.each([
    [[]],
    [{ unknown: true }],
    [{ name: 1 }],
    [{ image: 1 }],
    [{ description: 'a'.repeat(1001) }],
    [{ category: '' }],
    [{ location: 'a'.repeat(501) }],
    [{ startTime: 'bad' }],
    [{ endTime: 'bad' }],
    [{ startTime: '2027-11-30T19:00:00.000Z', endTime: '2027-11-30T18:00:00.000Z' }],
  ])('rejects malformed activity update %#', async (body) => {
    // Input: malformed activity update. Expect: 400 INVALID_ITINERARY_ITEM.
    // Behavior: validation rejects it before the real DAO mutates MongoDB.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities/${ACTIVITY_ID}`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_ITEM');
  });

  test('returns item not found for the wrong embedded item type', async () => {
    // Input: transportation ID on activity route. Expect: 404 item-not-found.
    // Behavior: the service checks both ID and type on the real stored itinerary.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities/${SEGMENT_ID}`)
      .send({ name: 'Wrong type' });

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_ITEM_NOT_FOUND');
  });

  test('does not allow item modification on an unsaved itinerary', async () => {
    // Input: existing item on an unsaved itinerary. Expect: 404 itinerary-not-found.
    // Behavior: the service enforces saved state loaded from real MongoDB.
    await database.collection('itineraries').updateOne(
      { _id: ITINERARY_ID },
      { $set: { saved: false } },
    );
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities/${ACTIVITY_ID}`)
      .send({ name: 'Blocked update' });

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

describe('DELETE /api/v1/users/:userId/itineraries/:itineraryId/activities/:activityId — no mocks', () => {
  test('removes the activity from the stored itinerary', async () => {
    // Input: existing activity ID. Expect: 204 and the embedded item removed.
    // Behavior: the real DAO pulls the matching activity from MongoDB.
    const response = await request(app).delete(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities/${ACTIVITY_ID}`,
    );
    expect(response.status).toBe(204);
    await expect(database.collection('itineraries').findOne({
      _id: ITINERARY_ID, 'items.id': ACTIVITY_ID,
    })).resolves.toBeNull();
  });

  test('returns item not found when deleting the wrong item type', async () => {
    // Input: transportation ID on activity route. Expect: 404 item-not-found.
    // Behavior: the real stored item fails the service type check.
    const response = await request(app).delete(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities/${SEGMENT_ID}`,
    );

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_ITEM_NOT_FOUND');
  });
});

describe('POST /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments — no mocks', () => {
  test('adds a transportation segment with a generated ID', async () => {
    // Input: valid segment. Expect: 201 with generated ID and submitted fields.
    // Behavior: the real DAO appends the segment to the MongoDB itinerary.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments`)
      .send(TRANSPORTATION_INPUT);
    expect(response.status).toBe(201);
    expect(response.body.transportationSegment).toMatchObject({
      ...TRANSPORTATION_INPUT, id: expect.any(String),
    });
    await expect(database.collection('itineraries').findOne({
      _id: ITINERARY_ID, 'items.id': response.body.transportationSegment.id,
    })).resolves.not.toBeNull();
  });

  test('rejects an unsupported mode', async () => {
    // Input: mode=flying. Expect: 400 INVALID_ITINERARY_ITEM, no insert.
    // Behavior: validation rejects the request before MongoDB is changed.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments`)
      .send({ ...TRANSPORTATION_INPUT, mode: 'flying' });
    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_ITEM');
  });

  test.each([
    [[]],
    [{ ...TRANSPORTATION_INPUT, type: 'activity' }],
    [{ ...TRANSPORTATION_INPUT, id: 'client-id' }],
    [{ ...TRANSPORTATION_INPUT, mode: 3 }],
    [{ ...TRANSPORTATION_INPUT, startLocation: '' }],
    [{ ...TRANSPORTATION_INPUT, endLocation: 1 }],
    [{ ...TRANSPORTATION_INPUT, distanceMeters: '400' }],
    [{ ...TRANSPORTATION_INPUT, distanceMeters: -1 }],
    [{ ...TRANSPORTATION_INPUT, startTime: 'bad' }],
    [{ ...TRANSPORTATION_INPUT, endTime: TRANSPORTATION_INPUT.startTime }],
  ])('rejects malformed transportation input %#', async (body) => {
    // Input: malformed transportation field. Expect: 400 item error.
    // Behavior: request validation rejects it before the real database changes.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_ITEM');
  });

  test('returns not found when adding transportation to an absent itinerary', async () => {
    // Input: valid segment and absent itinerary. Expect: 404 ITINERARY_NOT_FOUND.
    // Behavior: the real embedded MongoDB update matches no document.
    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${MISSING_ITINERARY_ID}/transportation-segments`)
      .send(TRANSPORTATION_INPUT);

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

describe('PATCH /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments/:segmentId — no mocks', () => {
  test('partially updates a transportation segment in MongoDB', async () => {
    // Input: existing segment and new mode/distance. Expect: 200, other fields preserved.
    // Behavior: the real DAO updates the matching embedded segment.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments/${SEGMENT_ID}`)
      .send({ mode: 'transit', distanceMeters: 1000 });
    expect(response.status).toBe(200);
    expect(response.body.transportationSegment).toMatchObject({
      id: SEGMENT_ID, mode: 'transit', distanceMeters: 1000,
      endLocation: 'UBC Nest',
    });
  });

  test('rejects an empty segment update', async () => {
    // Input: empty update. Expect: 400 INVALID_ITINERARY_ITEM and no mutation.
    // Behavior: validation rejects the request before the real DAO runs.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments/${SEGMENT_ID}`)
      .send({});
    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_ITEM');
  });

  test('updates every supported transportation field', async () => {
    // Input: every mutable segment field. Expect: 200 with all persisted changes.
    // Behavior: all update-validation and real embedded-update branches run.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments/${SEGMENT_ID}`)
      .send({
        mode: 'cycling',
        startLocation: 'MOA',
        endLocation: 'Main Mall',
        distanceMeters: 800,
        startTime: '2027-11-30T18:30:00.000Z',
        endTime: '2027-11-30T19:00:00.000Z',
      });

    expect(response.status).toBe(200);
    expect(response.body.transportationSegment).toMatchObject({
      mode: 'cycling', startLocation: 'MOA', endLocation: 'Main Mall',
    });
  });

  test.each([
    [[]],
    [{ unknown: true }],
    [{ mode: 1 }],
    [{ mode: 'flying' }],
    [{ startLocation: '' }],
    [{ endLocation: 'a'.repeat(501) }],
    [{ distanceMeters: 'far' }],
    [{ distanceMeters: -1 }],
    [{ startTime: 'bad' }],
    [{ endTime: 'bad' }],
    [{ startTime: '2027-11-30T20:00:00.000Z', endTime: '2027-11-30T19:00:00.000Z' }],
  ])('rejects malformed transportation update %#', async (body) => {
    // Input: malformed segment update. Expect: 400 INVALID_ITINERARY_ITEM.
    // Behavior: validation rejects it before the real database changes.
    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments/${SEGMENT_ID}`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INVALID_ITINERARY_ITEM');
  });
});

describe('DELETE /api/v1/users/:userId/itineraries/:itineraryId/transportation-segments/:segmentId — no mocks', () => {
  test('removes the transportation segment from the stored itinerary', async () => {
    // Input: existing segment ID. Expect: 204 and the embedded segment removed.
    // Behavior: the real DAO pulls the matching transportation item from MongoDB.
    const response = await request(app).delete(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments/${SEGMENT_ID}`,
    );
    expect(response.status).toBe(204);
    await expect(database.collection('itineraries').findOne({
      _id: ITINERARY_ID, 'items.id': SEGMENT_ID,
    })).resolves.toBeNull();
  });

  test('returns item not found when deleting the wrong item type', async () => {
    // Input: activity ID on transportation route. Expect: 404 item-not-found.
    // Behavior: the real stored item fails the service type check.
    const response = await request(app).delete(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments/${ACTIVITY_ID}`,
    );

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_ITEM_NOT_FOUND');
  });
});
