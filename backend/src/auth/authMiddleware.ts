import type { NextFunction, Request, RequestHandler, Response } from 'express';

import type { SessionService } from '../session/sessionService';
import * as AuthError from './authError';
import * as AuthValidation from './authValidation';

export type AuthContext = {
  sessionId: string;
  userId: string;
  expiresAt: Date;
};

export function authenticateSession(
  sessionService: Pick<SessionService, 'validateSession'>,
): RequestHandler {
  return async (request: Request, response: Response, next: NextFunction) => {
    try {
      const token = AuthValidation.parseBearerToken(
        request.header('authorization'),
      );
      const session = await sessionService.validateSession(token);
      request.auth = {
        sessionId: session.id,
        userId: session.userId,
        expiresAt: session.expiresAt,
      };
      next();
    } catch (error: unknown) {
      AuthError.sendAuthError(response, error);
    }
  };
}

export function requireUserOwnership(
  request: Request,
  response: Response,
  next: NextFunction,
): void {
  if (request.auth === undefined) {
    AuthError.sendAuthError(response, new AuthError.SessionRequiredError());
    return;
  }

  if (request.params.userId !== request.auth.userId) {
    response.status(403).json({
      error: {
        code: 'FORBIDDEN',
        message: 'The authenticated user cannot access this resource.',
      },
    });
    return;
  }

  next();
}
