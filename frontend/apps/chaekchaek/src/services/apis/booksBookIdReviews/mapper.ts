import type { GetBooksBookIdReviewsResponseDto } from './dto';
import type { GetBooksBookIdReviewsParams } from './repository.types';

// GetBooksBookIdReviews
export const mapGetBooksBookIdReviewsModelToRequestDTO = (
  model: GetBooksBookIdReviewsParams,
): GetBooksBookIdReviewsParams => {
  return model;
};

export const mapGetBooksBookIdReviewsResponseDTOToModel = (
  response: GetBooksBookIdReviewsResponseDto,
) => {
  return response;
};
import type { PostBooksBookIdReviewsResponseDto } from './dto';
import type { PostBooksBookIdReviewsCommand } from './repository.types';

// PostBooksBookIdReviews
export const mapPostBooksBookIdReviewsModelToRequestDTO = (
  model: PostBooksBookIdReviewsCommand,
): PostBooksBookIdReviewsCommand => {
  return model;
};

export const mapPostBooksBookIdReviewsResponseDTOToModel = (
  response: PostBooksBookIdReviewsResponseDto,
) => {
  return response;
};
