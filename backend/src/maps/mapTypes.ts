import type { TransportationMode } from "../users/userTypes";

export type MapCoordinates = {
  latitude: number;
  longitude: number;
};

export type GeocodedLocation = {
  coordinates: MapCoordinates;
  formattedAddress: string;
  placeId: string;
  isApproximate: boolean;
};

export type MapMarker = {
  activityId: string;
  title: string;
  address: string;
  placeId: string;
  coordinates: MapCoordinates;
  isApproximate: boolean;
};

export type UnmappedActivity = {
  activityId: string;
  title: string;
  address: string;
};

export type CalculatedRoute = {
  transportationMode: TransportationMode;
  encodedPolyline: string;
  distanceMeters: number;
  durationSeconds: number;
};

// For storage
export type MapRoute = CalculatedRoute & {
  fromActivityId: string;
  toActivityId: string;
};

export type ItineraryMap = {
  itineraryId: string;
  ownerId: string;
  itineraryUpdatedAt: string;
  markers: MapMarker[];
  routes: MapRoute[];
  unmappedActivities: UnmappedActivity[];
  generatedAt: string;
};
