import * as fetcher from './fetcher';
import {
  mapPutBooksByIsbnIsbn13RatingModelToRequestDTO,
  mapPutBooksByIsbnIsbn13RatingResponseDTOToModel,
} from './mapper';

import type { PutBooksByIsbnIsbn13Rating } from './repository.types';

export const putBooksByIsbnIsbn13Rating: PutBooksByIsbnIsbn13Rating = async (model) => {
  const { isbn13, rating } = mapPutBooksByIsbnIsbn13RatingModelToRequestDTO(model);

  const responseDTO = await fetcher.putBooksByIsbnIsbn13Rating({
    pathParams: [{ name: 'isbn13', value: isbn13 }],
    data: { rating },
  });

  return mapPutBooksByIsbnIsbn13RatingResponseDTOToModel(responseDTO);
};
import {
  mapDeleteBooksByIsbnIsbn13RatingModelToRequestDTO,
  mapDeleteBooksByIsbnIsbn13RatingResponseDTOToModel,
} from './mapper';

import type { DeleteBooksByIsbnIsbn13Rating } from './repository.types';

export const deleteBooksByIsbnIsbn13Rating: DeleteBooksByIsbnIsbn13Rating = async (model) => {
  const { isbn13 } = mapDeleteBooksByIsbnIsbn13RatingModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteBooksByIsbnIsbn13Rating({
    pathParams: [{ name: 'isbn13', value: isbn13 }],
  });

  return mapDeleteBooksByIsbnIsbn13RatingResponseDTOToModel(responseDTO);
};
