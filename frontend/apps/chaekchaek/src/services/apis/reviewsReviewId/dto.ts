import type { ResponseDto } from '@/services/apis/api.types';
import type { RequestHeaders } from '@/services/context/requestHeaders';

export interface DeleteReviewsReviewIdRequestDto {
  pathParams: [{ name: 'reviewId'; value: number }];
  headers?: RequestHeaders;
}

export type DeleteReviewsReviewIdResponseDto = ResponseDto<undefined>;

export interface PatchReviewsReviewIdRequestDto {
  pathParams: [{ name: 'reviewId'; value: number }];
  data: {
    isSpoiler?: boolean | undefined;
    quote?: string | undefined;
    totalPages?: number | undefined;
    content: string;
  };
  headers?: RequestHeaders;
}

export type PatchReviewsReviewIdResponseDto = ResponseDto<{
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
  reviewId: number;
}>;
