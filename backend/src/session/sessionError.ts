export class InvalidSessionError extends Error {
  constructor() {
    super('Session is invalid or has been revoked.');
    this.name = 'InvalidSessionError';
  }
}

export class ExpiredSessionError extends Error {
  constructor() {
    super('Session has expired.');
    this.name = 'ExpiredSessionError';
  }
}
