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
import {
  mapPatchLibraryBookIdModelToRequestDTO,
  mapPatchLibraryBookIdResponseDTOToModel,
} from './mapper';

import type { PatchLibraryBookId } from './repository.types';

export const patchLibraryBookId: PatchLibraryBookId = async (model) => {
  const { bookId, status, currentPage } = mapPatchLibraryBookIdModelToRequestDTO(model);

  const responseDTO = await fetcher.patchLibraryBookId({
    pathParams: [{ name: 'bookId', value: bookId }],
    data: { status, currentPage },
  });

  return mapPatchLibraryBookIdResponseDTOToModel(responseDTO);
};
