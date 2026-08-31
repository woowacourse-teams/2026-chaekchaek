export interface PostAuthGuestTokenCommand {}

export type PostAuthGuestToken = (command: PostAuthGuestTokenCommand) => Promise<{
  guestToken: string;
  nickname: string;
  expiresAt: string;
}>;
