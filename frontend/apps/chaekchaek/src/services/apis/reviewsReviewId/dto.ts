import type { ResponseDto } from '@/services/apis/api.types';

export interface DeleteReviewsReviewIdRequestDto {
  pathParams: [{ name: 'reviewId'; value: number }];
}

export type DeleteReviewsReviewIdResponseDto = ResponseDto<undefined>;

import type { RequestHeaders } from '@/services/context/requestHeaders';

export interface PatchReviewsReviewIdRequestDto {
  pathParams: [{ name: 'reviewId'; value: number }];
  data: {
    chapter?: string | undefined;
    isSpoiler?: boolean | undefined;
    quote?: string | undefined;
    totalPages?: number | undefined;
    currentPage?: number | undefined;
    content: string;
  };
  headers?: RequestHeaders;
}

export type PatchReviewsReviewIdResponseDto = ResponseDto<{
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
