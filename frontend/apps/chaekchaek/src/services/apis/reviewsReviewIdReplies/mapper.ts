import type { GetReviewsReviewIdRepliesResponseDto } from './dto';
import type { GetReviewsReviewIdRepliesParams } from './repository.types';

// GetReviewsReviewIdReplies
export const mapGetReviewsReviewIdRepliesModelToRequestDTO = (
  model: GetReviewsReviewIdRepliesParams,
): GetReviewsReviewIdRepliesParams => {
  return model;
};

export const mapGetReviewsReviewIdRepliesResponseDTOToModel = (
  response: GetReviewsReviewIdRepliesResponseDto,
) => {
  return response;
};
import type { PostReviewsReviewIdRepliesResponseDto } from './dto';
import type { PostReviewsReviewIdRepliesCommand } from './repository.types';

// PostReviewsReviewIdReplies
export const mapPostReviewsReviewIdRepliesModelToRequestDTO = (
  model: PostReviewsReviewIdRepliesCommand,
): PostReviewsReviewIdRepliesCommand => {
  return model;
};

export const mapPostReviewsReviewIdRepliesResponseDTOToModel = (
  response: PostReviewsReviewIdRepliesResponseDto,
) => {
  return response;
};
