import { randomUUID } from 'node:crypto';

import type { ItineraryRepository } from './itineraryRepository';
import {
  InvalidItineraryItemError,
  ItineraryTimeConflictError,
  ItineraryItemNotFoundError,
  ItineraryNotFoundError,
} from './itineraryErrors';
import type {
  ActivityItem,
  ActivityUpdate,
  CreateActivityInput,
  CreateItineraryInput,
  CreateTransportationInput,
  Itinerary,
  ItineraryDetails,
  ItineraryDetailsUpdate,
  ItineraryFilters,
  TransportationItem,
  TransportationSegmentUpdate,
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
    const itinerary = await this.getItinerary(userId, itineraryId);
    this.validateNoTimeConflict(
      itinerary,
      activityInput.startTime,
      activityInput.endTime,
    );

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
    const itinerary = await this.getItinerary(userId, itineraryId);
    this.validateNoTimeConflict(
      itinerary,
      segmentInput.startTime,
      segmentInput.endTime,
    );

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

  async removeActivity(
    userId: string,
    itineraryId: string,
    activityId: string,
  ): Promise<boolean> {
    await this.findSavedItem(userId, itineraryId, activityId, 'activity');
    const removed = await this.repository.removeEmbeddedActivity(
      userId,
      itineraryId,
      activityId,
    );
    if (!removed) {
      throw new ItineraryItemNotFoundError(activityId);
    }
    return true;
  }

  async removeTransportationSegment(
    userId: string,
    itineraryId: string,
    segmentId: string,
  ): Promise<boolean> {
    await this.findSavedItem(
      userId,
      itineraryId,
      segmentId,
      'transportation',
    );
    const removed =
      await this.repository.removeEmbeddedTransportationSegment(
        userId,
        itineraryId,
        segmentId,
      );
    if (!removed) {
      throw new ItineraryItemNotFoundError(segmentId);
    }
    return true;
  }

  async updateActivity(
    userId: string,
    itineraryId: string,
    activityId: string,
    update: ActivityUpdate,
  ): Promise<ActivityItem> {
    const itinerary = await this.getItinerary(userId, itineraryId);
    const current = this.findItem(
      itinerary,
      activityId,
      'activity',
    ) as ActivityItem;
    const activity: ActivityItem = { ...current, ...update };
    this.validateMergedTimeOrder(activity.startTime, activity.endTime);
    this.validateNoTimeConflict(
      itinerary,
      activity.startTime,
      activity.endTime,
      activityId,
    );

    const updated = await this.repository.updateEmbeddedActivity(
      userId,
      itineraryId,
      activityId,
      update,
    );
    if (!updated) {
      throw new ItineraryItemNotFoundError(activityId);
    }
    return activity;
  }

  async updateTransportationSegment(
    userId: string,
    itineraryId: string,
    segmentId: string,
    update: TransportationSegmentUpdate,
  ): Promise<TransportationItem> {
    const itinerary = await this.getItinerary(userId, itineraryId);
    const current = this.findItem(
      itinerary,
      segmentId,
      'transportation',
    ) as TransportationItem;
    const segment: TransportationItem = { ...current, ...update };
    this.validateMergedTimeOrder(segment.startTime, segment.endTime);
    this.validateNoTimeConflict(
      itinerary,
      segment.startTime,
      segment.endTime,
      segmentId,
    );

    const updated =
      await this.repository.updateEmbeddedTransportationSegment(
        userId,
        itineraryId,
        segmentId,
        update,
      );
    if (!updated) {
      throw new ItineraryItemNotFoundError(segmentId);
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

  private async findSavedItem(
    userId: string,
    itineraryId: string,
    itemId: string,
    type: 'activity' | 'transportation',
  ): Promise<ActivityItem | TransportationItem> {
    const itinerary = await this.getItinerary(userId, itineraryId);
    return this.findItem(itinerary, itemId, type);
  }

  private findItem(
    itinerary: Itinerary,
    itemId: string,
    type: 'activity' | 'transportation',
  ): ActivityItem | TransportationItem {
    if (!itinerary.saved) {
      throw new ItineraryNotFoundError(itinerary.id);
    }
    const item = itinerary.items.find(
      (candidate) => candidate.id === itemId && candidate.type === type,
    );
    if (item === undefined) {
      throw new ItineraryItemNotFoundError(itemId);
    }
    return item;
  }

  private validateNoTimeConflict(
    itinerary: Itinerary,
    startTime: string,
    endTime: string,
    excludedItemId?: string,
  ): void {
    const newStartTime = Date.parse(startTime);
    const newEndTime = Date.parse(endTime);
    const conflictingItem = itinerary.items.find(
      (item) =>
        item.id !== excludedItemId &&
        newStartTime < Date.parse(item.endTime) &&
        newEndTime > Date.parse(item.startTime),
    );
    if (conflictingItem !== undefined) {
      const conflictName = conflictingItem.type === 'activity'
        ? conflictingItem.name
        : 'transportation segment';
      throw new ItineraryTimeConflictError(conflictName);
    }
  }

  private validateMergedTimeOrder(startTime: string, endTime: string): void {
    if (Date.parse(startTime) >= Date.parse(endTime)) {
      throw new InvalidItineraryItemError([
        'startTime must be earlier than endTime.',
      ]);
    }
  }
}
