import type { GetHomeLatestReviewsResponseDto } from './dto';
import type { GetHomeLatestReviewsParams } from './repository.types';

// GetHomeLatestReviews
export const mapGetHomeLatestReviewsModelToRequestDTO = (
  model: GetHomeLatestReviewsParams,
): GetHomeLatestReviewsParams => {
  return model;
};

export const mapGetHomeLatestReviewsResponseDTOToModel = (
  response: GetHomeLatestReviewsResponseDto,
) => {
  return response;
};
