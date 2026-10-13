import { randomUUID } from 'node:crypto';

import { type Collection, type Db, type Document, ObjectId } from 'mongodb';

import { InvalidItineraryOwnerError } from './itineraryErrors';
import type { ItineraryRepository } from './itineraryRepository';
import type {
  ActivityItem,
  ActivityUpdate,
  CreateItineraryInput,
  Itinerary,
  ItineraryDetails,
  ItineraryDetailsUpdate,
  ItineraryFilters,
  ItineraryItem,
  ItineraryScore,
  TransportationItem,
  TransportationSegmentUpdate,
} from './itineraryTypes';

type ItineraryDocument = {
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
  score: ItineraryScore;
};

export class ItineraryDao implements ItineraryRepository {
  private readonly itineraries: Collection<ItineraryDocument>;

  constructor(database: Db) {
    this.itineraries = database.collection<ItineraryDocument>('itineraries');
  }

  async insertItinerary(
    userId: string,
    itinerary: CreateItineraryInput,
  ): Promise<Itinerary> {
    if (!ObjectId.isValid(userId)) {
      throw new InvalidItineraryOwnerError(userId);
    }

    const now = new Date();
    const document: ItineraryDocument = {
      _id: new ObjectId(),
      ownerId: new ObjectId(userId),
      name: itinerary.name,
      ...(itinerary.description === undefined
        ? {}
        : { description: itinerary.description }),
      ...(itinerary.startingLocation === undefined
        ? {}
        : { startingLocation: itinerary.startingLocation }),
      items: itinerary.items.map((item) => ({
        ...item,
        id: randomUUID(),
      })),
      createdAt: now,
      updatedAt: now,
      saved: itinerary.saved,
      inRecommendation: itinerary.inRecommendation,
      score: itinerary.score,
    };

    await this.itineraries.insertOne(document);

    return this.toItinerary(document);
  }

  async findItineraryDetails(
    userId: string,
    itineraryId: string,
  ): Promise<ItineraryDetails | null> {
    if (!ObjectId.isValid(userId) || !ObjectId.isValid(itineraryId)) {
      return null;
    }

    const document = await this.itineraries.findOne(
      {
        _id: new ObjectId(itineraryId),
        ownerId: new ObjectId(userId),
      },
      {
        projection: {
          name: 1,
          description: 1,
          startingLocation: 1,
        },
      },
    );

    if (document === null) {
      return null;
    }

    return {
      name: document.name,
      ...(document.description === undefined
        ? {}
        : { description: document.description }),
      ...(document.startingLocation === undefined
        ? {}
        : { startingLocation: document.startingLocation }),
    };
  }

  async findItinerary(
    userId: string,
    itineraryId: string,
  ): Promise<Itinerary | null> {
    const ids = this.parseIds(userId, itineraryId);
    if (ids === null) {
      return null;
    }

    const document = await this.itineraries.findOne({
      _id: ids.itineraryId,
      ownerId: ids.userId,
    });
    return document === null ? null : this.toItinerary(document);
  }

  async findItineraries(
    userId: string,
    filters: ItineraryFilters,
  ): Promise<Itinerary[]> {
    if (!ObjectId.isValid(userId)) {
      return [];
    }

    const documents = await this.itineraries
      .find({
        ownerId: new ObjectId(userId),
        ...(filters.saved === undefined ? {} : { saved: filters.saved }),
        ...(filters.inRecommendation === undefined
          ? {}
          : { inRecommendation: filters.inRecommendation }),
      })
      .sort({ updatedAt: -1 })
      .toArray();
    return documents.map((document) => this.toItinerary(document));
  }

  async updateItineraryDetails(
    userId: string,
    itineraryId: string,
    details: ItineraryDetailsUpdate,
  ): Promise<boolean> {
    const ids = this.parseIds(userId, itineraryId);
    if (ids === null) {
      return false;
    }

    const setFields: Document = {
      updatedAt: new Date(),
    };
    if (details.name !== undefined) {
      setFields.name = details.name;
    }
    if (details.description !== undefined) {
      setFields.description = details.description;
    }
    if (details.startingLocation !== undefined) {
      setFields.startingLocation = details.startingLocation;
    }

    const result = await this.itineraries.updateOne(
      { _id: ids.itineraryId, ownerId: ids.userId },
      { $set: setFields },
    );
    return result.matchedCount === 1;
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

  async removeEmbeddedActivity(
    userId: string,
    itineraryId: string,
    activityId: string,
  ): Promise<boolean> {
    return this.removeEmbeddedItem(
      userId,
      itineraryId,
      activityId,
      'activity',
    );
  }

  async removeEmbeddedTransportationSegment(
    userId: string,
    itineraryId: string,
    segmentId: string,
  ): Promise<boolean> {
    return this.removeEmbeddedItem(
      userId,
      itineraryId,
      segmentId,
      'transportation',
    );
  }

  async updateEmbeddedActivity(
    userId: string,
    itineraryId: string,
    activityId: string,
    update: ActivityUpdate,
  ): Promise<boolean> {
    return this.updateEmbeddedItem(
      userId,
      itineraryId,
      activityId,
      'activity',
      update,
    );
  }

  async updateEmbeddedTransportationSegment(
    userId: string,
    itineraryId: string,
    segmentId: string,
    update: TransportationSegmentUpdate,
  ): Promise<boolean> {
    return this.updateEmbeddedItem(
      userId,
      itineraryId,
      segmentId,
      'transportation',
      update,
    );
  }

  async removeItinerary(
    userId: string,
    itineraryId: string,
  ): Promise<boolean> {
    const ids = this.parseIds(userId, itineraryId);
    if (ids === null) {
      return false;
    }

    const result = await this.itineraries.deleteOne({
      _id: ids.itineraryId,
      ownerId: ids.userId,
    });
    return result.deletedCount === 1;
  }

  private parseIds(
    userId: string,
    itineraryId: string,
  ): { userId: ObjectId; itineraryId: ObjectId } | null {
    if (!ObjectId.isValid(userId) || !ObjectId.isValid(itineraryId)) {
      return null;
    }
    return {
      userId: new ObjectId(userId),
      itineraryId: new ObjectId(itineraryId),
    };
  }

  private async insertEmbeddedItem(
    userId: string,
    itineraryId: string,
    item: ItineraryItem,
  ): Promise<boolean> {
    const ids = this.parseIds(userId, itineraryId);
    if (ids === null) {
      return false;
    }

    const result = await this.itineraries.updateOne(
      {
        _id: ids.itineraryId,
        ownerId: ids.userId,
        saved: true,
      },
      {
        $push: { items: item },
        $set: { updatedAt: new Date() },
      },
    );
    return result.matchedCount === 1;
  }

  private async removeEmbeddedItem(
    userId: string,
    itineraryId: string,
    itemId: string,
    type: ItineraryItem['type'],
  ): Promise<boolean> {
    const ids = this.parseIds(userId, itineraryId);
    if (ids === null) {
      return false;
    }

    const result = await this.itineraries.updateOne(
      {
        _id: ids.itineraryId,
        ownerId: ids.userId,
        saved: true,
        items: { $elemMatch: { id: itemId, type } },
      },
      {
        $pull: { items: { id: itemId, type } },
        $set: { updatedAt: new Date() },
      },
    );
    return result.matchedCount === 1;
  }

  private async updateEmbeddedItem(
    userId: string,
    itineraryId: string,
    itemId: string,
    type: ItineraryItem['type'],
    update: ActivityUpdate | TransportationSegmentUpdate,
  ): Promise<boolean> {
    const ids = this.parseIds(userId, itineraryId);
    if (ids === null) {
      return false;
    }

    const setFields: Document = { updatedAt: new Date() };
    for (const [field, value] of Object.entries(update)) {
      setFields[`items.$.${field}`] = value;
    }

    const result = await this.itineraries.updateOne(
      {
        _id: ids.itineraryId,
        ownerId: ids.userId,
        saved: true,
        items: { $elemMatch: { id: itemId, type } },
      },
      { $set: setFields },
    );
    return result.matchedCount === 1;
  }

  private toItinerary(document: ItineraryDocument): Itinerary {
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
