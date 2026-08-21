import { instance } from '@/services/core/http';

import type { GetBooksIsbnRequestDto, GetBooksIsbnResponseDto } from './dto';

export const getBooksIsbn = async ({
  pathParams: [{ value: isbn }],
}: GetBooksIsbnRequestDto): Promise<GetBooksIsbnResponseDto> => {
  const response = await instance('/api/v1/books/by-isbn', {
    method: 'get',
    pathParams: [{ name: 'isbn', value: isbn }],
  });

  return response.data;
};
