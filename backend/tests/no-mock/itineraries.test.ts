import request from 'supertest';

import { createApp } from '../../src/app';
import type { ItineraryRepository } from '../../src/itineraries/itineraryRepository';
import type {
  ActivityItem,
  CreateActivityInput,
  CreateItineraryInput,
  CreateTransportationInput,
  Itinerary,
  ItineraryDetails,
  ItineraryDetailsUpdate,
  ItineraryFilters,
  ItineraryItem,
  TransportationItem,
} from '../../src/itineraries/itineraryTypes';

const USER_ID = '6705f0000000000000000001';
const ITINERARY_ID = '6705f1000000000000000001';

const ACTIVITY_INPUT: CreateActivityInput = {
  type: 'activity',
  name: 'Nitobe Memorial Garden',
  image: 'https://example.com/nitobe.jpg',
  description: 'Walk through the Japanese garden.',
  category: 'outdoors',
  location: '1895 Lower Mall, Vancouver, BC',
  startTime: '2026-10-17T21:00:00.000Z',
  endTime: '2026-10-17T22:00:00.000Z',
};

const TRANSPORTATION_INPUT: CreateTransportationInput = {
  type: 'transportation',
  mode: 'walking',
  startLocation: '6804 SW Marine Drive, Vancouver, BC',
  endLocation: '1895 Lower Mall, Vancouver, BC',
  distanceMeters: 3100,
  startTime: '2026-10-17T20:30:00.000Z',
  endTime: '2026-10-17T21:00:00.000Z',
};

const COMPLETE_ITINERARY: CreateItineraryInput = {
  ownerId: USER_ID,
  name: 'UBC Saturday itinerary',
  description: 'Two activities with transportation between them.',
  startingLocation: '6133 University Boulevard, Vancouver, BC',
  items: [
    {
      type: 'activity',
      name: 'Museum of Anthropology',
      image: 'https://example.com/moa.jpg',
      description: 'Explore the museum collections.',
      category: 'art',
      location: '6393 NW Marine Drive, Vancouver, BC',
      startTime: '2026-10-17T17:00:00.000Z',
      endTime: '2026-10-17T18:30:00.000Z',
    },
    {
      type: 'transportation',
      mode: 'walking',
      startLocation: '6393 NW Marine Drive, Vancouver, BC',
      endLocation: '6804 SW Marine Drive, Vancouver, BC',
      distanceMeters: 2400,
      startTime: '2026-10-17T18:30:00.000Z',
      endTime: '2026-10-17T19:05:00.000Z',
    },
    {
      type: 'activity',
      name: 'UBC Botanical Garden',
      image: 'https://example.com/garden.jpg',
      category: 'outdoors',
      location: '6804 SW Marine Drive, Vancouver, BC',
      startTime: '2026-10-17T19:05:00.000Z',
      endTime: '2026-10-17T20:30:00.000Z',
    },
  ],
  saved: true,
  inRecommendation: false,
  score: { relevance: 0.9 },
};

class TestItineraryRepository implements ItineraryRepository {
  insertedUserId: string | undefined;
  insertedItinerary: CreateItineraryInput | undefined;
  private storedItinerary: Itinerary | null = this.buildItinerary(
    COMPLETE_ITINERARY,
  );

  private buildItinerary(itinerary: CreateItineraryInput): Itinerary {
    return {
      ...itinerary,
      id: ITINERARY_ID,
      items: itinerary.items.map((item, index) => ({
        ...item,
        id: `generated-item-${index + 1}`,
      })),
      createdAt: '2026-10-12T12:00:00.000Z',
      updatedAt: '2026-10-12T12:00:00.000Z',
    };
  }

  async insertItinerary(
    userId: string,
    itinerary: CreateItineraryInput,
  ): Promise<Itinerary> {
    this.insertedUserId = userId;
    this.insertedItinerary = itinerary;
    this.storedItinerary = this.buildItinerary(itinerary);
    return this.storedItinerary;
  }

  async findItineraryDetails(
    userId: string,
    itineraryId: string,
  ): Promise<ItineraryDetails | null> {
    const itinerary = await this.findItinerary(userId, itineraryId);
    if (itinerary === null) {
      return null;
    }
    return {
      name: itinerary.name,
      ...(itinerary.description === undefined
        ? {}
        : { description: itinerary.description }),
      ...(itinerary.startingLocation === undefined
        ? {}
        : { startingLocation: itinerary.startingLocation }),
    };
  }

  async findItinerary(
    userId: string,
    itineraryId: string,
  ): Promise<Itinerary | null> {
    return userId === USER_ID &&
      itineraryId === ITINERARY_ID &&
      this.storedItinerary !== null
      ? this.storedItinerary
      : null;
  }

  async findItineraries(
    userId: string,
    filters: ItineraryFilters,
  ): Promise<Itinerary[]> {
    if (userId !== USER_ID || this.storedItinerary === null) return [];
    if (
      filters.saved !== undefined &&
      this.storedItinerary.saved !== filters.saved
    ) return [];
    if (
      filters.inRecommendation !== undefined &&
      this.storedItinerary.inRecommendation !== filters.inRecommendation
    ) return [];
    return [this.storedItinerary];
  }

  async updateItineraryDetails(
    userId: string,
    itineraryId: string,
    details: ItineraryDetailsUpdate,
  ): Promise<boolean> {
    const itinerary = await this.findItinerary(userId, itineraryId);
    if (itinerary === null) {
      return false;
    }
    this.storedItinerary = {
      ...itinerary,
      updatedAt: '2026-10-12T13:00:00.000Z',
      ...(details.name === undefined ? {} : { name: details.name }),
      ...(details.description === undefined
        ? {}
        : { description: details.description }),
      ...(details.startingLocation === undefined
        ? {}
        : { startingLocation: details.startingLocation }),
    };
    return true;
  }

  async insertEmbeddedActivity(
    userId: string,
    itineraryId: string,
    activity: ActivityItem,
  ): Promise<boolean> {
    return this.insertEmbeddedItem(userId, itineraryId, activity);
  }

  async insertEmbeddedTransportationSegment(
    userId: string,
    itineraryId: string,
    segment: TransportationItem,
  ): Promise<boolean> {
    return this.insertEmbeddedItem(userId, itineraryId, segment);
  }

  async removeItinerary(
    userId: string,
    itineraryId: string,
  ): Promise<boolean> {
    if ((await this.findItinerary(userId, itineraryId)) === null) {
      return false;
    }
    this.storedItinerary = null;
    return true;
  }

  private async insertEmbeddedItem(
    userId: string,
    itineraryId: string,
    item: ItineraryItem,
  ): Promise<boolean> {
    const itinerary = await this.findItinerary(userId, itineraryId);
    if (itinerary === null || !itinerary.saved) {
      return false;
    }
    this.storedItinerary = {
      ...itinerary,
      items: [...itinerary.items, item],
      updatedAt: '2026-10-12T14:00:00.000Z',
    };
    return true;
  }
}

function createTestApp(repository = new TestItineraryRepository()) {
  return {
    app: createApp({ itineraryRepository: repository }),
    repository,
  };
}

describe('Unmocked: POST /api/v1/users/:userId/itineraries', () => {
  test('creates an entire itinerary including activities and transportation', async () => {
    const { app, repository } = createTestApp();

    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries`)
      .send(COMPLETE_ITINERARY);

    expect(response.status).toBe(201);
    expect(response.headers.location).toBe(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`,
    );
    expect(repository.insertedUserId).toBe(USER_ID);
    expect(repository.insertedItinerary).toEqual(COMPLETE_ITINERARY);
    expect(response.body.itinerary).toEqual({
      ...COMPLETE_ITINERARY,
      id: ITINERARY_ID,
      items: COMPLETE_ITINERARY.items.map((item, index) => ({
        ...item,
        id: `generated-item-${index + 1}`,
      })),
      createdAt: '2026-10-12T12:00:00.000Z',
      updatedAt: '2026-10-12T12:00:00.000Z',
    });
  });

  test.each([
    [
      { ...COMPLETE_ITINERARY, ownerId: '6705f0000000000000000099' },
      'ownerId must match the userId route parameter.',
    ],
    [
      { ...COMPLETE_ITINERARY, items: undefined },
      'items must be an array.',
    ],
    [
      {
        ...COMPLETE_ITINERARY,
        items: [
          {
            ...COMPLETE_ITINERARY.items[0],
            endTime: COMPLETE_ITINERARY.items[0]?.startTime,
          },
        ],
      },
      'items[0].startTime must be earlier than items[0].endTime.',
    ],
    [
      {
        ...COMPLETE_ITINERARY,
        items: [{ ...COMPLETE_ITINERARY.items[0], id: 'client-item-id' }],
      },
      "Unexpected field 'items[0].id'.",
    ],
  ])('rejects an invalid complete itinerary %#', async (body, expectedDetail) => {
    const { app } = createTestApp();

    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error).toMatchObject({
      code: 'INVALID_ITINERARY_DETAILS',
      message: 'The itinerary details are invalid.',
    });
    expect(response.body.error.details).toContain(expectedDetail);
  });
});

describe('Unmocked: GET /api/v1/users/:userId/itineraries/:itineraryId/details', () => {
  test('returns only the itinerary details', async () => {
    const { app } = createTestApp();

    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/details`,
    );

    expect(response.status).toBe(200);
    expect(response.body).toEqual({
      details: {
        name: COMPLETE_ITINERARY.name,
        description: COMPLETE_ITINERARY.description,
        startingLocation: COMPLETE_ITINERARY.startingLocation,
      },
    });
    expect(response.body.details).not.toHaveProperty('items');
  });

  test('returns 404 when the itinerary does not belong to the user', async () => {
    const { app } = createTestApp();

    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/6705f1000000000000000099/details`,
    );

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
  });
});

describe('Unmocked: add itinerary activities and transportation', () => {
  test('adds an activity with a server-generated ID', async () => {
    const { app } = createTestApp();

    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/activities`)
      .send(ACTIVITY_INPUT);

    expect(response.status).toBe(201);
    expect(response.body.activity).toMatchObject(ACTIVITY_INPUT);
    expect(response.body.activity.id).toEqual(expect.any(String));
    expect(response.body.activity.id).not.toHaveLength(0);

    const itineraryResponse = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`,
    );
    expect(itineraryResponse.body.itinerary.items).toContainEqual(
      response.body.activity,
    );
  });

  test('adds a transportation segment with a server-generated ID', async () => {
    const { app } = createTestApp();

    const response = await request(app)
      .post(
        `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/transportation-segments`,
      )
      .send(TRANSPORTATION_INPUT);

    expect(response.status).toBe(201);
    expect(response.body.transportationSegment).toMatchObject(
      TRANSPORTATION_INPUT,
    );
    expect(response.body.transportationSegment.id).toEqual(expect.any(String));

    const itineraryResponse = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`,
    );
    expect(itineraryResponse.body.itinerary.items).toContainEqual(
      response.body.transportationSegment,
    );
  });

  test.each([
    [
      'activities',
      { ...ACTIVITY_INPUT, id: 'client-generated-id' },
      "Unexpected field 'id'.",
    ],
    [
      'activities',
      { ...ACTIVITY_INPUT, endTime: ACTIVITY_INPUT.startTime },
      'startTime must be earlier than endTime.',
    ],
    [
      'transportation-segments',
      { ...TRANSPORTATION_INPUT, mode: 'flying' },
      'mode must be one of: walking, cycling, transit, driving, other.',
    ],
  ])('rejects invalid input for %s', async (path, body, expectedDetail) => {
    const { app } = createTestApp();

    const response = await request(app)
      .post(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}/${path}`)
      .send(body);

    expect(response.status).toBe(400);
    expect(response.body.error).toMatchObject({
      code: 'INVALID_ITINERARY_ITEM',
      message: 'The itinerary item is invalid.',
    });
    expect(response.body.error.details).toContain(expectedDetail);
  });

  test.each(['activities', 'transportation-segments'])(
    'does not add %s to another user\'s itinerary',
    async (path) => {
      const { app } = createTestApp();
      const body = path === 'activities' ? ACTIVITY_INPUT : TRANSPORTATION_INPUT;

      const response = await request(app)
        .post(
          `/api/v1/users/6705f0000000000000000099/itineraries/${ITINERARY_ID}/${path}`,
        )
        .send(body);

      expect(response.status).toBe(404);
      expect(response.body.error.code).toBe('ITINERARY_NOT_FOUND');
    },
  );
});

describe('Unmocked: itinerary retrieval, update, and deletion', () => {
  test('gets one complete owner-scoped itinerary', async () => {
    const { app } = createTestApp();

    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`,
    );

    expect(response.status).toBe(200);
    expect(response.body.itinerary).toMatchObject({
      id: ITINERARY_ID,
      ownerId: USER_ID,
      name: COMPLETE_ITINERARY.name,
    });
    expect(response.body.itinerary.items).toHaveLength(3);
  });

  test('gets all itineraries belonging to the user', async () => {
    const { app } = createTestApp();

    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries`,
    );

    expect(response.status).toBe(200);
    expect(response.body.itineraries).toHaveLength(1);
    expect(response.body.itineraries[0].ownerId).toBe(USER_ID);
  });

  test.each([
    ['?saved=true', 1],
    ['?saved=false', 0],
    ['?inRecommendation=false', 1],
    ['?inRecommendation=true', 0],
    ['?saved=true&inRecommendation=false', 1],
    ['?saved=false&inRecommendation=false', 0],
  ])('filters itineraries with %s', async (query, expectedCount) => {
    const { app } = createTestApp();

    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries${query}`,
    );

    expect(response.status).toBe(200);
    expect(response.body.itineraries).toHaveLength(expectedCount);
  });

  test('rejects a non-boolean itinerary filter', async () => {
    const { app } = createTestApp();

    const response = await request(app).get(
      `/api/v1/users/${USER_ID}/itineraries?saved=yes`,
    );

    expect(response.status).toBe(400);
    expect(response.body.error).toEqual({
      code: 'INVALID_ITINERARY_FILTERS',
      message: 'The itinerary filters are invalid.',
      details: ["saved must be either 'true' or 'false'."],
    });
  });

  test('updates itinerary details without replacing the items', async () => {
    const { app } = createTestApp();
    const details: ItineraryDetails = {
      name: 'Updated UBC itinerary',
      description: 'Updated description.',
      startingLocation: 'UBC Bus Exchange, Vancouver, BC',
    };

    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .send(details);

    expect(response.status).toBe(200);
    expect(response.body.itinerary).toMatchObject(details);
    expect(response.body.itinerary.items).toHaveLength(3);
    expect(response.body.itinerary.updatedAt).toBe(
      '2026-10-12T13:00:00.000Z',
    );
  });

  test('updates only the provided description and preserves omitted details', async () => {
    const { app } = createTestApp();
    const description =
      '[test patch details] Visit the Museum of Anthropology and the UBC Botanical Garden.';

    const response = await request(app)
      .patch(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .send({ description });

    expect(response.status).toBe(200);
    expect(response.body.itinerary).toMatchObject({
      name: COMPLETE_ITINERARY.name,
      description,
      startingLocation: COMPLETE_ITINERARY.startingLocation,
    });
    expect(response.body.itinerary.items).toHaveLength(3);
  });

  test('deletes an owner-scoped itinerary', async () => {
    const { app } = createTestApp();

    await request(app)
      .delete(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .expect(204);

    await request(app)
      .get(`/api/v1/users/${USER_ID}/itineraries/${ITINERARY_ID}`)
      .expect(404);
  });

  test.each(['get', 'patch', 'delete'] as const)(
    '%s does not expose another user\'s itinerary',
    async (method) => {
      const { app } = createTestApp();
      const path =
        `/api/v1/users/6705f0000000000000000099/itineraries/${ITINERARY_ID}`;
      const operation = request(app)[method](path);
      if (method === 'patch') {
        operation.send({ name: 'Unauthorized update' });
      }
      const response = await operation;
      expect(response.status).toBe(404);
    },
  );
});
