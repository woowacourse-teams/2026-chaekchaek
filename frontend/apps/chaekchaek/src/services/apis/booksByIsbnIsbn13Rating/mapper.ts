import type { PutBooksByIsbnIsbn13RatingResponseDto } from './dto';
import type { PutBooksByIsbnIsbn13RatingCommand } from './repository.types';

// PutBooksByIsbnIsbn13Rating
export const mapPutBooksByIsbnIsbn13RatingModelToRequestDTO = (
  model: PutBooksByIsbnIsbn13RatingCommand,
): PutBooksByIsbnIsbn13RatingCommand => {
  return model;
};

export const mapPutBooksByIsbnIsbn13RatingResponseDTOToModel = (
  response: PutBooksByIsbnIsbn13RatingResponseDto,
) => {
  return response;
};
import type { DeleteBooksByIsbnIsbn13RatingResponseDto } from './dto';
import type { DeleteBooksByIsbnIsbn13RatingParams } from './repository.types';

// DeleteBooksByIsbnIsbn13Rating
export const mapDeleteBooksByIsbnIsbn13RatingModelToRequestDTO = (
  model: DeleteBooksByIsbnIsbn13RatingParams,
): DeleteBooksByIsbnIsbn13RatingParams => {
  return model;
};

export const mapDeleteBooksByIsbnIsbn13RatingResponseDTOToModel = (
  response: DeleteBooksByIsbnIsbn13RatingResponseDto,
) => {
  return response;
};
