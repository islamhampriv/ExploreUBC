import { createHash, randomBytes } from 'node:crypto';

import { ExpiredSessionError, InvalidSessionError } from './sessionError';
import type { SessionRepository } from './sessionRepository';
import type { CreatedSession, Session } from './sessionTypes';

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;
const SESSION_TOKEN_BYTES = 32;

type Clock = () => Date;
type RandomBytes = (size: number) => Buffer;

export class SessionService {
  constructor(
    private readonly repository: SessionRepository,
    private readonly ttlDays: number,
    private readonly clock: Clock = () => new Date(),
    private readonly generateRandomBytes: RandomBytes = randomBytes,
  ) {
    if (!Number.isInteger(ttlDays) || ttlDays < 1) {
      throw new Error('Session TTL must be a positive whole number of days.');
    }
  }

  async createSession(userId: string): Promise<CreatedSession> {
    const createdAt = this.clock();
    const expiresAt = this.calculateExpiry(createdAt);
    const token = this.generateRandomBytes(SESSION_TOKEN_BYTES).toString(
      'base64url',
    );
    const session = await this.repository.create({
      userId,
      tokenHash: this.hashToken(token),
      createdAt,
      expiresAt,
    });

    return {
      token,
      sessionId: session.id,
      userId: session.userId,
      expiresAt: session.expiresAt,
    };
  }

  async validateSession(token: string): Promise<Session> {
    const session = await this.repository.findByTokenHash(this.hashToken(token));
    if (session === null) {
      throw new InvalidSessionError();
    }

    if (session.expiresAt.getTime() <= this.clock().getTime()) {
      await this.repository.deleteByTokenHash(session.tokenHash);
      throw new ExpiredSessionError();
    }

    return session;
  }

  async refreshSession(token: string): Promise<Session> {
    const session = await this.validateSession(token);
    const refreshed = await this.repository.updateExpiryByTokenHash(
      session.tokenHash,
      this.calculateExpiry(this.clock()),
    );
    if (refreshed === null) {
      throw new InvalidSessionError();
    }
    return refreshed;
  }

  async revokeSession(token: string): Promise<void> {
    await this.repository.deleteByTokenHash(this.hashToken(token));
  }

  private hashToken(token: string): string {
    return createHash('sha256').update(token, 'utf8').digest('hex');
  }

  private calculateExpiry(from: Date): Date {
    return new Date(from.getTime() + this.ttlDays * MILLISECONDS_PER_DAY);
  }
}
