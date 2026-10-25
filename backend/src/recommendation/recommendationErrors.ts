export class RecommendationUserNotFoundError extends Error {
  constructor() {
    super('The requested user profile was not found.');
    this.name = 'RecommendationUserNotFoundError';
  }
}

export class RecommendationProfileIncompleteError extends Error {
  constructor() {
    super('A starting location is required before generating recommendations.');
    this.name = 'RecommendationProfileIncompleteError';
  }
}

export class RecommendationConfigurationError extends Error {
  constructor(missingVariables: string[]) {
    super(`Missing recommendation configuration: ${missingVariables.join(', ')}.`);
    this.name = 'RecommendationConfigurationError';
  }
}

export class RecommendationGenerationError extends Error {
  constructor(message = 'The itinerary generation service is unavailable.') {
    super(message);
    this.name = 'RecommendationGenerationError';
  }
}

export class InvalidRecommendationResponseError extends Error {
  constructor(public readonly details: string[]) {
    super('The itinerary generation service returned invalid itinerary data.');
    this.name = 'InvalidRecommendationResponseError';
  }
}

export class InvalidRecommendationOwnerError extends Error {
  constructor() {
    super('The requested user ID is invalid.');
    this.name = 'InvalidRecommendationOwnerError';
  }
}
