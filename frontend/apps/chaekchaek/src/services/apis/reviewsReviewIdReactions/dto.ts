import type { ResponseDto } from '@/services/apis/api.types';

export interface PostReviewsReviewIdReactionsRequestDto {
  pathParams: [{ name: 'reviewId'; value: number }];
}

export type PostReviewsReviewIdReactionsResponseDto = ResponseDto<undefined>;
export interface DeleteReviewsReviewIdReactionsRequestDto {
  pathParams: [{ name: 'reviewId'; value: number }];
}

export type DeleteReviewsReviewIdReactionsResponseDto = ResponseDto<undefined>;
