export type BookReview = {
  reviewId: number;
  author: {
    displayName: string;
    profileImageUrl: string;
  };
  createdAt: string;
  content: string;
  quote?: string;
  likedByMe: boolean;
  likeCount: number;
  replyCount: number;
  deleted: boolean;
};

export type BookReviewsProps = {
  reviewSort: string;
  reviewFeed: string;
  reviewCount: number | undefined;
  reviews: BookReview[] | undefined;
  onReviewSortChange: (reviewSort: string) => void;
  onReviewFeedChange: (reviewFeed: string) => void;
};
