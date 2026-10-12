export class InvalidItineraryDetailsError extends Error {
  readonly details: string[];

  constructor(details: string[]) {
    super('The itinerary details are invalid.');
    this.name = 'InvalidItineraryDetailsError';
    this.details = details;
  }
}

export class InvalidItineraryFiltersError extends Error {
  readonly details: string[];

  constructor(details: string[]) {
    super('The itinerary filters are invalid.');
    this.name = 'InvalidItineraryFiltersError';
    this.details = details;
  }
}

export class InvalidItineraryOwnerError extends Error {
  constructor(userId: string) {
    super(`User ID '${userId}' is not valid.`);
    this.name = 'InvalidItineraryOwnerError';
  }
}

export class ItineraryNotFoundError extends Error {
  constructor(itineraryId: string) {
    super(`Itinerary '${itineraryId}' was not found.`);
    this.name = 'ItineraryNotFoundError';
  }
}
