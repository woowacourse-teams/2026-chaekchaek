import type { PostBooksByIsbnIsbn13ReviewsResponseDto } from './dto';
import type { PostBooksByIsbnIsbn13ReviewsCommand } from './repository.types';

// PostBooksByIsbnIsbn13Reviews
export const mapPostBooksByIsbnIsbn13ReviewsModelToRequestDTO = (
  model: PostBooksByIsbnIsbn13ReviewsCommand,
): PostBooksByIsbnIsbn13ReviewsCommand => {
  return model;
};

export const mapPostBooksByIsbnIsbn13ReviewsResponseDTOToModel = (
  response: PostBooksByIsbnIsbn13ReviewsResponseDto,
) => {
  return response;
};
