export class InvalidAuthRequestError extends Error {
  constructor() {
    super('A non-empty idToken is required.');
    this.name = 'InvalidAuthRequestError';
  }
}

export class InvalidGoogleTokenError extends Error {
  constructor() {
    super('Google ID token is invalid.');
    this.name = 'InvalidGoogleTokenError';
  }
}

export class GoogleTokenVerificationUnavailableError extends Error {
  constructor() {
    super('Authentication service is temporarily unavailable.');
    this.name = 'GoogleTokenVerificationUnavailableError';
  }
}

export class SessionRequiredError extends Error {
  constructor() {
    super('A bearer session token is required.');
    this.name = 'SessionRequiredError';
  }
}

export function sendAuthError(response: Response, error: unknown): void {
  if (error instanceof InvalidAuthRequestError) {
    response.status(400).json({
      error: { code: 'INVALID_REQUEST', message: error.message },
    });
    return;
  }

  if (error instanceof InvalidGoogleTokenError) {
    response.status(401).json({
      error: { code: 'INVALID_GOOGLE_TOKEN', message: error.message },
    });
    return;
  }

  if (error instanceof SessionRequiredError) {
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

  if (error instanceof GoogleTokenVerificationUnavailableError) {
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
import type { Response } from 'express';

import * as SessionError from '../session/sessionError';
