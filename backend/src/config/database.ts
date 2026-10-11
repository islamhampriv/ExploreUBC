import mongoose from "mongoose";

import { env } from "./env";

/** Connects the backend to MongoDB. */
export async function connectToDatabase(): Promise<void> {
  await mongoose.connect(env.mongodbUri);
}

/** Closes the MongoDB connection. */
export async function disconnectFromDatabase(): Promise<void> {
  await mongoose.disconnect();
}
