import { instance } from '@/services/core/http';

import type { GetBooksBookIdReviewsRequestDto, GetBooksBookIdReviewsResponseDto } from './dto';

export const getBooksBookIdReviews = async ({
  pathParams: [{ value: bookId }],
  query: { page, feed, sort },
}: GetBooksBookIdReviewsRequestDto): Promise<GetBooksBookIdReviewsResponseDto> => {
  const response = await instance(`/api/v1/books/${bookId}/reviews`, {
    method: 'get',
    // pathParams: [{ name: 'bookId', value: bookId }],
    query: { page, feed, sort },
  });

  return response.data;
};

import type { PostBooksBookIdReviewsRequestDto, PostBooksBookIdReviewsResponseDto } from './dto';

export const postBooksBookIdReviews = async ({
  pathParams: [{ value: bookId }],
  data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
}: PostBooksBookIdReviewsRequestDto): Promise<PostBooksBookIdReviewsResponseDto> => {
  const response = await instance(`/api/v1/books/${bookId}/reviews`, {
    method: 'post',
    // pathParams: [{ name: 'bookId', value: bookId }],
    data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
  });

  return response.data;
};
