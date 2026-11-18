import { Router, type Response } from 'express';

import {
  InvalidItineraryOwnerError,
  ItineraryNotFoundError,
} from '../itineraries/itineraryErrors';
import type { ItineraryMapService } from './itineraryMapService';

/** Creates the HTTP endpoint for an itinerary's generated map. */
export function createItineraryMapRouter(
  itineraryMapService: ItineraryMapService,
): Router {
  const router = Router({ mergeParams: true });

  router.get('/', async (request, response) => {
    try {
      const { userId, itineraryId } = getRouteIds(request.params);
      const itineraryMap = await itineraryMapService.getItineraryMap(
        userId,
        itineraryId,
      );
      response.json({ itineraryMap });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  return router;
}

function getRouteIds(params: unknown): { userId: string; itineraryId: string } {
  const { userId, itineraryId } = params as {
    userId?: string;
    itineraryId?: string;
  };
  if (userId === undefined || userId === '') {
    throw new InvalidItineraryOwnerError('');
  }
  if (itineraryId === undefined || itineraryId === '') {
    throw new ItineraryNotFoundError('');
  }
  return { userId, itineraryId };
}

function sendError(response: Response, error: unknown): void {
  if (error instanceof InvalidItineraryOwnerError) {
    response.status(400).json({
      error: {
        code: 'INVALID_USER_ID',
        message: error.message,
      },
    });
    return;
  }

  if (error instanceof ItineraryNotFoundError) {
    response.status(404).json({
      error: {
        code: 'ITINERARY_NOT_FOUND',
        message: error.message,
      },
    });
    return;
  }

  console.error('Itinerary map request failed', error);
  response.status(500).json({
    error: {
      code: 'INTERNAL_SERVER_ERROR',
      message: 'An unexpected error occurred.',
    },
  });
}
