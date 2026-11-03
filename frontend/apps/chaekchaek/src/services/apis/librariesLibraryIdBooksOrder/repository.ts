import * as fetcher from './fetcher';
import {
  mapPatchLibrariesLibraryIdBooksOrderModelToRequestDTO,
  mapPatchLibrariesLibraryIdBooksOrderResponseDTOToModel,
} from './mapper';

import type { PatchLibrariesLibraryIdBooksOrder } from './repository.types';

export const patchLibrariesLibraryIdBooksOrder: PatchLibrariesLibraryIdBooksOrder = async (
  model,
) => {
  const { libraryId, bookIds } = mapPatchLibrariesLibraryIdBooksOrderModelToRequestDTO(model);

  const responseDTO = await fetcher.patchLibrariesLibraryIdBooksOrder({
    pathParams: [{ name: 'libraryId', value: libraryId }],
    data: { bookIds },
  });

  return mapPatchLibrariesLibraryIdBooksOrderResponseDTOToModel(responseDTO);
};
