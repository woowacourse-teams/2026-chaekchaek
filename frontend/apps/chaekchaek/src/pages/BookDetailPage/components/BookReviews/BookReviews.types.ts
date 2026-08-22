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
  sort: string;
  feed: string;
  count: number | undefined;
  reviews: BookReview[] | undefined;
  onSortChange: (sort: string) => void;
  onFeedChange: (feed: string) => void;
};
