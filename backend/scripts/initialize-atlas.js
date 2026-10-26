const { MongoClient } = require('mongodb');
const { seedTestData } = require('./seed-test-data');

const userValidator = {
  $jsonSchema: {
    bsonType: 'object',
    title: 'ExploreUBC user',
    additionalProperties: false,
    required: [
      'googleSubject',
      'email',
      'displayName',
      'start_location',
      'preferences',
      'create_at',
      'updated_at',
    ],
    properties: {
      _id: { bsonType: 'objectId' },
      googleSubject: { bsonType: 'string' },
      email: { bsonType: 'string' },
      displayName: { bsonType: 'string' },
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
      create_at: { bsonType: 'date' },
      updated_at: { bsonType: 'date' },
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
  required: ['id', 'type', 'name', 'image', 'location', 'startTime', 'endTime'],
  properties: {
    id: { bsonType: 'string' },
    type: { enum: ['activity'] },
    name: { bsonType: 'string' },
    image: { bsonType: 'string' },
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

    await seedTestData(database);
    console.log('Atlas database schema and indexes are ready.');
  } finally {
    await client.close();
  }
}

initializeDatabase().catch((error) => {
  console.error('Atlas database initialization failed:', error);
  process.exitCode = 1;
});
