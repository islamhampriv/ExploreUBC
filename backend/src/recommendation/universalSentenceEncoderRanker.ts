import '@tensorflow/tfjs';
import { load } from '@tensorflow-models/universal-sentence-encoder';

import type { InterestPreference } from '../users/userTypes';
import { RecommendationGenerationError } from './recommendationErrors';
import type {
  GeneratedItineraryInput,
  ItineraryRanker,
  RecommendedItineraryInput,
} from './recommendationTypes';

const STOP_WORDS = new Set([
  'a', 'an', 'and', 'are', 'as', 'at', 'be', 'by', 'for', 'from',
  'has', 'have', 'in', 'into', 'is', 'it', 'its', 'of', 'on', 'or',
  'that', 'the', 'their', 'then', 'this', 'to', 'with', 'your',
]);

type EmbeddingTensor = {
  array(): Promise<number[][]>;
  dispose(): void;
};

type SentenceEncoderModel = {
  embed(input: string[]): Promise<EmbeddingTensor>;
};

type ModelLoader = () => Promise<SentenceEncoderModel>;

export function cleanTextForRanking(text: string): string {
  const words = text
    .toLowerCase()
    .replaceAll('_', ' ')
    .match(/\p{L}[\p{L}\p{N}]*/gu) ?? [];
  return words.filter((word) => !STOP_WORDS.has(word)).join(' ');
}

function cosineSimilarity(
  left: number[] | undefined,
  right: number[] | undefined,
): number {
  if (left === undefined || right === undefined || left.length !== right.length) return 0;

  let dotProduct = 0;
  let leftMagnitudeSquared = 0;
  let rightMagnitudeSquared = 0;
  for (let index = 0; index < left.length; index += 1) {
    const leftValue = left[index]!;
    const rightValue = right[index]!;
    dotProduct += leftValue * rightValue;
    leftMagnitudeSquared += leftValue * leftValue;
    rightMagnitudeSquared += rightValue * rightValue;
  }
  const denominator = Math.sqrt(leftMagnitudeSquared) * Math.sqrt(rightMagnitudeSquared);
  if (denominator === 0) return 0;
  return Math.max(-1, Math.min(1, dotProduct / denominator));
}

export class UniversalSentenceEncoderItineraryRanker implements ItineraryRanker {
  private modelPromise: Promise<SentenceEncoderModel> | undefined;

  constructor(private readonly modelLoader: ModelLoader = load) {}

  async rankItineraries(
    itineraries: GeneratedItineraryInput[],
    interest: InterestPreference,
  ): Promise<RecommendedItineraryInput[]> {
    const model = await this.getModel();
    const cleanedInterest = cleanTextForRanking(interest);
    const cleanedDescriptions = itineraries.map((itinerary) =>
      cleanTextForRanking(itinerary.description ?? ''),
    );
    const tensor = await model.embed([cleanedInterest, ...cleanedDescriptions]);
    let embeddings: number[][];
    try {
      embeddings = await tensor.array();
    } finally {
      tensor.dispose();
    }
    const interestEmbedding = embeddings[0];

    const scoredItineraries = itineraries.map((itinerary, index) => {
        const interestSimilarity = cleanedDescriptions[index] === ''
          ? 0
          : Number(
            cosineSimilarity(
              embeddings[index + 1],
              interestEmbedding,
            ).toFixed(6),
          );

        return {
          itinerary,
          interestSimilarity,
          originalIndex: index,
        };
      });
    const rawScores = scoredItineraries.map(({ interestSimilarity }) =>
      interestSimilarity,
    );
    const minimumScore = Math.min(...rawScores);
    const maximumScore = Math.max(...rawScores);
    const scoreRange = maximumScore - minimumScore;

    return scoredItineraries
      .map(({ itinerary, interestSimilarity, originalIndex }) => ({
        itinerary: {
          ...itinerary,
          score: {
            interestSimilarity,
            interestSimilarityOutOfFive: Number((scoreRange === 0
              ? 4.25
              : 3.5 +
                ((interestSimilarity - minimumScore) / scoreRange) * 1.5
            ).toFixed(6)),
          },
        },
        originalIndex,
      }))
      .sort((left, right) =>
        right.itinerary.score.interestSimilarity! -
          left.itinerary.score.interestSimilarity! ||
        left.originalIndex - right.originalIndex,
      )
      .map(({ itinerary }) => itinerary);
  }

  private getModel(): Promise<SentenceEncoderModel> {
    this.modelPromise ??= this.modelLoader().catch((error: unknown) => {
      throw new RecommendationGenerationError(
        error instanceof Error
          ? `Could not load the sentence encoder model: ${error.message}`
          : 'Could not load the sentence encoder model.',
      );
    });
    return this.modelPromise;
  }
}
