import type { PostLibraryBulkDeleteResponseDto } from './dto';
import type { PostLibraryBulkDeleteCommand } from './repository.types';

// PostLibraryBulkDelete
export const mapPostLibraryBulkDeleteModelToRequestDTO = (
  model: PostLibraryBulkDeleteCommand,
): PostLibraryBulkDeleteCommand => {
  return model;
};

export const mapPostLibraryBulkDeleteResponseDTOToModel = (
  response: PostLibraryBulkDeleteResponseDto,
) => {
  return response;
};
