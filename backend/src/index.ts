import { MongoClient } from 'mongodb';

import { createApp } from './app';
import { env } from './config/env';
import { ItineraryDao } from './itineraries/itineraryDao';
import { UserDao } from './users/userDao';

async function start(): Promise<void> {
  const mongoClient = new MongoClient(env.mongodbUri);
  await mongoClient.connect();

  const userDao = new UserDao(mongoClient.db());
  const itineraryDao = new ItineraryDao(mongoClient.db());
  const app = createApp({
    itineraryRepository: itineraryDao,
    userRepository: userDao,
  });
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

void start().catch((error: unknown) => {
  console.error('Failed to start the server:', error);
  process.exit(1);
});
