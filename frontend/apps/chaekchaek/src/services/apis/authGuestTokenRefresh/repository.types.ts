import type { RequestContext } from '@/services/context/requestContext';

export interface PostAuthGuestTokenRefreshCommand {}

export type PostAuthGuestTokenRefresh = (
  command: PostAuthGuestTokenRefreshCommand,
  context: RequestContext,
) => Promise<{
  nickname: string;
  guestToken: string;
  expiresAt: string;
  actorId: number;
  actorType: 'MEMBER' | 'GUEST';
}>;
