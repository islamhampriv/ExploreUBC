import type { NewSession, Session } from './sessionTypes';

export interface SessionRepository {
  create(session: NewSession): Promise<Session>;
  findByTokenHash(tokenHash: string): Promise<Session | null>;
  updateExpiryByTokenHash(
    tokenHash: string,
    expiresAt: Date,
  ): Promise<Session | null>;
  deleteByTokenHash(tokenHash: string): Promise<boolean>;
}
