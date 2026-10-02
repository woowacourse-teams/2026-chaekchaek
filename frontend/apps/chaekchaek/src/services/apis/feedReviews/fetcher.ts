import { instance } from '@/services/core/http';

import type { GetFeedReviewsRequestDto, GetFeedReviewsResponseDto } from './dto';

export const getFeedReviews = async ({
  query: { page },
}: GetFeedReviewsRequestDto): Promise<GetFeedReviewsResponseDto> => {
  const response = await instance('/api/v1/feed/reviews', {
    method: 'get',
    query: { page },
  });

  return response.data;
};
