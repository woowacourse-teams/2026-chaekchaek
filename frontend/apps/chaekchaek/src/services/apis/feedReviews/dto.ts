import type { ResponseDto } from '@/services/apis/api.types';

export interface GetFeedReviewsRequestDto {
  query: { page: number };
}

export type GetFeedReviewsResponseDto = ResponseDto<{
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
    currentPage?: number;
  }[];
  nextPage: number;
  totalCount: number;
}>;
