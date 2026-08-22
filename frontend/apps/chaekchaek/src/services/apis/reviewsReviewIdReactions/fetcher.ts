import { requestAjax } from '@/services/core/http';

import type {
  PostReviewsReviewIdReactionsRequestDto,
  PostReviewsReviewIdReactionsResponseDto,
} from './dto';

export const postReviewsReviewIdReactions = async ({
  pathParams: [{ value: reviewId }],
}: PostReviewsReviewIdReactionsRequestDto): Promise<PostReviewsReviewIdReactionsResponseDto> => {
  const response = await requestAjax(`/api/v1/reviews/${reviewId}/reactions`, {
    method: 'post',
    // pathParams: [{ name: 'reviewId', value: reviewId }],
  });

  return response.data;
};

import type {
  DeleteReviewsReviewIdReactionsRequestDto,
  DeleteReviewsReviewIdReactionsResponseDto,
} from './dto';

export const deleteReviewsReviewIdReactions = async ({
  pathParams: [{ value: reviewId }],
}: DeleteReviewsReviewIdReactionsRequestDto): Promise<DeleteReviewsReviewIdReactionsResponseDto> => {
  const response = await requestAjax(`/api/v1/reviews/${reviewId}/reactions`, {
    method: 'delete',
    // pathParams: [{ name: 'reviewId', value: reviewId }],
  });

  return response.data;
};
