import type { ResponseDto } from '@/services/apis/api.types';
import type { RequestHeaders } from '@/services/context/requestHeaders';

export interface GetBooksBookIdReviewsRequestDto {
  pathParams: [{ name: 'bookId'; value: number }];
  query: { page: number; feed: string; sort: 'LATEST' | 'OLDEST' | 'POPULAR' };
  headers?: RequestHeaders;
}

export type GetBooksBookIdReviewsResponseDto = ResponseDto<{
  nextPage: number;
  totalCount: number;
  items: {
    chapter: string;
    author: {
      memberId?: number | null;
      actorType?: 'MEMBER' | 'GUEST';
      profileStatus?: 'AVAILABLE' | 'UNAVAILABLE' | 'WITHDRAWN';
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
    quote?: string;
    reviewId: number;
  }[];
}>;
export interface PostBooksBookIdReviewsRequestDto {
  pathParams: [{ name: 'bookId'; value: number }];
  data: {
    content: string | undefined;
    isSpoiler?: boolean | undefined;
    quote?: string | undefined;
    totalPages?: number | undefined;
  };
  headers?: {
    'X-Guest-Token'?: string;
  };
}

export type PostBooksBookIdReviewsResponseDto = ResponseDto<undefined>;
