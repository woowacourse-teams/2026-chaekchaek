import type { RequestContext } from '@/services/context/requestContext';

export interface GetReviewsReviewIdRepliesParams {
  reviewId: number;
  page: number;
}

export type GetReviewsReviewIdReplies = (params: GetReviewsReviewIdRepliesParams) => Promise<{
  nextPage: number;
  totalCount: number;
  items: {
    createdAt: string;
    likedByMe: boolean;
    deleted: boolean;
    author: {
      mine: boolean;
      displayName: string;
      anonymous: boolean;
      profileImageUrl: string;
      actor: string;
      profileStatus: string;
      memberId: number;
    };
    replyId: number;
    likeCount: number;
    content: string;
  }[];
}>;
export interface PostReviewsReviewIdRepliesCommand {
  reviewId: number;
  content: string;
}

export type PostReviewsReviewIdReplies = (
  command: PostReviewsReviewIdRepliesCommand,
  context?: RequestContext,
) => Promise<undefined>;
