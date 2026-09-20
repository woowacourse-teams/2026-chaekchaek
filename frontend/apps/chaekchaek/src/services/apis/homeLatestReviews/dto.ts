import type { ResponseDto } from '@/services/apis/api.types';

export interface GetHomeLatestReviewsRequestDto {}

export type GetHomeLatestReviewsResponseDto = ResponseDto<{
  reviews: {
    createdAt: string;
    replyCount: number;
    author: {
      mine: boolean;
      actorType: string;
      displayName: string;
      profileStatus: string;
      anonymous: boolean;
      profileImageUrl: string;
      memberId: number;
    };
    isbn13: string;
    bookCoverImageUrl: string;
    content: string;
    bookTitle: string;
    bookId: number;
  }[];
}>;
