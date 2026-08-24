import * as fetcher from './fetcher';
import {
  mapPatchLibraryBulkStatusModelToRequestDTO,
  mapPatchLibraryBulkStatusResponseDTOToModel,
} from './mapper';

import type { PatchLibraryBulkStatus } from './repository.types';

export const patchLibraryBulkStatus: PatchLibraryBulkStatus = async (model) => {
  const { bookIds, status } = mapPatchLibraryBulkStatusModelToRequestDTO(model);

  const responseDTO = await fetcher.patchLibraryBulkStatus({
    data: { bookIds, status },
  });

  return mapPatchLibraryBulkStatusResponseDTOToModel(responseDTO);
};
