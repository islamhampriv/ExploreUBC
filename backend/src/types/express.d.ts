import type { AuthContext } from '../auth/authMiddleware';

declare global {
  namespace Express {
    interface Request {
      auth?: AuthContext;
    }
  }
}

export {};
