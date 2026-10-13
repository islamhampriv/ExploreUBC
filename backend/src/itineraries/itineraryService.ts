import { randomUUID } from 'node:crypto';

import type { ItineraryRepository } from './itineraryRepository';
import { ItineraryNotFoundError } from './itineraryErrors';
import type {
  ActivityItem,
  CreateActivityInput,
  CreateItineraryInput,
  CreateTransportationInput,
  Itinerary,
  ItineraryDetails,
  ItineraryDetailsUpdate,
  ItineraryFilters,
  TransportationItem,
} from './itineraryTypes';

export class ItineraryService {
  constructor(private readonly repository: ItineraryRepository) {}

  async createItinerary(
    userId: string,
    itinerary: CreateItineraryInput,
  ): Promise<Itinerary> {
    return this.repository.insertItinerary(userId, itinerary);
  }

  async getItineraryDetails(
    userId: string,
    itineraryId: string,
  ): Promise<ItineraryDetails> {
    const details = await this.repository.findItineraryDetails(
      userId,
      itineraryId,
    );
    if (details === null) {
      throw new ItineraryNotFoundError(itineraryId);
    }
    return details;
  }

  async getItinerary(userId: string, itineraryId: string): Promise<Itinerary> {
    const itinerary = await this.repository.findItinerary(userId, itineraryId);
    if (itinerary === null) {
      throw new ItineraryNotFoundError(itineraryId);
    }
    return itinerary;
  }

  async getItineraries(
    userId: string,
    filters: ItineraryFilters = {},
  ): Promise<Itinerary[]> {
    return this.repository.findItineraries(userId, filters);
  }

  async updateItineraryDetails(
    userId: string,
    itineraryId: string,
    details: ItineraryDetailsUpdate,
  ): Promise<Itinerary> {
    const updated = await this.repository.updateItineraryDetails(
      userId,
      itineraryId,
      details,
    );
    if (!updated) {
      throw new ItineraryNotFoundError(itineraryId);
    }
    return this.getItinerary(userId, itineraryId);
  }

  async addActivity(
    userId: string,
    itineraryId: string,
    activityInput: CreateActivityInput,
  ): Promise<ActivityItem> {
    const activity: ActivityItem = {
      ...activityInput,
      id: randomUUID(),
    };
    const inserted = await this.repository.insertEmbeddedActivity(
      userId,
      itineraryId,
      activity,
    );
    if (!inserted) {
      throw new ItineraryNotFoundError(itineraryId);
    }
    return activity;
  }

  async addTransportationSegment(
    userId: string,
    itineraryId: string,
    segmentInput: CreateTransportationInput,
  ): Promise<TransportationItem> {
    const segment: TransportationItem = {
      ...segmentInput,
      id: randomUUID(),
    };
    const inserted = await this.repository.insertEmbeddedTransportationSegment(
      userId,
      itineraryId,
      segment,
    );
    if (!inserted) {
      throw new ItineraryNotFoundError(itineraryId);
    }
    return segment;
  }

  async deleteItinerary(
    userId: string,
    itineraryId: string,
  ): Promise<boolean> {
    const deleted = await this.repository.removeItinerary(userId, itineraryId);
    if (!deleted) {
      throw new ItineraryNotFoundError(itineraryId);
    }
    return true;
  }
}
