import { Router, type Response } from 'express';

import {
  InvalidPreferencesUpdateError,
  UserNotFoundError,
} from './userErrors';
import type { UserService } from './userService';
import { parsePreferencesUpdate } from './userValidation';

function sendError(response: Response, error: unknown): void {
  if (error instanceof InvalidPreferencesUpdateError) {
    response.status(400).json({
      error: {
        code: 'INVALID_PREFERENCES_UPDATE',
        message: error.message,
        details: error.details,
      },
    });
    return;
  }

  if (error instanceof UserNotFoundError) {
    response.status(404).json({
      error: {
        code: 'USER_NOT_FOUND',
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

export function createUserRouter(userService: UserService): Router {
  const router = Router();

  router.get('/:userId/preferences', async (request, response) => {
    try {
      const preferences = await userService.getPreferences(request.params.userId);
      response.json({ preferences });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.get('/:userId', async (request, response) => {
    try {
      const user = await userService.getUser(request.params.userId);
      response.json({ user });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.patch('/:userId/preferences', async (request, response) => {
    try {
      const update = parsePreferencesUpdate(request.body);
      const preferences = await userService.updatePreferences(
        request.params.userId,
        update,
      );
      response.json({ preferences });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  return router;
}

