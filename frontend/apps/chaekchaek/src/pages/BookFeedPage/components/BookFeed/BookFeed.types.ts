export type BookFeedReview = {
  reviewId: number;
  isbn13: string;
  bookCoverImageUrl: string;
  bookTitle: string;
  author: {
    memberId?: number | null;
    actorType: 'MEMBER' | 'GUEST';
    profileStatus: 'AVAILABLE' | 'UNAVAILABLE' | 'WITHDRAWN';
    displayName: string;
    profileImageUrl: string;
    mine: boolean;
  };
  createdAt: string;
  isSpoiler: boolean;
  content: string;
  quote?: string;
  replyCount: number;
  bookAuthors: string[];
  likedByMe: boolean;
  likeCount: number;
};

export type BookFeedProps = {
  review: BookFeedReview;
  onFeedRefresh?: () => void;
};
