import { instance } from '@/services/core/http';

import type { GetBooksRequestDto, GetBooksResponseDto } from './dto';

export const getBooks = async ({
  query: { query, page },
}: GetBooksRequestDto): Promise<GetBooksResponseDto> => {
  const response = await instance('/api/v1/books', {
    method: 'get',
    query: { query, page },
  });

  return response;
};
