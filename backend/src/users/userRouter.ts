import { Router, type Response } from 'express';

import {
  InvalidOnboardingError,
  InvalidUserUpdateError,
  UserNotFoundError,
} from './userErrors';
import type { UserService } from './userService';
import { parseOnboarding, parseUserUpdate } from './userValidation';

function sendError(response: Response, error: unknown): void {
  if (error instanceof InvalidOnboardingError) {
    response.status(400).json({
      error: {
        code: 'INVALID_ONBOARDING',
        message: error.message,
        details: error.details,
      },
    });
    return;
  }

  if (error instanceof InvalidUserUpdateError) {
    response.status(400).json({
      error: {
        code: 'INVALID_USER_UPDATE',
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
  const router = Router({ mergeParams: true });

  router.get('/', async (request, response) => {
    try {
      const { userId } = request.params as { userId: string };
      const user = await userService.getUser(userId);
      response.json({ user });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.patch('/', async (request, response) => {
    try {
      const { userId } = request.params as { userId: string };
      const update = parseUserUpdate(request.body);
      const user = await userService.updateUser(userId, update);
      response.json({ user });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.put('/onboarding', async (request, response) => {
    try {
      const { userId } = request.params as { userId: string };
      const onboarding = parseOnboarding(request.body);
      const user = await userService.completeOnboarding(
        userId,
        onboarding,
      );
      response.json({
        onboardingRequired: false,
        user,
      });
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  return router;
}
