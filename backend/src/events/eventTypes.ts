// Defines the internal TypeScript types used by the events feature.

export interface LiveEvent {
  externalId: string;
  title: string;
  imageUrl: string;
  startDateTime: Date;
  venueName: string;
  city: string;
  location: {
    type: 'Point';
    coordinates: [number, number];
  };
  category: string;
  ticketUrl: string;
  priceMin?: number;
  priceMax?: number;
  currency?: string;
  lastSyncedAt: Date;
}

export interface LiveEventCandidate extends LiveEvent {
  distanceKm: number;
}

export interface RecommendedLiveEvent extends LiveEventCandidate {
  recommendationScore: number;
}

export interface NearbyEventQuery {
  longitude: number;
  latitude: number;
  maxDistanceKm: number;
  candidateLimit?: number;
}

export interface EventUpsertResult {
  matchedCount: number;
  modifiedCount: number;
  upsertedCount: number;
}
