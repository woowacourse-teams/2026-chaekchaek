import type { RequestContext } from '@/services/context/requestContext';

export interface GetBooksBookIdReviewsParams {
  bookId: number;
  page: number;
  feed: string;
  sort: string;
}

export type GetBooksBookIdReviews = (
  params: GetBooksBookIdReviewsParams,
  context?: RequestContext,
) => Promise<{
  nextPage: number;
  totalCount: number;
  items: {
    chapter: string;
    author: {
      memberId?: number | null;
      actorType?: 'MEMBER' | 'GUEST';
      profileStatus?: 'AVAILABLE' | 'UNAVAILABLE' | 'WITHDRAWN';
      mine: boolean;
      displayName: string;
      anonymous: boolean;
      profileImageUrl: string;
    };
    likeCount: number;
    content: string;
    createdAt: string;
    recentReplies: {
      createdAt: string;
      likedByMe: boolean;
      deleted: boolean;
      author: {
        mine: boolean;
        displayName: string;
        anonymous: boolean;
        profileImageUrl: string;
      };
      replyId: number;
      likeCount: number;
      content: string;
    }[];
    replyCount: number;
    isSpoiler: boolean;
    likedByMe: boolean;
    deleted: boolean;
    quote?: string;
    currentPage: number;
    reviewId: number;
  }[];
}>;
export interface PostBooksBookIdReviewsCommand {
  bookId: number;
  content: string;
  chapter?: string | undefined;
  isSpoiler?: boolean | undefined;
  quote?: string | undefined;
  totalPages?: number | undefined;
  currentPage?: number | undefined;
}

export type PostBooksBookIdReviews = (command: PostBooksBookIdReviewsCommand) => Promise<undefined>;
