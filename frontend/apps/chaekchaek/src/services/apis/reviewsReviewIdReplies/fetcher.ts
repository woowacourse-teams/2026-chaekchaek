import { instance } from '@/services/core/http';
import { createFetcherRequestHeaders } from '@/services/context/requestHeaders';

import type {
  GetReviewsReviewIdRepliesRequestDto,
  GetReviewsReviewIdRepliesResponseDto,
} from './dto';

export const getReviewsReviewIdReplies = async ({
  pathParams: [{ value: reviewId }],
  query: { page },
}: GetReviewsReviewIdRepliesRequestDto): Promise<GetReviewsReviewIdRepliesResponseDto> => {
  const response = await instance(`/api/v1/reviews/${reviewId}/replies`, {
    method: 'get',
    //  pathParams: [{ name: 'reviewId', value: reviewId }],
    query: { page },
  });

  return response.data;
};

import type {
  PostReviewsReviewIdRepliesRequestDto,
  PostReviewsReviewIdRepliesResponseDto,
} from './dto';

export const postReviewsReviewIdReplies = async ({
  pathParams: [{ value: reviewId }],
  data: { content },
  headers: { 'X-Guest-Token': guestToken } = {},
}: PostReviewsReviewIdRepliesRequestDto): Promise<PostReviewsReviewIdRepliesResponseDto> => {
  const requestHeaders = createFetcherRequestHeaders({ 'X-Guest-Token': guestToken });

  const response = await instance(`/api/v1/reviews/${reviewId}/replies`, {
    method: 'post',
    //  pathParams: [{ name: 'reviewId', value: reviewId }]
    data: { content },
    headers: requestHeaders,
  });

  return response.data;
};
