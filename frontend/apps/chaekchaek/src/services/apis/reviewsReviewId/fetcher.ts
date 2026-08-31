import { instance } from '@/services/core/http';

import type { DeleteReviewsReviewIdRequestDto, DeleteReviewsReviewIdResponseDto } from './dto';

export const deleteReviewsReviewId = async ({
  pathParams: [{ value: reviewId }],
}: DeleteReviewsReviewIdRequestDto): Promise<DeleteReviewsReviewIdResponseDto> => {
  const response = await instance(`/api/v1/reviews/${reviewId}`, {
    method: 'delete',
    //  pathParams: [{ name: 'reviewId', value: reviewId }]
  });

  return response.data;
};

import { createFetcherRequestHeaders } from '@/services/context/requestHeaders';

import type { PatchReviewsReviewIdRequestDto, PatchReviewsReviewIdResponseDto } from './dto';

export const patchReviewsReviewId = async ({
  pathParams: [{ value: reviewId }],
  data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
  headers: { 'X-Guest-Token': guestToken } = {},
}: PatchReviewsReviewIdRequestDto): Promise<PatchReviewsReviewIdResponseDto> => {
  const requestHeaders = createFetcherRequestHeaders({ 'X-Guest-Token': guestToken });

  const response = await instance(`/api/v1/reviews/${reviewId}`, {
    method: 'patch',
    // pathParams: [{ name: 'reviewId', value: reviewId }],
    data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
    headers: requestHeaders,
  });

  return response.data;
};
