import * as fetcher from './fetcher';
import {
  mapDeleteLibrariesLibraryIdBooksBookIdModelToRequestDTO,
  mapDeleteLibrariesLibraryIdBooksBookIdResponseDTOToModel,
} from './mapper';

import type { DeleteLibrariesLibraryIdBooksBookId } from './repository.types';

export const deleteLibrariesLibraryIdBooksBookId: DeleteLibrariesLibraryIdBooksBookId = async (
  model,
) => {
  const { libraryId, bookId } = mapDeleteLibrariesLibraryIdBooksBookIdModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteLibrariesLibraryIdBooksBookId({
    pathParams: [
      { name: 'libraryId', value: libraryId },
      { name: 'bookId', value: bookId },
    ],
  });

  return mapDeleteLibrariesLibraryIdBooksBookIdResponseDTOToModel(responseDTO);
};
