import { instance } from '@/services/core/http';

import type {
  PutBooksByIsbnIsbn13RatingRequestDto,
  PutBooksByIsbnIsbn13RatingResponseDto,
} from './dto';

export const putBooksByIsbnIsbn13Rating = async ({
  pathParams: [{ value: isbn13 }],
  data: { rating },
}: PutBooksByIsbnIsbn13RatingRequestDto): Promise<PutBooksByIsbnIsbn13RatingResponseDto> => {
  const response = await instance(`/api/v1/books/by-isbn/${isbn13}/rating`, {
    method: 'put',
    data: { rating },
  });

  return response.data;
};

import type {
  DeleteBooksByIsbnIsbn13RatingRequestDto,
  DeleteBooksByIsbnIsbn13RatingResponseDto,
} from './dto';

export const deleteBooksByIsbnIsbn13Rating = async ({
  pathParams: [{ value: isbn13 }],
}: DeleteBooksByIsbnIsbn13RatingRequestDto): Promise<DeleteBooksByIsbnIsbn13RatingResponseDto> => {
  const response = await instance(`/api/v1/books/by-isbn/${isbn13}/rating`, {
    method: 'delete',
  });

  return response.data;
};
