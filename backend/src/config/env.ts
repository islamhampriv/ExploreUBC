import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

const mongodbUri = process.env.MONGODB_URI?.trim();
if (mongodbUri === undefined || mongodbUri === '') {
  throw new Error('MONGODB_URI is required.');
}

export const env = {
  port,
  mongodbUri,
} as const;
