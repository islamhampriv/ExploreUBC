import type { TransportationMode } from '../users/userTypes';

export type ActivityItem = {
  id: string;
  type: 'activity';
  name: string;
  image: string;
  description?: string;
  category?: string;
  location: string;
  startTime: string;
  endTime: string;
};

export type TransportationItem = {
  id: string;
  type: 'transportation';
  mode: TransportationMode;
  startLocation: string;
  endLocation: string;
  distanceMeters: number;
  startTime: string;
  endTime: string;
};

export type ItineraryItem = ActivityItem | TransportationItem;

export type CreateActivityInput = Omit<ActivityItem, 'id'>;
export type CreateTransportationInput = Omit<TransportationItem, 'id'>;
export type ActivityUpdate = Partial<Omit<ActivityItem, 'id' | 'type'>>;
export type TransportationSegmentUpdate = Partial<
  Omit<TransportationItem, 'id' | 'type'>
>;
export type CreateItineraryItemInput =
  | CreateActivityInput
  | CreateTransportationInput;

export type ItineraryScore = Record<string, number>;

export type Itinerary = {
  id: string;
  ownerId: string;
  name: string;
  description?: string;
  startingLocation?: string;
  items: ItineraryItem[];
  createdAt: string;
  updatedAt: string;
  saved: boolean;
  inRecommendation: boolean;
  score: ItineraryScore;
};

export type CreateItineraryInput = Omit<
  Itinerary,
  'id' | 'items' | 'createdAt' | 'updatedAt'
> & {
  items: CreateItineraryItemInput[];
};

export type ItineraryDetails = {
  name: string;
  description?: string;
  startingLocation?: string;
};

export type ItineraryDetailsUpdate = Partial<
  ItineraryDetails & Pick<Itinerary, 'saved' | 'inRecommendation'>
>;

export type ItineraryFilters = Partial<
  Pick<Itinerary, 'saved' | 'inRecommendation'>
>;
