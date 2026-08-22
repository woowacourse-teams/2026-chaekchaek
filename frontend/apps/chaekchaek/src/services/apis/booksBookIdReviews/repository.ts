import * as fetcher from './fetcher';
import {
  mapGetBooksBookIdReviewsModelToRequestDTO,
  mapGetBooksBookIdReviewsResponseDTOToModel,
} from './mapper';

import type { GetBooksBookIdReviews } from './repository.types';

export const getBooksBookIdReviews: GetBooksBookIdReviews = async (model) => {
  const { page, feed, sort, bookId } = mapGetBooksBookIdReviewsModelToRequestDTO(model);

  const responseDTO = await fetcher.getBooksBookIdReviews({
    query: { page, feed, sort },
    pathParams: [{ name: 'bookId', value: bookId }],
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
