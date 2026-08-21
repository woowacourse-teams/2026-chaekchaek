import * as fetcher from './fetcher';
import {
  mapPutLibraryBookIdRatingModelToRequestDTO,
  mapPutLibraryBookIdRatingResponseDTOToModel,
} from './mapper';

import type { PutLibraryBookIdRating } from './repository.types';

export const putLibraryBookIdRating: PutLibraryBookIdRating = async (model) => {
  const { bookId, rating } = mapPutLibraryBookIdRatingModelToRequestDTO(model);

  const responseDTO = await fetcher.putLibraryBookIdRating({
    pathParams: [{ name: 'bookId', value: bookId }],
    data: { rating },
  });

  return mapPutLibraryBookIdRatingResponseDTOToModel(responseDTO);
};
import {
  mapDeleteLibraryBookIdRatingModelToRequestDTO,
  mapDeleteLibraryBookIdRatingResponseDTOToModel,
} from './mapper';

import type { DeleteLibraryBookIdRating } from './repository.types';

export const deleteLibraryBookIdRating: DeleteLibraryBookIdRating = async (model) => {
  const { bookId } = mapDeleteLibraryBookIdRatingModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteLibraryBookIdRating({
    pathParams: [{ name: 'bookId', value: bookId }],
  });

  return mapDeleteLibraryBookIdRatingResponseDTOToModel(responseDTO);
};
