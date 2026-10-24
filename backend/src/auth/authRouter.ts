import { Router, type Response } from 'express';

import * as SessionError from '../session/sessionError';
import * as AuthError from './authError';
import type { AuthService } from './authService';
import * as AuthValidation from './authValidation';

function sendError(response: Response, error: unknown): void {
  if (error instanceof AuthError.InvalidAuthRequestError) {
    response.status(400).json({
      error: { code: 'INVALID_REQUEST', message: error.message },
    });
    return;
  }

  if (error instanceof AuthError.InvalidGoogleTokenError) {
    response.status(401).json({
      error: { code: 'INVALID_GOOGLE_TOKEN', message: error.message },
    });
    return;
  }

  if (error instanceof AuthError.SessionRequiredError) {
    response.status(401).json({
      error: { code: 'SESSION_REQUIRED', message: error.message },
    });
    return;
  }

  if (error instanceof SessionError.ExpiredSessionError) {
    response.status(401).json({
      error: { code: 'SESSION_EXPIRED', message: error.message },
    });
    return;
  }

  if (error instanceof SessionError.InvalidSessionError) {
    response.status(401).json({
      error: { code: 'INVALID_SESSION', message: error.message },
    });
    return;
  }

  if (error instanceof AuthError.GoogleTokenVerificationUnavailableError) {
    response.status(503).json({
      error: { code: 'AUTH_SERVICE_UNAVAILABLE', message: error.message },
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

export function createAuthRouter(authService: AuthService): Router {
  const router = Router();

  router.post('/google', async (request, response) => {
    try {
      const idToken = AuthValidation.parseGoogleAuthRequest(request.body);
      const result = await authService.authenticateWithGoogle(idToken);
      response.status(200).json(result);
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.get('/session', async (request, response) => {
    try {
      const sessionToken = AuthValidation.parseBearerToken(
        request.header('authorization'),
      );
      const result = await authService.getCurrentSession(sessionToken);
      response.status(200).json(result);
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  router.delete('/session', async (request, response) => {
    try {
      const sessionToken = AuthValidation.parseBearerToken(
        request.header('authorization'),
      );
      await authService.logout(sessionToken);
      response.status(204).send();
    } catch (error: unknown) {
      sendError(response, error);
    }
  });

  return router;
}
