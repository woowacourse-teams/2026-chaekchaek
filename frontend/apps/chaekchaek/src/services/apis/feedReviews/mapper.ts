import type { GetFeedReviewsResponseDto } from './dto';
import type { GetFeedReviewsParams } from './repository.types';

// GetFeedReviews
export const mapGetFeedReviewsModelToRequestDTO = (
  model: GetFeedReviewsParams,
): GetFeedReviewsParams => {
  return model;
};

export const mapGetFeedReviewsResponseDTOToModel = (response: GetFeedReviewsResponseDto) => {
  return response;
};
