export interface DeleteReviewsReviewIdParams {
  reviewId: number;
}

export type DeleteReviewsReviewId = (params: DeleteReviewsReviewIdParams) => Promise<undefined>;
export interface PatchReviewsReviewIdParams {
  reviewId: number;
  chapter: string;
  isSpoiler: boolean;
  quote: string;
  totalPages: number;
  currentPage: number;
  content: string;
}

export type PatchReviewsReviewId = (params: PatchReviewsReviewIdParams) => Promise<{
  chapter: string;
  author: {
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
  quote: string;
  currentPage: number;
  reviewId: number;
}>;
