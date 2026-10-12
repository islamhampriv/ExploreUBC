export class InvalidPreferencesUpdateError extends Error {
  readonly details: string[];

  constructor(details: string[]) {
    super('The preference update is invalid.');
    this.name = 'InvalidPreferencesUpdateError';
    this.details = details;
  }
}

export class UserNotFoundError extends Error {
  constructor(userId: string) {
    super(`User '${userId}' was not found.`);
    this.name = 'UserNotFoundError';
  }
}

