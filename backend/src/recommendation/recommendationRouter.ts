import { Router, type Response } from 'express';

import {
  InvalidRecommendationOwnerError,
  InvalidRecommendationResponseError,
  RecommendationConfigurationError,
  RecommendationGenerationError,
  RecommendationProfileIncompleteError,
  RecommendationUserNotFoundError,
} from './recommendationErrors';
import type { RecommendationService } from './recommendationService';

function sendError(response: Response, error: unknown): void {
  if (error instanceof RecommendationProfileIncompleteError) {
    response.status(400).json({
      error: { code: 'INCOMPLETE_PROFILE', message: error.message },
    });
    return;
  }
  if (error instanceof RecommendationUserNotFoundError) {
    response.status(404).json({
      error: { code: 'USER_NOT_FOUND', message: error.message },
    });
    return;
  }
  if (error instanceof InvalidRecommendationOwnerError) {
    response.status(400).json({
      error: { code: 'INVALID_USER_ID', message: error.message },
    });
    return;
  }
  if (
    error instanceof InvalidRecommendationResponseError ||
    error instanceof RecommendationGenerationError
  ) {
    response.status(502).json({
      error: {
        code: 'RECOMMENDATION_GENERATION_FAILED',
        message: 'Recommendations could not be generated. Please try again.',
      },
    });
    return;
  }
  if (error instanceof RecommendationConfigurationError) {
    response.status(500).json({
      error: {
        code: 'RECOMMENDATION_NOT_CONFIGURED',
        message: 'The recommendation service is not configured.',
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

export function createRecommendationRouter(
  service: RecommendationService,
): Router {
  const router = Router({ mergeParams: true });

  router.post('/', async (request, response) => {
    try {
      const { userId } = request.params as { userId?: string };
      if (userId === undefined || userId === '') {
        throw new InvalidRecommendationOwnerError();
      }
      const itineraries = await service.generateRecommendations(userId);
      response.status(201).json({ itineraries });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.get('/', async (request, response) => {
    try {
      const { userId } = request.params as { userId?: string };
      if (userId === undefined || userId === '') {
        throw new InvalidRecommendationOwnerError();
      }
      const itineraries = await service.findLatestRecommendations(userId);
      response.json({ itineraries });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  return router;
}
