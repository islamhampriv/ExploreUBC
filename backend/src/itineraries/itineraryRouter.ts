import { Router, type Response } from 'express';

import {
  InvalidItineraryDetailsError,
  InvalidItineraryFiltersError,
  InvalidItineraryItemError,
  InvalidItineraryOwnerError,
  ItineraryItemNotFoundError,
  ItineraryNotFoundError,
  ItineraryTimeConflictError,
} from './itineraryErrors';
import type { ItineraryService } from './itineraryService';
import type { ItineraryMapService } from '../maps/itineraryMapService';
import {
  parseItinerary,
  parseItineraryDetailsUpdate,
  parseItineraryFilters,
  parseActivityInput,
  parseActivityUpdate,
  parseTransportationSegmentInput,
  parseTransportationSegmentUpdate,
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

  if (error instanceof ItineraryItemNotFoundError) {
    response.status(404).json({
      error: {
        code: 'ITINERARY_ITEM_NOT_FOUND',
        message: error.message,
      },
    });
    return;
  }

  if (error instanceof ItineraryTimeConflictError) {
    response.status(409).json({
      error: {
        code: 'ITINERARY_TIME_CONFLICT',
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
  itineraryMapService?: ItineraryMapService,
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

  if (itineraryMapService !== undefined) {
    router.get('/:itineraryId/map', async (request, response) => {
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
  }

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

  router.patch(
    '/:itineraryId/activities/:activityId',
    async (request, response) => {
      try {
        const { userId, itineraryId, itemId } = getItemRouteIds(
          request.params,
          'activityId',
        );
        const update = parseActivityUpdate(request.body);
        const activity = await itineraryService.updateActivity(
          userId,
          itineraryId,
          itemId,
          update,
        );
        response.json({ activity });
      } catch (error: unknown) {
        sendError(response, error);
      }
    },
  );

  router.delete(
    '/:itineraryId/activities/:activityId',
    async (request, response) => {
      try {
        const { userId, itineraryId, itemId } = getItemRouteIds(
          request.params,
          'activityId',
        );
        await itineraryService.removeActivity(userId, itineraryId, itemId);
        response.status(204).send();
      } catch (error: unknown) {
        sendError(response, error);
      }
    },
  );

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

  router.patch(
    '/:itineraryId/transportation-segments/:segmentId',
    async (request, response) => {
      try {
        const { userId, itineraryId, itemId } = getItemRouteIds(
          request.params,
          'segmentId',
        );
        const update = parseTransportationSegmentUpdate(request.body);
        const transportationSegment =
          await itineraryService.updateTransportationSegment(
            userId,
            itineraryId,
            itemId,
            update,
          );
        response.json({ transportationSegment });
      } catch (error: unknown) {
        sendError(response, error);
      }
    },
  );

  router.delete(
    '/:itineraryId/transportation-segments/:segmentId',
    async (request, response) => {
      try {
        const { userId, itineraryId, itemId } = getItemRouteIds(
          request.params,
          'segmentId',
        );
        await itineraryService.removeTransportationSegment(
          userId,
          itineraryId,
          itemId,
        );
        response.status(204).send();
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
      await itineraryMapService?.deleteItineraryMap(itineraryId);
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

function getItemRouteIds(
  params: unknown,
  itemParameter: 'activityId' | 'segmentId',
): { userId: string; itineraryId: string; itemId: string } {
  const { userId, itineraryId } = getRouteIds(params);
  const itemId = (params as Record<string, string | undefined>)[itemParameter];
  if (itemId === undefined || itemId === '') {
    throw new ItineraryItemNotFoundError('');
  }
  return { userId, itineraryId, itemId };
}
