import * as fetcher from './fetcher';
import { mapGetLibraryModelToRequestDTO, mapGetLibraryResponseDTOToModel } from './mapper';

import type { GetLibrary } from './repository.types';

export const getLibrary: GetLibrary = async (model) => {
  const { page, status, sort } = mapGetLibraryModelToRequestDTO(model);

  const responseDTO = await fetcher.getLibrary({
    query: { page, status, sort },
  });

  return mapGetLibraryResponseDTOToModel(responseDTO);
};
import { mapPostLibraryModelToRequestDTO, mapPostLibraryResponseDTOToModel } from './mapper';

import type { PostLibrary } from './repository.types';

export const postLibrary: PostLibrary = async (model) => {
  const { isbn13, totalPages, status } = mapPostLibraryModelToRequestDTO(model);

  const responseDTO = await fetcher.postLibrary({
    data: { isbn13, totalPages, status },
  });

  return mapPostLibraryResponseDTOToModel(responseDTO);
};
