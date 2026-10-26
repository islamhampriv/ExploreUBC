export type Session = {
  id: string;
  userId: string;
  tokenHash: string;
  createdAt: Date;
  expiresAt: Date;
};

export type NewSession = Omit<Session, 'id'>;

export type CreatedSession = {
  token: string;
  sessionId: string;
  userId: string;
  expiresAt: Date;
};
