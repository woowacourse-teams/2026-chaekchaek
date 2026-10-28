import * as fetcher from './fetcher';
import {
  mapGetLibrariesLibraryIdModelToRequestDTO,
  mapGetLibrariesLibraryIdResponseDTOToModel,
} from './mapper';

import type { GetLibrariesLibraryId } from './repository.types';

export const getLibrariesLibraryId: GetLibrariesLibraryId = async (model) => {
  const { libraryId } = mapGetLibrariesLibraryIdModelToRequestDTO(model);

  const responseDTO = await fetcher.getLibrariesLibraryId({
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
  const { libraryId, isbn13, totalPages, status } =
    mapPostLibrariesLibraryIdModelToRequestDTO(model);

  const responseDTO = await fetcher.postLibrariesLibraryId({
    pathParams: [{ name: 'libraryId', value: libraryId }],
    data: { isbn13, totalPages, status },
  });

  return mapPostLibrariesLibraryIdResponseDTOToModel(responseDTO);
};
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
  mapPatchLibrariesLibraryIdModelToRequestDTO,
  mapPatchLibrariesLibraryIdResponseDTOToModel,
} from './mapper';

import type { PatchLibrariesLibraryId } from './repository.types';

export const patchLibrariesLibraryId: PatchLibrariesLibraryId = async (model) => {
  const { libraryId, name } = mapPatchLibrariesLibraryIdModelToRequestDTO(model);

  const responseDTO = await fetcher.patchLibrariesLibraryId({
    pathParams: [{ name: 'libraryId', value: libraryId }],
    data: { name },
  });

  return mapPatchLibrariesLibraryIdResponseDTOToModel(responseDTO);
};
