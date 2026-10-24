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
