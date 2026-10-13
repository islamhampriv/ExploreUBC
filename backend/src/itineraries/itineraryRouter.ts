import { Router, type Response } from 'express';

import {
  InvalidItineraryDetailsError,
  InvalidItineraryFiltersError,
  InvalidItineraryItemError,
  InvalidItineraryOwnerError,
  ItineraryNotFoundError,
} from './itineraryErrors';
import type { ItineraryService } from './itineraryService';
import {
  parseItinerary,
  parseItineraryDetailsUpdate,
  parseItineraryFilters,
  parseActivityInput,
  parseTransportationSegmentInput,
} from './itineraryValidation';

function sendError(response: Response, error: unknown): void {
  if (error instanceof InvalidItineraryDetailsError) {
    response.status(400).json({
      error: {
        code: 'INVALID_ITINERARY_DETAILS',
        message: error.message,
        details: error.details,
      },
    });
    return;
  }

  if (error instanceof InvalidItineraryFiltersError) {
    response.status(400).json({
      error: {
        code: 'INVALID_ITINERARY_FILTERS',
        message: error.message,
        details: error.details,
      },
    });
    return;
  }

  if (error instanceof InvalidItineraryItemError) {
    response.status(400).json({
      error: {
        code: 'INVALID_ITINERARY_ITEM',
        message: error.message,
        details: error.details,
      },
    });
    return;
  }

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

  response.status(500).json({
    error: {
      code: 'INTERNAL_SERVER_ERROR',
      message: 'An unexpected error occurred.',
    },
  });
}

export function createItineraryRouter(
  itineraryService: ItineraryService,
): Router {
  const router = Router({ mergeParams: true });

  router.post('/', async (request, response) => {
    try {
      const { userId } = request.params as { userId?: string };
      if (userId === undefined || userId === '') {
        throw new InvalidItineraryOwnerError('');
      }

      const itineraryInput = parseItinerary(request.body, userId);
      const itinerary = await itineraryService.createItinerary(
        userId,
        itineraryInput,
      );

      response
        .status(201)
        .location(
          `/api/v1/users/${encodeURIComponent(userId)}/itineraries/${itinerary.id}`,
        )
        .json({ itinerary });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.get('/:itineraryId/details', async (request, response) => {
    try {
      const { userId, itineraryId } = request.params as {
        userId?: string;
        itineraryId?: string;
      };
      if (userId === undefined || userId === '') {
        throw new InvalidItineraryOwnerError('');
      }
      if (itineraryId === undefined || itineraryId === '') {
        throw new ItineraryNotFoundError('');
      }

      const details = await itineraryService.getItineraryDetails(
        userId,
        itineraryId,
      );
      response.json({ details });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.get('/', async (request, response) => {
    try {
      const { userId } = request.params as { userId?: string };
      if (userId === undefined || userId === '') {
        throw new InvalidItineraryOwnerError('');
      }
      const filters = parseItineraryFilters(request.query);
      const itineraries = await itineraryService.getItineraries(userId, filters);
      response.json({ itineraries });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.get('/:itineraryId', async (request, response) => {
    try {
      const { userId, itineraryId } = getRouteIds(request.params);
      const itinerary = await itineraryService.getItinerary(userId, itineraryId);
      response.json({ itinerary });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.post('/:itineraryId/activities', async (request, response) => {
    try {
      const { userId, itineraryId } = getRouteIds(request.params);
      const activityInput = parseActivityInput(request.body);
      const activity = await itineraryService.addActivity(
        userId,
        itineraryId,
        activityInput,
      );
      response.status(201).json({ activity });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.post(
    '/:itineraryId/transportation-segments',
    async (request, response) => {
      try {
        const { userId, itineraryId } = getRouteIds(request.params);
        const segmentInput = parseTransportationSegmentInput(request.body);
        const transportationSegment =
          await itineraryService.addTransportationSegment(
            userId,
            itineraryId,
            segmentInput,
          );
        response.status(201).json({ transportationSegment });
      } catch (error: unknown) {
        sendError(response, error);
      }
    },
  );

  router.patch('/:itineraryId', async (request, response) => {
    try {
      const { userId, itineraryId } = getRouteIds(request.params);
      const details = parseItineraryDetailsUpdate(request.body);
      const itinerary = await itineraryService.updateItineraryDetails(
        userId,
        itineraryId,
        details,
      );
      response.json({ itinerary });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.delete('/:itineraryId', async (request, response) => {
    try {
      const { userId, itineraryId } = getRouteIds(request.params);
      await itineraryService.deleteItinerary(userId, itineraryId);
      response.status(204).send();
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
