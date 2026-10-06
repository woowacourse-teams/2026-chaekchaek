import type { RequestContext } from '@/services/context/requestContext';

export interface GetAuthGuestTokenParams {}

export type GetAuthGuestToken = (
  params: GetAuthGuestTokenParams,
  context: RequestContext,
) => Promise<{
  actorType: 'MEMBER' | 'GUEST';
  actorId: number;
  nickname: string;
  expiresAt: string;
}>;

export interface PostAuthGuestTokenCommand {}

export type PostAuthGuestToken = (command: PostAuthGuestTokenCommand) => Promise<{
  guestToken: string;
  nickname: string;
  expiresAt: string;
  actorId: number;
  actorType: 'MEMBER' | 'GUEST';
}>;
