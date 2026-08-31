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
  chapter?: string;
  author: {
    displayName: string;
    profileImageUrl: string;
    mine: boolean;
  };
  createdAt: string;
  content: string;
  quote?: string;
  currentPage?: number;
  likedByMe: boolean;
  likeCount: number;
  replyCount: number;
  deleted: boolean;
  isSpoiler: boolean;
  recentReplies: RecentReply[];
};

export type BookReviewsProps = {
  bookId: number;
  isbn: string;
  feed: FeedType;
  sort: SortType;
  count: number | undefined;
  reviews: BookReview[] | undefined;
  onFeedChange: (feed: FeedType) => void;
  onSortChange: (sort: SortType) => void;
  onReviewsRefresh: () => void;
};
