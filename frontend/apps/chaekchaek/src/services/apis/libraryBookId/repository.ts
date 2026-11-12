import * as fetcher from './fetcher';
import {
  mapDeleteLibraryBookIdModelToRequestDTO,
  mapDeleteLibraryBookIdResponseDTOToModel,
} from './mapper';

import type { DeleteLibraryBookId } from './repository.types';

export const deleteLibraryBookId: DeleteLibraryBookId = async (model) => {
  const { bookId } = mapDeleteLibraryBookIdModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteLibraryBookId({
    pathParams: [{ name: 'bookId', value: bookId }],
  });

  return mapDeleteLibraryBookIdResponseDTOToModel(responseDTO);
};
