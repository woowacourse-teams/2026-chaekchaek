import type { DeleteLibraryBookIdResponseDto } from './dto';
import type { DeleteLibraryBookIdParams } from './repository.types';

// DeleteLibraryBookId
export const mapDeleteLibraryBookIdModelToRequestDTO = (
  model: DeleteLibraryBookIdParams,
): DeleteLibraryBookIdParams => {
  return model;
};

export const mapDeleteLibraryBookIdResponseDTOToModel = (
  response: DeleteLibraryBookIdResponseDto,
) => {
  return response;
};
