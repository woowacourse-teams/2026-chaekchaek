import * as fetcher from './fetcher';
import {
  mapPostBooksByIsbnIsbn13ReviewsModelToRequestDTO,
  mapPostBooksByIsbnIsbn13ReviewsResponseDTOToModel,
} from './mapper';

import type { PostBooksByIsbnIsbn13Reviews } from './repository.types';

export const postBooksByIsbnIsbn13Reviews: PostBooksByIsbnIsbn13Reviews = async (
  model,
  context,
) => {
  const { isbn13, isSpoiler, quote, totalPages, content } =
    mapPostBooksByIsbnIsbn13ReviewsModelToRequestDTO(model);

  const guestToken = context?.guestToken;

  const responseDTO = await fetcher.postBooksByIsbnIsbn13Reviews({
    pathParams: [{ name: 'isbn13', value: isbn13 }],
    data: { isSpoiler, quote, totalPages, content },
    ...(guestToken && {
      headers: {
        'X-Guest-Token': guestToken,
      },
    }),
  });

  return mapPostBooksByIsbnIsbn13ReviewsResponseDTOToModel(responseDTO);
};
