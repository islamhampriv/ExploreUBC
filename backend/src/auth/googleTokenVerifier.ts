import {
  gaxios,
  OAuth2Client,
  type TokenPayload,
  type VerifyIdTokenOptions,
} from 'google-auth-library';

import type { GoogleUserIdentity } from '../users/userTypes';
import {
  GoogleTokenVerificationUnavailableError,
  InvalidGoogleTokenError,
} from './authError';

const GOOGLE_ISSUERS = new Set(['accounts.google.com', 'https://accounts.google.com']);

export interface GoogleTokenVerifier {
  verify(idToken: string): Promise<GoogleUserIdentity>;
}

type VerificationTicket = {
  getPayload(): TokenPayload | undefined;
};

export type GoogleVerificationClient = {
  verifyIdToken(options: VerifyIdTokenOptions): Promise<VerificationTicket>;
};

type Clock = () => Date;

export class GoogleIdTokenVerifier implements GoogleTokenVerifier {
  constructor(
    private readonly clientId: string,
    private readonly client: GoogleVerificationClient = new OAuth2Client(),
    private readonly clock: Clock = () => new Date(),
  ) {}

  async verify(idToken: string): Promise<GoogleUserIdentity> {
    if (idToken.trim() === '') {
      throw new InvalidGoogleTokenError();
    }

    try {
      const ticket = await this.client.verifyIdToken({
        idToken,
        audience: this.clientId,
      });
      const payload = ticket.getPayload();

      if (!this.hasValidClaims(payload)) {
        throw new InvalidGoogleTokenError();
      }

      return {
        googleSubject: payload.sub,
        email: payload.email ?? '',
        displayName: payload.name ?? payload.email ?? '',
        profileImageUrl: payload.picture ?? null,
      };
    } catch (error: unknown) {
      if (error instanceof InvalidGoogleTokenError) {
        throw error;
      }

      if (error instanceof gaxios.GaxiosError) {
        throw new GoogleTokenVerificationUnavailableError();
      }

      // Do not include the token or the verifier library's error details.
      throw new InvalidGoogleTokenError();
    }
  }

  private hasValidClaims(
    payload: TokenPayload | undefined,
  ): payload is TokenPayload & { sub: string; exp: number } {
    if (
      payload === undefined ||
      typeof payload.sub !== 'string' ||
      payload.sub.trim() === '' ||
      typeof payload.exp !== 'number' ||
      payload.exp * 1000 <= this.clock().getTime() ||
      !GOOGLE_ISSUERS.has(payload.iss)
    ) {
      return false;
    }

    const audiences = Array.isArray(payload.aud) ? payload.aud : [payload.aud];
    return audiences.includes(this.clientId);
  }
}
