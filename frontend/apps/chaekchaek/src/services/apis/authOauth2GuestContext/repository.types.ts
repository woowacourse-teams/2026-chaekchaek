import type { RequestContext } from '@/services/context/requestContext';

export interface PostAuthOauth2GuestContextCommand {}

export type PostAuthOauth2GuestContext = (
  command: PostAuthOauth2GuestContextCommand,
  contexT: RequestContext,
) => Promise<undefined>;
