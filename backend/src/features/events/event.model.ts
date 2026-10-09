// Defines the MongoDB schema, model, validation, and indexes for events.

import { model, Schema } from "mongoose";

import { LiveEvent } from "./event.types";

const liveEventSchema = new Schema<LiveEvent>({
  externalId: { type: String, required: true, unique: true, trim: true },
  title: { type: String, required: true, trim: true },
  imageUrl: { type: String, required: true, trim: true },
  startDateTime: { type: Date, required: true },
  venueName: { type: String, required: true, trim: true },
  city: { type: String, required: true, trim: true },
  category: { type: String, required: true, trim: true },
  ticketUrl: { type: String, required: true, trim: true },
  priceMin: { type: Number, min: 0 },
  priceMax: { type: Number, min: 0 },
  currency: { type: String, trim: true, uppercase: true },
  lastSyncedAt: { type: Date, required: true },
});

liveEventSchema.index({ startDateTime: 1 });
liveEventSchema.index({ category: 1, startDateTime: 1 });

export const LiveEventModel = model<LiveEvent>("LiveEvent", liveEventSchema);
