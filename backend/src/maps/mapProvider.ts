import type { TransportationMode } from '../users/userTypes';
import type {
  GeocodedLocation,
  MapCoordinates,
  TravelOption,
} from './mapTypes';

/** Provides geocoding and directions independently of a specific map vendor. */
export interface MapProvider {
  geocodeAddress(address: string): Promise<GeocodedLocation | null>;
  getRoute(
    origin: MapCoordinates,
    destination: MapCoordinates,
    transportationMode: TransportationMode,
  ): Promise<TravelOption | null>;
}
