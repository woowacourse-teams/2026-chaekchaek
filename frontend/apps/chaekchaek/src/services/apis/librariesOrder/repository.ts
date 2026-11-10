import * as fetcher from './fetcher';
import {
  mapPatchLibrariesOrderModelToRequestDTO,
  mapPatchLibrariesOrderResponseDTOToModel,
} from './mapper';

import type { PatchLibrariesOrder } from './repository.types';

export const patchLibrariesOrder: PatchLibrariesOrder = async (model) => {
  const { libraryIds } = mapPatchLibrariesOrderModelToRequestDTO(model);

  const responseDTO = await fetcher.patchLibrariesOrder({
    data: { libraryIds },
  });

  return mapPatchLibrariesOrderResponseDTOToModel(responseDTO);
};
