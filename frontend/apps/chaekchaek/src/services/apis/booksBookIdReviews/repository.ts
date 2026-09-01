import * as fetcher from './fetcher';
import { createRepositoryRequestHeaders } from '@/services/context/requestHeaders';
import {
  mapGetBooksBookIdReviewsModelToRequestDTO,
  mapGetBooksBookIdReviewsResponseDTOToModel,
} from './mapper';

import type { GetBooksBookIdReviews } from './repository.types';

export const getBooksBookIdReviews: GetBooksBookIdReviews = async (model, context) => {
  const { page, feed, sort, bookId } = mapGetBooksBookIdReviewsModelToRequestDTO(model);

  const { guestToken } = context ?? {};
  const headers = createRepositoryRequestHeaders({ guestToken });

  const responseDTO = await fetcher.getBooksBookIdReviews({
    query: { page, feed, sort },
    pathParams: [{ name: 'bookId', value: bookId }],
    headers,
  });

  return mapGetBooksBookIdReviewsResponseDTOToModel(responseDTO);
};
import {
  mapPostBooksBookIdReviewsModelToRequestDTO,
  mapPostBooksBookIdReviewsResponseDTOToModel,
} from './mapper';

import type { PostBooksBookIdReviews } from './repository.types';

export const postBooksBookIdReviews: PostBooksBookIdReviews = async (model) => {
  const { bookId, chapter, isSpoiler, quote, totalPages, currentPage, content } =
    mapPostBooksBookIdReviewsModelToRequestDTO(model);

  const responseDTO = await fetcher.postBooksBookIdReviews({
    pathParams: [{ name: 'bookId', value: bookId }],
    data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
  });

  return mapPostBooksBookIdReviewsResponseDTOToModel(responseDTO);
};
