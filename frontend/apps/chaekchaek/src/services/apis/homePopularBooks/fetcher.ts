import { instance } from '@/services/core/http';

import type { GetHomePopularBooksRequestDto, GetHomePopularBooksResponseDto } from './dto';

export const getHomePopularBooks =
  async ({}: GetHomePopularBooksRequestDto): Promise<GetHomePopularBooksResponseDto> => {
    const response = await instance('/api/v1/home/popular-books', {
      method: 'get',
    });

    return response.data;
  };
