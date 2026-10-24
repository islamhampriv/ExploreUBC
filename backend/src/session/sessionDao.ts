import { type Collection, type Db, ObjectId } from 'mongodb';

import type { SessionRepository } from './sessionRepository';
import type { NewSession, Session } from './sessionTypes';

type SessionDocument = {
  _id: ObjectId;
  userId: ObjectId;
  tokenHash: string;
  createdAt: Date;
  expiresAt: Date;
};

export class SessionDao implements SessionRepository {
  private readonly sessions: Collection<SessionDocument>;

  constructor(database: Db) {
    this.sessions = database.collection<SessionDocument>('sessions');
  }

  async create(session: NewSession): Promise<Session> {
    const document: SessionDocument = {
      _id: new ObjectId(),
      userId: new ObjectId(session.userId),
      tokenHash: session.tokenHash,
      createdAt: session.createdAt,
      expiresAt: session.expiresAt,
    };

    await this.sessions.insertOne(document);
    return this.toSession(document);
  }

  async findByTokenHash(tokenHash: string): Promise<Session | null> {
    const document = await this.sessions.findOne({ tokenHash });
    return document === null ? null : this.toSession(document);
  }

  async updateExpiryByTokenHash(
    tokenHash: string,
    expiresAt: Date,
  ): Promise<Session | null> {
    const document = await this.sessions.findOneAndUpdate(
      { tokenHash },
      { $set: { expiresAt } },
      { returnDocument: 'after' },
    );
    return document === null ? null : this.toSession(document);
  }

  async deleteByTokenHash(tokenHash: string): Promise<boolean> {
    const result = await this.sessions.deleteOne({ tokenHash });
    return result.deletedCount === 1;
  }

  private toSession(document: SessionDocument): Session {
    return {
      id: document._id.toHexString(),
      userId: document.userId.toHexString(),
      tokenHash: document.tokenHash,
      createdAt: document.createdAt,
      expiresAt: document.expiresAt,
    };
  }
}
