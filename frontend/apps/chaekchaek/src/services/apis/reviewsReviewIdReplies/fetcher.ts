import { requestAjax } from '@/services/core/http';

import type {
  GetReviewsReviewIdRepliesRequestDto,
  GetReviewsReviewIdRepliesResponseDto,
} from './dto';

export const getReviewsReviewIdReplies = async ({
  pathParams: [{ value: reviewId }],
  query: { page },
}: GetReviewsReviewIdRepliesRequestDto): Promise<GetReviewsReviewIdRepliesResponseDto> => {
  const response = await requestAjax(`/api/v1/reviews/${reviewId}/replies`, {
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
}: PostReviewsReviewIdRepliesRequestDto): Promise<PostReviewsReviewIdRepliesResponseDto> => {
  const response = await requestAjax(`/api/v1/reviews/${reviewId}/replies`, {
    method: 'post',
    //  pathParams: [{ name: 'reviewId', value: reviewId }]
    data: { content },
  });

  return response.data;
};
