const { MongoClient, ObjectId } = require('mongodb');

const userValidator = {
  $jsonSchema: {
    bsonType: 'object',
    title: 'ExploreUBC user',
    additionalProperties: false,
    required: [
      'googleSubject',
      'email',
      'displayName',
      'profileImageUrl',
      'start_location',
      'preferences',
      'onboardingCompletedAt',
      'create_at',
      'updated_at',
    ],
    properties: {
      _id: { bsonType: 'objectId' },
      googleSubject: { bsonType: 'string' },
      email: { bsonType: 'string' },
      displayName: { bsonType: 'string' },
      profileImageUrl: { bsonType: ['string', 'null'] },
      start_location: { bsonType: 'string' },
      preferences: {
        bsonType: 'object',
        additionalProperties: false,
        required: ['transportation', 'budget', 'interest'],
        properties: {
          transportation: {
            bsonType: 'object',
            additionalProperties: false,
            required: ['mode'],
            properties: {
              mode: {
                enum: ['walking', 'cycling', 'transit', 'driving', 'other'],
              },
            },
          },
          budget: { enum: ['economical', 'balanced', 'premium'] },
          interest: {
            enum: ['food', 'art', 'outdoors', 'markets', 'live_music', 'gaming'],
          },
        },
      },
      onboardingCompletedAt: { bsonType: ['date', 'null'] },
      create_at: { bsonType: 'date' },
      updated_at: { bsonType: 'date' },
    },
  },
};

const sessionValidator = {
  $jsonSchema: {
    bsonType: 'object',
    title: 'ExploreUBC session',
    additionalProperties: false,
    required: ['_id', 'userId', 'tokenHash', 'createdAt', 'expiresAt'],
    properties: {
      _id: { bsonType: 'objectId' },
      userId: { bsonType: 'objectId' },
      tokenHash: { bsonType: 'string' },
      createdAt: { bsonType: 'date' },
      expiresAt: { bsonType: 'date' },
    },
  },
};

const transportationModes = [
  'walking',
  'cycling',
  'transit',
  'driving',
  'other',
];
const numberTypes = ['double', 'int', 'long', 'decimal'];

const activityItemSchema = {
  bsonType: 'object',
  additionalProperties: false,
  required: ['id', 'type', 'name', 'location', 'startTime', 'endTime'],
  properties: {
    id: { bsonType: 'string' },
    type: { enum: ['activity'] },
    name: { bsonType: 'string' },
    description: { bsonType: 'string' },
    category: { bsonType: 'string' },
    location: { bsonType: 'string' },
    startTime: { bsonType: 'string' },
    endTime: { bsonType: 'string' },
  },
};

const transportationItemSchema = {
  bsonType: 'object',
  additionalProperties: false,
  required: [
    'id',
    'type',
    'mode',
    'startLocation',
    'endLocation',
    'distanceMeters',
    'startTime',
    'endTime',
  ],
  properties: {
    id: { bsonType: 'string' },
    type: { enum: ['transportation'] },
    mode: { enum: transportationModes },
    startLocation: { bsonType: 'string' },
    endLocation: { bsonType: 'string' },
    distanceMeters: { bsonType: numberTypes, minimum: 0 },
    startTime: { bsonType: 'string' },
    endTime: { bsonType: 'string' },
  },
};

const itineraryValidator = {
  $jsonSchema: {
    bsonType: 'object',
    title: 'ExploreUBC itinerary',
    additionalProperties: false,
    required: [
      '_id',
      'ownerId',
      'name',
      'items',
      'createdAt',
      'updatedAt',
      'saved',
      'inRecommendation',
      'score',
    ],
    properties: {
      _id: { bsonType: 'objectId' },
      ownerId: { bsonType: 'objectId' },
      name: { bsonType: 'string' },
      description: { bsonType: 'string' },
      startingLocation: { bsonType: 'string' },
      items: {
        bsonType: 'array',
        items: { oneOf: [activityItemSchema, transportationItemSchema] },
      },
      createdAt: { bsonType: 'date' },
      updatedAt: { bsonType: 'date' },
      saved: { bsonType: 'bool' },
      inRecommendation: { bsonType: 'bool' },
      score: {
        bsonType: 'object',
        additionalProperties: { bsonType: numberTypes },
      },
    },
  },
};

async function ensureCollection(database, name, validator, indexes) {
  const validationOptions = {
    validator,
    validationLevel: 'strict',
    validationAction: 'error',
  };

  try {
    await database.createCollection(name, validationOptions);
  } catch (error) {
    if (
      typeof error !== 'object' ||
      error === null ||
      !('code' in error) ||
      error.code !== 48
    ) {
      throw error;
    }

    await database.command({ collMod: name, ...validationOptions });
  }

  const collection = database.collection(name);
  for (const [keys, options] of indexes) {
    await collection.createIndex(keys, options);
  }
}

// Keep this idempotent seed in sync with database/init/003-insert-user-itineraries.js.
async function seedUserAndItinerary(database) {
  const testUserId = new ObjectId('6acd519c76376cefc61e97f1');
  const testItineraryId = new ObjectId('6705f1000000000000000001');
  const now = new Date();

  await database.collection('users').replaceOne(
    { _id: testUserId },
    {
      _id: testUserId,
      googleSubject: 'dev-seed-user-google-subject',
      email: 'test.user@student.ubc.ca',
      displayName: 'Test User',
      profileImageUrl: null,
      start_location: 'UBC Bus Exchange',
      preferences: {
        transportation: { mode: 'transit' },
        budget: 'balanced',
        interest: 'outdoors',
      },
      onboardingCompletedAt: now,
      create_at: now,
      updated_at: now,
    },
    { upsert: true },
  );

  await database.collection('itineraries').replaceOne(
    { _id: testItineraryId },
    {
      _id: testItineraryId,
      ownerId: testUserId,
      name: 'UBC Afternoon',
      description: 'A local test itinerary with an activity and transit segment.',
      startingLocation: 'UBC Bus Exchange',
      items: [
        {
          id: '11111111-1111-4111-8111-111111111111',
          type: 'activity',
          name: 'Visit the Museum of Anthropology',
          description: 'Explore the museum collections.',
          category: 'art',
          location: 'Museum of Anthropology at UBC',
          startTime: '2026-10-12T20:00:00.000Z',
          endTime: '2026-10-12T21:30:00.000Z',
        },
        {
          id: '22222222-2222-4222-8222-222222222222',
          type: 'transportation',
          mode: 'transit',
          startLocation: 'Museum of Anthropology at UBC',
          endLocation: 'UBC Bus Exchange',
          distanceMeters: 1800,
          startTime: '2026-10-12T21:35:00.000Z',
          endTime: '2026-10-12T21:50:00.000Z',
        },
      ],
      createdAt: now,
      updatedAt: now,
      saved: true,
      inRecommendation: false,
      score: { overall: 0.9, preferenceMatch: 0.85 },
    },
    { upsert: true },
  );
}

async function initializeDatabase() {
  const uri = process.env.MONGODB_URI?.trim();
  if (!uri) {
    throw new Error('MONGODB_URI is required to initialize the database.');
  }

  const client = new MongoClient(uri);
  try {
    await client.connect();
    const database = client.db('cpen321');

    await ensureCollection(database, 'users', userValidator, [
      [{ googleSubject: 1 }, { unique: true, name: 'unique_google_subject' }],
    ]);
    await ensureCollection(database, 'itineraries', itineraryValidator, [
      [{ ownerId: 1, updatedAt: -1 }, { name: 'owner_updated_at' }],
    ]);
    await database.collection('itineraries').updateMany(
      { 'items.image': { $exists: true } },
      { $unset: { 'items.$[].image': '' } },
    );
    await ensureCollection(database, 'sessions', sessionValidator, [
      [
        { tokenHash: 1 },
        { unique: true, name: 'unique_session_token_hash' },
      ],
      [
        { expiresAt: 1 },
        { expireAfterSeconds: 0, name: 'expire_sessions' },
      ],
      [{ userId: 1 }, { name: 'sessions_by_user' }],
    ]);

    const itineraryMaps = database.collection('itineraryMaps');
    await itineraryMaps.createIndex(
      { itineraryId: 1 },
      { unique: true, name: 'unique_itinerary_map' },
    );

    const liveEvents = database.collection('liveevents');
    await liveEvents.createIndex(
      { externalId: 1 },
      { unique: true, name: 'unique_external_event' },
    );
    await liveEvents.createIndex({ startDateTime: 1 });
    await liveEvents.createIndex({ category: 1, startDateTime: 1 });
    await liveEvents.createIndex({ location: '2dsphere' });

    await seedUserAndItinerary(database);

    console.log('Database schema, indexes, and seed data are ready.');
  } finally {
    await client.close();
  }
}

initializeDatabase().catch((error) => {
  console.error('Atlas database initialization failed:', error);
  process.exitCode = 1;
});
