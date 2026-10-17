import express, { type Express } from 'express';
import { MongoClient } from 'mongodb';
import { attachDatabasePool } from '@vercel/functions';

import type { ItineraryRepository } from './itineraries/itineraryRepository';
import { ItineraryDao } from './itineraries/itineraryDao';
import { createItineraryRouter } from './itineraries/itineraryRouter';
import { ItineraryService } from './itineraries/itineraryService';
import type { UserRepository } from './users/userRepository';
import { UserDao } from './users/userDao';
import { createUserRouter } from './users/userRouter';
import { UserService } from './users/userService';
import { env } from './config/env';

export type AppDependencies = {
  itineraryRepository?: ItineraryRepository;
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
const app = createApp({
  itineraryRepository: new ItineraryDao(database),
  userRepository: new UserDao(database),
});

async function start(): Promise<void> {
  await mongoClient.connect();
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
