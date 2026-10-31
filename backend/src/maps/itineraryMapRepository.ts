import type { ItineraryMap } from './mapTypes';

export interface ItineraryMapRepository {
  findByItineraryId(itineraryId: string): Promise<ItineraryMap | null>;
  upsertMapData(mapData: ItineraryMap): Promise<void>;
  deleteByItineraryId(itineraryId: string): Promise<boolean>;
}
