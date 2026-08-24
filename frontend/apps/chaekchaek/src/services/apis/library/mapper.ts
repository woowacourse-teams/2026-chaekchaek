import type { GetLibraryResponseDto } from './dto';
import type { GetLibraryParams } from './repository.types';

// GetLibrary
export const mapGetLibraryModelToRequestDTO = (model: GetLibraryParams): GetLibraryParams => {
  return model;
};

export const mapGetLibraryResponseDTOToModel = (response: GetLibraryResponseDto) => {
  return response;
};
import type { PostLibraryResponseDto } from './dto';
import type { PostLibraryCommand } from './repository.types';

// PostLibrary
export const mapPostLibraryModelToRequestDTO = (model: PostLibraryCommand): PostLibraryCommand => {
  return model;
};

export const mapPostLibraryResponseDTOToModel = (response: PostLibraryResponseDto) => {
  return response;
};
