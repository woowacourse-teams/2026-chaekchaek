import type { PutLibraryBookIdRatingResponseDto } from './dto';
import type { PutLibraryBookIdRatingCommand } from './repository.types';

// PutLibraryBookIdRating
export const mapPutLibraryBookIdRatingModelToRequestDTO = (
  model: PutLibraryBookIdRatingCommand,
): PutLibraryBookIdRatingCommand => {
  return model;
};

export const mapPutLibraryBookIdRatingResponseDTOToModel = (
  response: PutLibraryBookIdRatingResponseDto,
) => {
  return response;
};
import type { DeleteLibraryBookIdRatingResponseDto } from './dto';
import type { DeleteLibraryBookIdRatingParams } from './repository.types';

// DeleteLibraryBookIdRating
export const mapDeleteLibraryBookIdRatingModelToRequestDTO = (
  model: DeleteLibraryBookIdRatingParams,
): DeleteLibraryBookIdRatingParams => {
  return model;
};

export const mapDeleteLibraryBookIdRatingResponseDTOToModel = (
  response: DeleteLibraryBookIdRatingResponseDto,
) => {
  return response;
};
