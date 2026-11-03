import type { PostLibrariesResponseDto } from './dto';
import type { PostLibrariesCommand } from './repository.types';

// PostLibraries
export const mapPostLibrariesModelToRequestDTO = (
  model: PostLibrariesCommand,
): PostLibrariesCommand => {
  return model;
};

export const mapPostLibrariesResponseDTOToModel = (response: PostLibrariesResponseDto) => {
  return response;
};
import type { GetLibrariesResponseDto } from './dto';
import type { GetLibrariesParams } from './repository.types';

// GetLibraries
export const mapGetLibrariesModelToRequestDTO = (model: GetLibrariesParams): GetLibrariesParams => {
  return model;
};

export const mapGetLibrariesResponseDTOToModel = (response: GetLibrariesResponseDto) => {
  return response;
};
