import type { ResponseDto } from '@/services/apis/api.types';

export interface GetReviewsReviewIdRepliesRequestDto {
  pathParams: [{ name: 'reviewId'; value: number }];
  query: { page: number };
}

export type GetReviewsReviewIdRepliesResponseDto = ResponseDto<{
  nextPage: number;
  totalCount: number;
  items: {
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
}>;
export interface PostReviewsReviewIdRepliesRequestDto {
  pathParams: [{ name: 'reviewId'; value: number }];
  data: { content: string };
}

export type PostReviewsReviewIdRepliesResponseDto = ResponseDto<undefined>;
