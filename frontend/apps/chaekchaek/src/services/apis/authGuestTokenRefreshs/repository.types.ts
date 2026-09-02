import type { RequestContext } from '@/services/context/requestContext';

export interface PostAuthGuestTokenRefreshsCommand {}

export type PostAuthGuestTokenRefreshs = (
  command: PostAuthGuestTokenRefreshsCommand,
  context: RequestContext,
) => Promise<{
  nickname: string;
  guestToken: string;
  expiresAt: string;
}>;
