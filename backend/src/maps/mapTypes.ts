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

export type RouteStep = {
  instruction: string;
};

export type TravelOption = {
  encodedPolyline: string;
  distanceMeters: number;
  durationSeconds: number;
  steps: RouteStep[];
};

export type MapRoute = {
  fromActivityId: string;
  toActivityId: string;
  transportationMode: TransportationMode;
  encodedPolyline: string;
  distanceMeters: number;
  durationSeconds: number;
  walking: TravelOption | null;
  driving: TravelOption | null;
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
