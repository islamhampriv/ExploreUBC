import type { UserRepository } from '../users/userRepository';
import * as SessionError from '../session/sessionError';
import type { SessionService } from '../session/sessionService';
import type { GoogleTokenVerifier } from './googleTokenVerifier';
import type { CurrentSessionResult, GoogleAuthResult } from './authTypes';

export class AuthService {
  constructor(
    private readonly googleTokenVerifier: GoogleTokenVerifier,
    private readonly userRepository: UserRepository,
    private readonly sessionService: SessionService,
  ) {}

  async authenticateWithGoogle(idToken: string): Promise<GoogleAuthResult> {
    const identity = await this.googleTokenVerifier.verify(idToken);
    const user = await this.userRepository.findOrCreateByGoogleIdentity(identity);
    const session = await this.sessionService.createSession(user.id);

    return {
      sessionToken: session.token,
      expiresAt: session.expiresAt,
      onboardingRequired: user.onboardingCompletedAt === null,
      user,
    };
  }

  async getCurrentSession(sessionToken: string): Promise<CurrentSessionResult> {
    const session = await this.sessionService.refreshSession(sessionToken);
    const user = await this.userRepository.findUser(session.userId);

    if (user === null) {
      await this.sessionService.revokeSession(sessionToken);
      throw new SessionError.InvalidSessionError();
    }

    return {
      expiresAt: session.expiresAt,
      onboardingRequired: user.onboardingCompletedAt === null,
      user,
    };
  }

  async logout(sessionToken: string): Promise<void> {
    await this.sessionService.validateSession(sessionToken);
    await this.sessionService.revokeSession(sessionToken);
  }
}
