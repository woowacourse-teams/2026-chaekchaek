export interface GetAuthGuestTokenParams {}

export type GetAuthGuestToken = (params: GetAuthGuestTokenParams) => Promise<{
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
