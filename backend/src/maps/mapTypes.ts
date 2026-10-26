import type { TransportationMode } from '../users/userTypes';

export type MapCoordinates = {
  latitude: number;
  longitude: number;
};

export type GeocodedLocation = {
  coordinates: MapCoordinates;
  formattedAddress: string;
  placeId: string;
};

export type MapRoute = {
  transportationMode: TransportationMode;
  encodedPolyline: string;
  distanceMeters: number;
  durationSeconds: number;
};
