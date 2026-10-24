import type { NextFunction, Request, RequestHandler, Response } from 'express';

import * as SessionError from '../session/sessionError';
import type { SessionService } from '../session/sessionService';
import * as AuthError from './authError';
import * as AuthValidation from './authValidation';

export type AuthContext = {
  sessionId: string;
  userId: string;
  expiresAt: Date;
};

function sendError(response: Response, error: unknown): void {
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

  response.status(500).json({
    error: {
      code: 'INTERNAL_SERVER_ERROR',
      message: 'An unexpected error occurred.',
    },
  });
}

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
      sendError(response, error);
    }
  };
}

export function requireUserOwnership(
  request: Request,
  response: Response,
  next: NextFunction,
): void {
  if (request.auth === undefined) {
    sendError(response, new AuthError.SessionRequiredError());
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
