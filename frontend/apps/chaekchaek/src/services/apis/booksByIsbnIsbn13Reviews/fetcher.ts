import { instance } from '@/services/core/http';

import type {
  PostBooksByIsbnIsbn13ReviewsRequestDto,
  PostBooksByIsbnIsbn13ReviewsResponseDto,
} from './dto';

export const postBooksByIsbnIsbn13Reviews = async ({
  pathParams: [{ value: isbn13 }],
  data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
}: PostBooksByIsbnIsbn13ReviewsRequestDto): Promise<PostBooksByIsbnIsbn13ReviewsResponseDto> => {
  const response = await instance(`/api/v1/books/by-isbn/${isbn13}/reviews`, {
    method: 'post',
    // pathParams: [{ name: 'isbn13', value: isbn13 }],
    data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
  });

  return response.data;
};
