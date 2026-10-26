import { randomUUID } from 'node:crypto';

import { type Collection, type Db, ObjectId } from 'mongodb';

import type { Itinerary, ItineraryItem } from '../itineraries/itineraryTypes';
import { InvalidRecommendationOwnerError } from './recommendationErrors';
import type { RecommendationRepository } from './recommendationRepository';
import type { RecommendedItineraryInput } from './recommendationTypes';

type RecommendationDocument = {
  _id: ObjectId;
  ownerId: ObjectId;
  name: string;
  description?: string;
  startingLocation?: string;
  items: ItineraryItem[];
  createdAt: Date;
  updatedAt: Date;
  saved: boolean;
  inRecommendation: boolean;
  score: Record<string, number>;
};

export class RecommendationDao implements RecommendationRepository {
  private readonly itineraries: Collection<RecommendationDocument>;

  constructor(database: Db) {
    this.itineraries = database.collection<RecommendationDocument>('itineraries');
  }

  async findLatestRecommendations(userId: string): Promise<Itinerary[]> {
    if (!ObjectId.isValid(userId)) return [];

    const documents = await this.itineraries
      .find({
        ownerId: new ObjectId(userId),
        inRecommendation: true,
        saved: false,
      })
      .sort({ 'score.interestSimilarity': -1, updatedAt: -1, _id: 1 })
      .limit(4)
      .toArray();

    return documents.map((document) => this.toItinerary(document));
  }

  async replaceLatestRecommendations(
    userId: string,
    itineraries: RecommendedItineraryInput[],
  ): Promise<Itinerary[]> {
    if (!ObjectId.isValid(userId)) {
      throw new InvalidRecommendationOwnerError();
    }

    const ownerId = new ObjectId(userId);
    const now = new Date();
    const documents: RecommendationDocument[] = itineraries.map((itinerary) => ({
      _id: new ObjectId(),
      ownerId,
      name: itinerary.name,
      ...(itinerary.description === undefined
        ? {}
        : { description: itinerary.description }),
      ...(itinerary.startingLocation === undefined
        ? {}
        : { startingLocation: itinerary.startingLocation }),
      items: itinerary.items.map((item) => ({ ...item, id: randomUUID() })),
      createdAt: now,
      updatedAt: now,
      saved: false,
      inRecommendation: false,
      score: itinerary.score,
    }));

    await this.itineraries.insertMany(documents);
    const newIds = documents.map((document) => document._id);
    let publishedAt = now;

    try {
      await this.itineraries.deleteMany({
        ownerId,
        saved: false,
        inRecommendation: true,
      });

      publishedAt = new Date();
      const publishResult = await this.itineraries.updateMany(
        { ownerId, saved: false, inRecommendation: false, _id: { $in: newIds } },
        { $set: { inRecommendation: true, updatedAt: publishedAt } },
      );
      if (publishResult.matchedCount !== documents.length) {
        throw new Error('Not all generated recommendations could be published.');
      }
    } catch (error: unknown) {
      await this.itineraries.deleteMany({ _id: { $in: newIds } });
      throw error;
    }

    return documents.map((document) => this.toItinerary({
      ...document,
      updatedAt: publishedAt,
      inRecommendation: true,
    }));
  }

  private toItinerary(document: RecommendationDocument): Itinerary {
    return {
      id: document._id.toHexString(),
      ownerId: document.ownerId.toHexString(),
      name: document.name,
      ...(document.description === undefined
        ? {}
        : { description: document.description }),
      ...(document.startingLocation === undefined
        ? {}
        : { startingLocation: document.startingLocation }),
      items: document.items,
      createdAt: document.createdAt.toISOString(),
      updatedAt: document.updatedAt.toISOString(),
      saved: document.saved,
      inRecommendation: document.inRecommendation,
      score: document.score,
    };
  }
}
