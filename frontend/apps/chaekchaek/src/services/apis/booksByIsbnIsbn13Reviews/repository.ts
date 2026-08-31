import * as fetcher from './fetcher';
import {
  mapPostBooksByIsbnIsbn13ReviewsModelToRequestDTO,
  mapPostBooksByIsbnIsbn13ReviewsResponseDTOToModel,
} from './mapper';

import type { PostBooksByIsbnIsbn13Reviews } from './repository.types';

export const postBooksByIsbnIsbn13Reviews: PostBooksByIsbnIsbn13Reviews = async (model) => {
  const { isbn13, chapter, isSpoiler, quote, totalPages, currentPage, content, guestToken } =
    mapPostBooksByIsbnIsbn13ReviewsModelToRequestDTO(model);

  const responseDTO = await fetcher.postBooksByIsbnIsbn13Reviews({
    pathParams: [{ name: 'isbn13', value: isbn13 }],
    data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
    headers: {
      'X-Guest-Token': guestToken,
    },
  });

  return mapPostBooksByIsbnIsbn13ReviewsResponseDTOToModel(responseDTO);
};
