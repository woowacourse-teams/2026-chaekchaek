export interface GetFeedReviewsParams {
  page: number;
}

export type GetFeedReviews = (params: GetFeedReviewsParams) => Promise<{
  reviews: {
    createdAt: string;
    replyCount: number;
    isSpoiler: boolean;
    author: {
      mine: boolean;
      actorType: 'MEMBER' | 'GUEST';
      displayName: string;
      profileStatus: string;
      anonymous: boolean;
      profileImageUrl: string;
      memberId: number;
    };
    isbn13: string;
    bookCoverImageUrl: string;
    reviewId: number;
    content: string;
    quote?: string;
    bookTitle: string;
    bookAuthors: string[];
    bookId: number;
    likeCount: number;
    likedByMe: boolean;
  }[];
  nextPage: number;
  totalCount: number;
}>;
