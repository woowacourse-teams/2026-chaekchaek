import { instance } from '@/services/core/http';
import { createFetcherRequestHeaders } from '@/services/context/requestHeaders';

import type {
  PostBooksByIsbnIsbn13ReviewsRequestDto,
  PostBooksByIsbnIsbn13ReviewsResponseDto,
} from './dto';

export const postBooksByIsbnIsbn13Reviews = async ({
  pathParams: [{ value: isbn13 }],
  data: { isSpoiler, quote, totalPages, content },
  headers,
}: PostBooksByIsbnIsbn13ReviewsRequestDto): Promise<PostBooksByIsbnIsbn13ReviewsResponseDto> => {
  const guestToken = headers?.['X-Guest-Token'];
  const requestHeaders = createFetcherRequestHeaders({ 'X-Guest-Token': guestToken });

  const response = await instance(`/api/v1/books/by-isbn/${isbn13}/reviews`, {
    method: 'post',
    // pathParams: [{ name: 'isbn13', value: isbn13 }],
    data: { isSpoiler, quote, totalPages, content },
    ...(guestToken && { headers: requestHeaders }),
  });

  return response.data;
};
