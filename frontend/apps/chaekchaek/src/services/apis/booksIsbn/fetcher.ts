import { requestAjax } from '@/services/core/http';

import type { GetBooksIsbnRequestDto, GetBooksIsbnResponseDto } from './dto';

export const getBooksIsbn = async ({
  pathParams: [{ value: bookId }],
}: GetBooksIsbnRequestDto): Promise<GetBooksIsbnResponseDto> => {
  const response = await requestAjax('/api/v1/books', {
    method: 'get',
    pathParams: [{ name: 'bookId', value: bookId }],
  });

  return response.data;
};
