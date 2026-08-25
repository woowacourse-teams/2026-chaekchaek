export type FeedType = 'ALL' | 'MINE';
export type SortType = 'LATEST' | 'OLDEST' | 'POPULAR' | 'PAGE';

export type RecentReply = {
  replyId: number;
  author: {
    displayName: string;
    profileImageUrl: string;
  };
  createdAt: string;
  content: string;
  likedByMe: boolean;
  likeCount: number;
  deleted: boolean;
};

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
  isSpoiler: boolean;
  recentReplies: RecentReply[];
};

export type BookReviewsProps = {
  bookId: number;
  feed: FeedType;
  sort: SortType;
  count: number | undefined;
  reviews: BookReview[] | undefined;
  isSpoilerVisible: boolean;
  onFeedChange: (feed: FeedType) => void;
  onSortChange: (sort: SortType) => void;
  onReviewsRefresh: () => void;
};
