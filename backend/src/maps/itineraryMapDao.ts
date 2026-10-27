import { type Collection, type Db, ObjectId } from 'mongodb';

import type { ItineraryMapRepository } from './itineraryMapRepository';
import type {
  ItineraryMap,
  MapMarker,
  MapRoute,
  UnmappedActivity,
} from './mapTypes';

type ItineraryMapDocument = {
  _id: ObjectId;
  itineraryId: ObjectId;
  ownerId: ObjectId;
  itineraryUpdatedAt: Date;
  markers: MapMarker[];
  routes: MapRoute[];
  unmappedActivities?: UnmappedActivity[];
  generatedAt: Date;
};

export class ItineraryMapDao implements ItineraryMapRepository {
  private readonly itineraryMaps: Collection<ItineraryMapDocument>;

  constructor(database: Db) {
    this.itineraryMaps =
      database.collection<ItineraryMapDocument>('itineraryMaps');
  }

  /** Creates the unique itinerary map index. */
  async createIndexes(): Promise<void> {
    await this.itineraryMaps.createIndex(
      { itineraryId: 1 },
      { unique: true, name: 'unique_itinerary_map' },
    );
  }

  /** Finds cached map data for one itinerary. */
  async findByItineraryId(itineraryId: string): Promise<ItineraryMap | null> {
    if (!ObjectId.isValid(itineraryId)) {
      return null;
    }
    const document = await this.itineraryMaps.findOne({
      itineraryId: new ObjectId(itineraryId),
    });
    return document === null ? null : this.toItineraryMap(document);
  }

  /** Inserts or replaces cached map data for one itinerary. */
  async upsertMapData(mapData: ItineraryMap): Promise<void> {
    if (
      !ObjectId.isValid(mapData.itineraryId) ||
      !ObjectId.isValid(mapData.ownerId)
    ) {
      throw new Error('Itinerary map contains an invalid ID');
    }

    await this.itineraryMaps.updateOne(
      { itineraryId: new ObjectId(mapData.itineraryId) },
      {
        $set: {
          ownerId: new ObjectId(mapData.ownerId),
          itineraryUpdatedAt: new Date(mapData.itineraryUpdatedAt),
          markers: mapData.markers,
          routes: mapData.routes,
          unmappedActivities: mapData.unmappedActivities,
          generatedAt: new Date(mapData.generatedAt),
        },
        $setOnInsert: { _id: new ObjectId() },
      },
      { upsert: true },
    );
  }

  /** Deletes cached map data for one itinerary. */
  async deleteByItineraryId(itineraryId: string): Promise<boolean> {
    if (!ObjectId.isValid(itineraryId)) {
      return false;
    }
    const result = await this.itineraryMaps.deleteOne({
      itineraryId: new ObjectId(itineraryId),
    });
    return result.deletedCount === 1;
  }

  /** Converts a MongoDB document into API-safe map data. */
  private toItineraryMap(document: ItineraryMapDocument): ItineraryMap {
    return {
      itineraryId: document.itineraryId.toHexString(),
      ownerId: document.ownerId.toHexString(),
      itineraryUpdatedAt: toIsoString(document.itineraryUpdatedAt),
      markers: document.markers,
      routes: document.routes,
      unmappedActivities: document.unmappedActivities ?? [],
      generatedAt: toIsoString(document.generatedAt),
    };
  }
}

function toIsoString(value: Date | string): string {
  if (value instanceof Date) {
    return value.toISOString();
  }
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toISOString();
}
