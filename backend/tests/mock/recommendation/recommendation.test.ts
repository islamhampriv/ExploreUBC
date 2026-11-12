import supertest from 'supertest';
import type { Db } from 'mongodb';

import { createApp } from '../../../src/index';
import type { Itinerary } from '../../../src/itineraries/itineraryTypes';
import { RecommendationDao } from '../../../src/recommendation/recommendationDao';
import type { RecommendationRepository } from '../../../src/recommendation/recommendationRepository';
import { SoclaasClient } from '../../../src/recommendation/soclaasClient';
import {
  cleanTextForRanking,
  UniversalSentenceEncoderItineraryRanker,
} from '../../../src/recommendation/universalSentenceEncoderRanker';
import type {
  GeneratedItineraryInput,
  ItineraryGenerator,
  ItineraryRanker,
  RecommendationProfile,
  RecommendedItineraryInput,
} from '../../../src/recommendation/recommendationTypes';
import type { UserRepository } from '../../../src/users/userRepository';
import type {
  User,
  UserPreferences,
  UserUpdate,
} from '../../../src/users/userTypes';

const USER_ID = '6705f0000000000000000001';
const PREFERENCES: UserPreferences = {
  transportation: { mode: 'transit' },
  budget: 'balanced',
  interest: 'art',
};

function generatedJson(count = 4): string {
  return JSON.stringify({
    itineraries: Array.from({ length: count }, (_, index) => ({
      name: `Generated itinerary ${index + 1}`,
      description: 'A generated day around UBC.',
      startingLocation: 'Untrusted LLM starting location',
      items: [
        {
          type: 'activity',
          name: `Activity ${index + 1}`,
          image: '',
          category: 'art',
          location: 'Museum of Anthropology at UBC',
          startTime: `2026-11-0${index + 1}T10:00:00.000Z`,
          endTime: `2026-11-0${index + 1}T11:00:00.000Z`,
        },
        {
          type: 'transportation',
          mode: 'transit',
          startLocation: 'Museum of Anthropology at UBC',
          endLocation: 'UBC Bus Exchange',
          distanceMeters: 1800,
          startTime: `2026-11-0${index + 1}T11:00:00.000Z`,
          endTime: `2026-11-0${index + 1}T11:20:00.000Z`,
        },
      ],
    })),
  });
}

class TestUserRepository implements UserRepository {
  constructor(private readonly startLocation = 'UBC Bus Exchange') {}

  async findUser(userId: string): Promise<User | null> {
    if (userId !== USER_ID) return null;
    return {
      googleSubject: 'google-subject',
      email: 'student@example.com',
      displayName: 'Student',
      start_location: this.startLocation,
      preferences: PREFERENCES,
      create_at: new Date('2026-10-01T00:00:00.000Z'),
      updated_at: new Date('2026-10-01T00:00:00.000Z'),
    };
  }

  async findPreferences(userId: string): Promise<UserPreferences | null> {
    return userId === USER_ID ? PREFERENCES : null;
  }

  async updateUser(
    _userId: string,
    _update: UserUpdate,
  ): Promise<boolean> {
    return false;
  }
}

class TestRecommendationRepository implements RecommendationRepository {
  replacementCalls = 0;
  itineraries: Itinerary[] = [];

  async findLatestRecommendations(userId: string): Promise<Itinerary[]> {
    return userId === USER_ID ? this.itineraries : [];
  }

  async replaceLatestRecommendations(
    userId: string,
    inputs: RecommendedItineraryInput[],
  ): Promise<Itinerary[]> {
    this.replacementCalls += 1;
    this.itineraries = inputs.map((input, index) => ({
      ...input,
      id: `6705f100000000000000000${index + 1}`,
      ownerId: userId,
      items: input.items.map((item, itemIndex) => ({
        ...item,
        id: `item-${index}-${itemIndex}`,
      })),
      createdAt: '2026-10-25T12:00:00.000Z',
      updatedAt: '2026-10-25T12:00:00.000Z',
      saved: false,
      inRecommendation: true,
    }));
    return this.itineraries;
  }
}

class TestGenerator implements ItineraryGenerator {
  receivedProfile: RecommendationProfile | undefined;

  constructor(private readonly output: string) {}

  async generateItineraries(profile: RecommendationProfile): Promise<string> {
    this.receivedProfile = profile;
    return this.output;
  }
}

class TestRanker implements ItineraryRanker {
  receivedInterest: string | undefined;

  async rankItineraries(
    itineraries: GeneratedItineraryInput[],
    interest: UserPreferences['interest'],
  ): Promise<RecommendedItineraryInput[]> {
    this.receivedInterest = interest;
    const scores = [0.25, 0.9, 0.5, 0.75];
    return itineraries.map((itinerary, index) => {
      const interestSimilarity = scores[index] ?? 0;
      return {
        ...itinerary,
        score: {
          interestSimilarity,
          interestSimilarityOutOfFive:
            3.5 + ((interestSimilarity - 0.25) / 0.65) * 1.5,
        },
      };
    });
  }
}

function buildApp(
  generator: ItineraryGenerator,
  repository = new TestRecommendationRepository(),
  userRepository: UserRepository = new TestUserRepository(),
  ranker = new TestRanker(),
) {
  return {
    app: createApp({
      recommendationGenerator: generator,
      recommendationRanker: ranker,
      recommendationRepository: repository,
      userRepository,
    }),
    repository,
    ranker,
  };
}

describe('Mocked: POST /api/v1/users/:userId/recommendations', () => {
  test('loads the profile and returns generated and fetched results by score', async () => {
    const generator = new TestGenerator(generatedJson());
    const { app, ranker } = buildApp(generator);

    const response = await supertest(app)
      .post(`/api/v1/users/${USER_ID}/recommendations`)
      .send({});

    expect(response.status).toBe(201);
    expect(generator.receivedProfile).toEqual({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    });
    expect(ranker.receivedInterest).toBe('art');
    expect(response.body.itineraries).toHaveLength(4);
    expect(response.body.itineraries.map((itinerary: Itinerary) => itinerary.name))
      .toEqual([
        'Generated itinerary 2',
        'Generated itinerary 4',
        'Generated itinerary 3',
        'Generated itinerary 1',
      ]);
    expect(response.body.itineraries[0]).toMatchObject({
      ownerId: USER_ID,
      startingLocation: 'UBC Bus Exchange',
      saved: false,
      inRecommendation: true,
      score: {
        interestSimilarity: 0.9,
        interestSimilarityOutOfFive: 5,
      },
    });
  });

  test('keeps existing recommendations when the LLM returns fewer than four', async () => {
    const repository = new TestRecommendationRepository();
    const { app } = buildApp(new TestGenerator(generatedJson(3)), repository);

    const response = await supertest(app)
      .post(`/api/v1/users/${USER_ID}/recommendations`)
      .send({});

    expect(response.status).toBe(502);
    expect(response.body.error.code).toBe('RECOMMENDATION_GENERATION_FAILED');
    expect(repository.replacementCalls).toBe(0);
  });

  test('rejects generation when the profile has no starting location', async () => {
    const generator = new TestGenerator(generatedJson());
    const { app } = buildApp(
      generator,
      new TestRecommendationRepository(),
      new TestUserRepository(''),
    );

    const response = await supertest(app)
      .post(`/api/v1/users/${USER_ID}/recommendations`)
      .send({});

    expect(response.status).toBe(400);
    expect(response.body.error.code).toBe('INCOMPLETE_PROFILE');
    expect(generator.receivedProfile).toBeUndefined();
  });

  test('returns not found when the route user does not exist', async () => {
    const { app } = buildApp(new TestGenerator(generatedJson()));
    const response = await supertest(app)
      .post('/api/v1/users/6705f0000000000000000099/recommendations')
      .send({});

    expect(response.status).toBe(404);
    expect(response.body.error.code).toBe('USER_NOT_FOUND');
  });
});

describe('Mocked: GET /api/v1/users/:userId/recommendations', () => {
  test('returns the current recommendations sorted by score', async () => {
    const repository = new TestRecommendationRepository();
    const scoredRecommendations: Array<[string, number]> = [
      ['Low', 0.25],
      ['High', 0.9],
      ['Medium', 0.5],
    ];
    repository.itineraries = scoredRecommendations.map(
      ([name, interestSimilarity], index) => ({
        id: `recommendation-${index}`,
        ownerId: USER_ID,
        name,
        items: [],
        createdAt: '2026-11-01T00:00:00.000Z',
        updatedAt: '2026-11-01T00:00:00.000Z',
        saved: false,
        inRecommendation: true,
        score: { interestSimilarity },
      }),
    );
    const { app } = buildApp(new TestGenerator(generatedJson()), repository);

    const response = await supertest(app)
      .get(`/api/v1/users/${USER_ID}/recommendations`);

    expect(response.status).toBe(200);
    expect(response.body.itineraries.map((itinerary: Itinerary) => itinerary.name))
      .toEqual(['High', 'Medium', 'Low']);
  });

  test('returns an empty list when the repository has no recommendations', async () => {
    const { app } = buildApp(new TestGenerator(generatedJson()));

    const response = await supertest(app)
      .get('/api/v1/users/6705f0000000000000000099/recommendations');

    expect(response.status).toBe(200);
    expect(response.body).toEqual({ itineraries: [] });
  });

  test('maps unexpected repository failures to an internal error', async () => {
    const repository = new TestRecommendationRepository();
    repository.findLatestRecommendations = async () => {
      throw new Error('database unavailable');
    };
    const { app } = buildApp(new TestGenerator(generatedJson()), repository);

    const response = await supertest(app)
      .get(`/api/v1/users/${USER_ID}/recommendations`);

    expect(response.status).toBe(500);
    expect(response.body.error.code).toBe('INTERNAL_SERVER_ERROR');
  });
});

describe('Universal Sentence Encoder itinerary ranking', () => {
  test('removes punctuation and stop words before vector lookup', () => {
    expect(cleanTextForRanking('The museum, and ART! at markets.'))
      .toBe('museum art markets');
  });

  test('scores descriptions by cosine similarity and sorts descending', async () => {
    const embeddingValues: Record<string, number[]> = {
      art: [1, 0],
      gallery: [0.9, 0.1],
      painting: [0.8, 0.2],
      hiking: [0, 1],
    };
    const dispose = jest.fn();
    const ranker = new UniversalSentenceEncoderItineraryRanker(async () => ({
      embed: async (texts: string[]) => ({
        array: async () => texts.map((text) =>
          embeddingValues[text] ?? embeddingValues[text.split(' ')[0]!] ?? [0, 0],
        ),
        dispose,
      }),
    }));
    const candidates: GeneratedItineraryInput[] = [
      { name: 'Hiking', description: 'The hiking trail.', items: [] },
      { name: 'Gallery', description: 'A gallery!', items: [] },
      { name: 'Unknown', description: 'Unmapped vocabulary.', items: [] },
      { name: 'Painting', description: 'Painting and the arts.', items: [] },
    ];

    const ranked = await ranker.rankItineraries(candidates, 'art');

    expect(ranked.map((itinerary) => itinerary.name)).toEqual([
      'Gallery',
      'Painting',
      'Hiking',
      'Unknown',
    ]);
    expect(ranked[0]?.score.interestSimilarity).toBeCloseTo(0.993884, 6);
    expect(ranked[0]?.score.interestSimilarityOutOfFive).toBe(5);
    expect(ranked[1]?.score.interestSimilarity).toBeCloseTo(0.970143, 6);
    expect(ranked[1]?.score.interestSimilarityOutOfFive).toBeCloseTo(4.964169, 6);
    expect(ranked[2]?.score.interestSimilarity).toBe(0);
    expect(ranked[2]?.score.interestSimilarityOutOfFive).toBe(3.5);
    expect(ranked[3]?.score.interestSimilarity).toBe(0);
    expect(ranked[3]?.score.interestSimilarityOutOfFive).toBe(3.5);
    expect(dispose).toHaveBeenCalledTimes(1);
  });

  test('handles empty descriptions, equal scores, and mismatched vectors', async () => {
    const dispose = jest.fn();
    const loadModel = jest.fn(async () => ({
      embed: async () => ({
        array: async () => [[1, 0], [1], [0, 0], [1, 0]],
        dispose,
      }),
    }));
    const ranker = new UniversalSentenceEncoderItineraryRanker(loadModel);
    const candidates: GeneratedItineraryInput[] = [
      { name: 'Missing description', items: [] },
      { name: 'Zero vector', description: 'zero', items: [] },
      { name: 'Matching vector', description: 'match', items: [] },
    ];

    const first = await ranker.rankItineraries(candidates, 'art');
    const second = await ranker.rankItineraries(candidates, 'art');

    expect(first.map((itinerary) => itinerary.score.interestSimilarity))
      .toEqual([1, 0, 0]);
    expect(first[0]?.score.interestSimilarityOutOfFive).toBe(5);
    expect(first[1]?.score.interestSimilarityOutOfFive).toBe(3.5);
    expect(second).toEqual(first);
    expect(loadModel).toHaveBeenCalledTimes(1);
    expect(dispose).toHaveBeenCalledTimes(2);
  });

  test.each([
    [new Error('model download failed'), 'model download failed'],
    ['non-error rejection', 'Could not load the sentence encoder model.'],
  ])('maps model loader failure %# to a generation error', async (failure, message) => {
    const ranker = new UniversalSentenceEncoderItineraryRanker(
      async () => Promise.reject(failure),
    );

    await expect(ranker.rankItineraries([], 'art'))
      .rejects.toThrow(message);
  });
});

describe('Mocked: SoCLaaS Responses API client', () => {
  function clientWith(fetchFunction: jest.Mock) {
    return new SoclaasClient({
      endpoint: 'https://soclaas-api.comp.nus.edu.sg/v1',
      apiKey: 'test-key',
      model: 'qwen3.5:9b',
      maxOutputTokens: 16_384,
    }, fetchFunction);
  }

  test('posts the profile to /v1/responses and reads nested output text', async () => {
    const fetchMock = jest.fn(async (
      _input: string | URL,
      _init?: RequestInit,
    ) => new Response(JSON.stringify({
      status: 'completed',
      output: [{
        content: [{ type: 'output_text', text: generatedJson() }],
      }],
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    const client = new SoclaasClient({
      endpoint: 'https://soclaas-api.comp.nus.edu.sg/v1/',
      apiKey: 'test-key',
      model: 'qwen3.5:9b',
      maxOutputTokens: 16_384,
    }, fetchMock);

    const output = await client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    });

    expect(output).toBe(generatedJson());
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, request] = fetchMock.mock.calls[0]!;
    expect(url).toBe('https://soclaas-api.comp.nus.edu.sg/v1/responses');
    expect(request?.method).toBe('POST');
    expect(request?.headers).toMatchObject({
      Authorization: 'Bearer test-key',
      'Content-Type': 'application/json',
    });
    expect(JSON.parse(request?.body as string)).toMatchObject({
      model: 'qwen3.5:9b',
      max_output_tokens: 16_384,
    });
    expect(request?.signal).toBeInstanceOf(AbortSignal);
  });

  test('reports when the provider marks output incomplete due to the token limit', async () => {
    const fetchMock = jest.fn(async () => new Response(JSON.stringify({
      status: 'incomplete',
      incomplete_details: { reason: 'max_output_tokens' },
      output: [],
    }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
    const client = new SoclaasClient({
      endpoint: 'https://soclaas-api.comp.nus.edu.sg/v1',
      apiKey: 'test-key',
      model: 'qwen3.5:9b',
      maxOutputTokens: 2_000,
    }, fetchMock);

    await expect(client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    })).rejects.toThrow('reached its output token limit');
  });

  test('reports provider HTTP errors without trying to parse output', async () => {
    const fetchMock = jest.fn(async () => new Response(JSON.stringify({
      error: { message: 'Model is unavailable.' },
    }), {
      status: 503,
      headers: { 'Content-Type': 'application/json' },
    }));
    const client = new SoclaasClient({
      endpoint: 'https://soclaas-api.comp.nus.edu.sg/v1',
      apiKey: 'test-key',
      model: 'qwen3.5:9b',
      maxOutputTokens: 16_384,
    }, fetchMock);

    await expect(client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    })).rejects.toThrow('returned 503: Model is unavailable.');
  });

  test('reads top-level output text', async () => {
    const client = clientWith(jest.fn(async () => new Response(JSON.stringify({
      status: 'completed',
      output_text: generatedJson(),
    }), { status: 200 })));

    await expect(client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    })).resolves.toBe(generatedJson());
  });

  test('reports incomplete output for provider reasons other than token limits', async () => {
    const client = clientWith(jest.fn(async () => new Response(JSON.stringify({
      status: 'incomplete',
      incomplete_details: { reason: 'content_filter' },
    }), { status: 200 })));

    await expect(client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    })).rejects.toThrow('returned an incomplete response');
  });

  test.each([
    [{ status: 'completed' }, 'no output text'],
    [{ status: 'completed', output_text: ' ', output: [{}] }, 'no output text'],
    [{
      status: 'completed',
      output: [{ content: [{ type: 'refusal', text: 'No' }] }],
    }, 'no output text'],
  ])('rejects responses without usable output text %#', async (payload, message) => {
    const client = clientWith(jest.fn(async () => new Response(
      JSON.stringify(payload),
      { status: 200 },
    )));

    await expect(client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    })).rejects.toThrow(message);
  });

  test.each([
    [new Error('offline'), 'offline'],
    ['offline', 'itinerary generation service is unavailable'],
  ])('wraps fetch rejection %#', async (failure, message) => {
    const fetchMock = jest.fn(async () => Promise.reject(failure));
    const client = clientWith(fetchMock);

    await expect(client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    })).rejects.toThrow(message);
  });

  test('reports successful responses containing invalid JSON', async () => {
    const client = clientWith(jest.fn(async () => new Response(
      'not-json',
      { status: 200 },
    )));

    await expect(client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    })).rejects.toThrow('returned invalid JSON');
  });

  test.each([
    [JSON.stringify({ error: 'Overloaded' }), 'returned 429: Overloaded'],
    ['not-json', 'returned 500.'],
  ])('reports alternate HTTP error payload %#', async (body, message) => {
    const status = body === 'not-json' ? 500 : 429;
    const client = clientWith(jest.fn(async () => new Response(body, { status })));

    await expect(client.generateItineraries({
      start_location: 'UBC Bus Exchange',
      preferences: PREFERENCES,
    })).rejects.toThrow(message);
  });
});

describe('Mocked: RecommendationDao publication', () => {
  function createDao(options: {
    deleteFailure?: Error;
    matchedCount?: number;
  } = {}) {
    const findCursor = {
      sort: jest.fn().mockReturnThis(),
      limit: jest.fn().mockReturnThis(),
      toArray: jest.fn().mockResolvedValue([]),
    };
    const deleteMany = options.deleteFailure === undefined
      ? jest.fn().mockResolvedValue({ deletedCount: 0 })
      : jest.fn()
        .mockRejectedValueOnce(options.deleteFailure)
        .mockResolvedValueOnce({ deletedCount: 4 });
    const collection = {
      find: jest.fn().mockReturnValue(findCursor),
      insertMany: jest.fn().mockResolvedValue({ insertedCount: 4 }),
      deleteMany,
      updateMany: jest.fn().mockResolvedValue({
        matchedCount: options.matchedCount ?? 4,
      }),
    };
    const database = {
      collection: jest.fn().mockReturnValue(collection),
    } as unknown as Db;
    return { dao: new RecommendationDao(database), collection };
  }

  const inputs: RecommendedItineraryInput[] = Array.from(
    { length: 4 },
    (_, index) => ({
      name: `Candidate ${index + 1}`,
      description: `Description ${index + 1}`,
      startingLocation: 'UBC Bus Exchange',
      items: [],
      score: { interestSimilarity: index / 10 },
    }),
  );

  test('cleans up inserted candidates when deleting old recommendations fails', async () => {
    const failure = new Error('delete failed');
    const { dao, collection } = createDao({ deleteFailure: failure });

    await expect(dao.replaceLatestRecommendations(USER_ID, inputs))
      .rejects.toBe(failure);
    expect(collection.deleteMany).toHaveBeenCalledTimes(2);
  });

  test('cleans up when not every candidate can be published', async () => {
    const { dao, collection } = createDao({ matchedCount: 3 });

    await expect(dao.replaceLatestRecommendations(USER_ID, inputs))
      .rejects.toThrow('Not all generated recommendations could be published.');
    expect(collection.deleteMany).toHaveBeenCalledTimes(2);
  });
});
