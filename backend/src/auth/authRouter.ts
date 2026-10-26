import { Router } from 'express';

import * as AuthError from './authError';
import type { AuthService } from './authService';
import * as AuthValidation from './authValidation';

export function createAuthRouter(authService: AuthService): Router {
  const router = Router();

  router.post('/google', async (request, response) => {
    try {
      const idToken = AuthValidation.parseGoogleAuthRequest(request.body);
      const result = await authService.authenticateWithGoogle(idToken);
      response.status(200).json(result);
    } catch (error: unknown) {
      AuthError.sendAuthError(response, error);
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
      AuthError.sendAuthError(response, error);
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
      AuthError.sendAuthError(response, error);
    }
  });

  return router;
}
