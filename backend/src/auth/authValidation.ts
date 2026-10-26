import {
  InvalidAuthRequestError,
  SessionRequiredError,
} from './authError';

export function parseGoogleAuthRequest(body: unknown): string {
  if (
    typeof body !== 'object' ||
    body === null ||
    Array.isArray(body) ||
    !('idToken' in body) ||
    typeof body.idToken !== 'string' ||
    body.idToken.trim() === ''
  ) {
    throw new InvalidAuthRequestError();
  }

  return body.idToken;
}

export function parseBearerToken(authorization: string | undefined): string {
  if (authorization === undefined) {
    throw new SessionRequiredError();
  }

  const match = /^Bearer ([^\s]+)$/u.exec(authorization);
  if (match?.[1] === undefined) {
    throw new SessionRequiredError();
  }

  return match[1];
}
