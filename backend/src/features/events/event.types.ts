// Defines the internal TypeScript types used by the events feature.

export interface LiveEvent {
  externalId: string;
  title: string;
  imageUrl: string;
  startDateTime: Date;
  venueName: string;
  city: string;
  category: string;
  ticketUrl: string;
  priceMin?: number;
  priceMax?: number;
  currency?: string;
  lastSyncedAt: Date;
}
