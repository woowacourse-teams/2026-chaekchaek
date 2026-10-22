import * as fetcher from './fetcher';
import { mapPostLibrariesModelToRequestDTO, mapPostLibrariesResponseDTOToModel } from './mapper';

import type { PostLibraries } from './repository.types';

export const postLibraries: PostLibraries = async (model) => {
  const { name } = mapPostLibrariesModelToRequestDTO(model);

  const responseDTO = await fetcher.postLibraries({
    data: { name },
  });

  return mapPostLibrariesResponseDTOToModel(responseDTO);
};
import { mapGetLibrariesModelToRequestDTO, mapGetLibrariesResponseDTOToModel } from './mapper';

import type { GetLibraries } from './repository.types';

export const getLibraries: GetLibraries = async (model) => {
  const { memberId } = mapGetLibrariesModelToRequestDTO(model);

  const responseDTO = await fetcher.getLibraries({
    query: { memberId },
  });

  return mapGetLibrariesResponseDTOToModel(responseDTO);
};
