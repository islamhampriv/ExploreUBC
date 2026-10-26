import { attachDatabasePool } from '@vercel/functions';
import express, { type Express } from 'express';
import { MongoClient } from 'mongodb';

import { env } from './config/env';
import { EventDao } from './events/eventDao';
import type { EventRepository } from './events/eventRepository';
import { createEventRouter } from './events/eventRouter';
import { EventService } from './events/eventService';
import { ItineraryDao } from './itineraries/itineraryDao';
import type { ItineraryRepository } from './itineraries/itineraryRepository';
import { createItineraryRouter } from './itineraries/itineraryRouter';
import { ItineraryService } from './itineraries/itineraryService';
import { ItineraryMapDao } from './maps/itineraryMapDao';
import { RecommendationDao } from './recommendation/recommendationDao';
import type { RecommendationRepository } from './recommendation/recommendationRepository';
import { createRecommendationRouter } from './recommendation/recommendationRouter';
import { RecommendationService } from './recommendation/recommendationService';
import { SoclaasClient } from './recommendation/soclaasClient';
import type {
  ItineraryGenerator,
  ItineraryRanker,
} from './recommendation/recommendationTypes';
import { UniversalSentenceEncoderItineraryRanker } from './recommendation/universalSentenceEncoderRanker';
import { UserDao } from './users/userDao';
import type { UserRepository } from './users/userRepository';
import { createUserRouter } from './users/userRouter';
import { UserService } from './users/userService';

export type AppDependencies = {
  eventRepository?: EventRepository;
  itineraryRepository?: ItineraryRepository;
  recommendationGenerator?: ItineraryGenerator;
  recommendationRanker?: ItineraryRanker;
  recommendationRepository?: RecommendationRepository;
  userRepository?: UserRepository;
};

export function createApp(dependencies: AppDependencies = {}): Express {
  const app = express();

  app.use(express.json());

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  if (dependencies.userRepository !== undefined) {
    const userService = new UserService(dependencies.userRepository);
    app.use('/api/v1/users', createUserRouter(userService));
  }

  if (dependencies.itineraryRepository !== undefined) {
    const itineraryService = new ItineraryService(
      dependencies.itineraryRepository,
    );
    app.use(
      '/api/v1/users/:userId/itineraries',
      createItineraryRouter(itineraryService),
    );
  }

  if (
    dependencies.eventRepository !== undefined &&
    dependencies.userRepository !== undefined
  ) {
    const eventService = new EventService(
      dependencies.eventRepository,
      dependencies.userRepository,
    );
    app.use(
      '/api/v1/users/:userId/events',
      createEventRouter(eventService),
    );
  }

  if (
    dependencies.recommendationRepository !== undefined &&
    dependencies.recommendationGenerator !== undefined &&
    dependencies.recommendationRanker !== undefined &&
    dependencies.userRepository !== undefined
  ) {
    const recommendationService = new RecommendationService(
      dependencies.recommendationRepository,
      dependencies.userRepository,
      dependencies.recommendationGenerator,
      dependencies.recommendationRanker,
    );
    app.use(
      '/api/v1/users/:userId/recommendations',
      createRecommendationRouter(recommendationService),
    );
  }

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}

const mongoClient = new MongoClient(env.mongodbUri);
if (process.env.VERCEL === '1') {
  attachDatabasePool(mongoClient);
}

const database = mongoClient.db('cpen321');
const eventDao = new EventDao(database);
const userDao = new UserDao(database);
const itineraryMapDao = new ItineraryMapDao(database);
const app = createApp({
  eventRepository: eventDao,
  itineraryRepository: new ItineraryDao(database),
  recommendationGenerator: new SoclaasClient(env.soclaas),
  recommendationRanker: new UniversalSentenceEncoderItineraryRanker(),
  recommendationRepository: new RecommendationDao(database),
  userRepository: userDao,
});

async function start(): Promise<void> {
  await mongoClient.connect();
  await eventDao.createIndexes();
  await itineraryMapDao.createIndexes();

  const server = app.listen(env.port, () => {
    console.log(`Server listening on port ${env.port}`);
  });

  for (const signal of ['SIGINT', 'SIGTERM'] as const) {
    process.on(signal, () => {
      server.close(() => {
        void mongoClient.close().finally(() => {
          process.exit(0);
        });
      });
    });
  }
}

if (require.main === module) {
  void start().catch((error: unknown) => {
    console.error('Failed to start the server:', error);
    process.exitCode = 1;
  });
}

export default app;
