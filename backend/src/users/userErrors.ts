export class InvalidUserUpdateError extends Error {
  readonly details: string[];

  constructor(details: string[]) {
    super('The user update is invalid.');
    this.name = 'InvalidUserUpdateError';
    this.details = details;
  }
}

export class UserNotFoundError extends Error {
  constructor(userId: string) {
    super(`User '${userId}' was not found.`);
    this.name = 'UserNotFoundError';
  }
}
