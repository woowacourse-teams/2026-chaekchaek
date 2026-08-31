import { instance } from '@/services/core/http';
import { createFetcherRequestHeaders } from '@/services/context/requestHeaders';

import type { DeleteReviewsReviewIdRequestDto, DeleteReviewsReviewIdResponseDto } from './dto';

export const deleteReviewsReviewId = async ({
  pathParams: [{ value: reviewId }],
  headers: { 'X-Guest-Token': guestToken } = {},
}: DeleteReviewsReviewIdRequestDto): Promise<DeleteReviewsReviewIdResponseDto> => {
  const requestHeaders = createFetcherRequestHeaders({ 'X-Guest-Token': guestToken });

  const response = await instance(`/api/v1/reviews/${reviewId}`, {
    method: 'delete',
    //  pathParams: [{ name: 'reviewId', value: reviewId }]
    headers: requestHeaders,
  });

  return response.data;
};

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
