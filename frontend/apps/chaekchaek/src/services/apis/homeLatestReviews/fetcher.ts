import { instance } from '@/services/core/http';

import type { GetHomeLatestReviewsRequestDto, GetHomeLatestReviewsResponseDto } from './dto';

export const getHomeLatestReviews =
  async ({}: GetHomeLatestReviewsRequestDto): Promise<GetHomeLatestReviewsResponseDto> => {
    const response = await instance('/api/v1/home/latest-reviews', {
      method: 'get',
    });

    return response.data;
  };
