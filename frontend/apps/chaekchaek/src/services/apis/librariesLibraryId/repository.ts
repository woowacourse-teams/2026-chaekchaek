import * as fetcher from './fetcher';
import {
  mapDeleteLibrariesLibraryIdModelToRequestDTO,
  mapDeleteLibrariesLibraryIdResponseDTOToModel,
} from './mapper';

import type { DeleteLibrariesLibraryId } from './repository.types';

export const deleteLibrariesLibraryId: DeleteLibrariesLibraryId = async (model) => {
  const { libraryId } = mapDeleteLibrariesLibraryIdModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteLibrariesLibraryId({
    pathParams: [{ name: 'libraryId', value: libraryId }],
  });

  return mapDeleteLibrariesLibraryIdResponseDTOToModel(responseDTO);
};
import {
  mapGetLibrariesLibraryIdModelToRequestDTO,
  mapGetLibrariesLibraryIdResponseDTOToModel,
} from './mapper';

import type { GetLibrariesLibraryId } from './repository.types';

export const getLibrariesLibraryId: GetLibrariesLibraryId = async (model) => {
  const { page, libraryId } = mapGetLibrariesLibraryIdModelToRequestDTO(model);

  const responseDTO = await fetcher.getLibrariesLibraryId({
    query: { page },
    pathParams: [{ name: 'libraryId', value: libraryId }],
  });

  return mapGetLibrariesLibraryIdResponseDTOToModel(responseDTO);
};
import {
  mapPostLibrariesLibraryIdModelToRequestDTO,
  mapPostLibrariesLibraryIdResponseDTOToModel,
} from './mapper';

import type { PostLibrariesLibraryId } from './repository.types';

export const postLibrariesLibraryId: PostLibrariesLibraryId = async (model) => {
  const { libraryId, isbn13, totalPages } = mapPostLibrariesLibraryIdModelToRequestDTO(model);

  const responseDTO = await fetcher.postLibrariesLibraryId({
    pathParams: [{ name: 'libraryId', value: libraryId }],
    data: { isbn13, totalPages },
  });

  return mapPostLibrariesLibraryIdResponseDTOToModel(responseDTO);
};
