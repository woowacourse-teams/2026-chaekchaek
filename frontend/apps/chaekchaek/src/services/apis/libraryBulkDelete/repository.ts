import * as fetcher from './fetcher';
import {
  mapPostLibraryBulkDeleteModelToRequestDTO,
  mapPostLibraryBulkDeleteResponseDTOToModel,
} from './mapper';

import type { PostLibraryBulkDelete } from './repository.types';

export const postLibraryBulkDelete: PostLibraryBulkDelete = async (model) => {
  const { bookIds } = mapPostLibraryBulkDeleteModelToRequestDTO(model);

  const responseDTO = await fetcher.postLibraryBulkDelete({
    data: { bookIds },
  });

  return mapPostLibraryBulkDeleteResponseDTOToModel(responseDTO);
};
