import type { PostReviewsReviewIdReactionsResponseDto } from './dto';
import type { PostReviewsReviewIdReactionsCommand } from './repository.types';

// PostReviewsReviewIdReactions
export const mapPostReviewsReviewIdReactionsModelToRequestDTO = (
  model: PostReviewsReviewIdReactionsCommand,
): PostReviewsReviewIdReactionsCommand => {
  return model;
};

export const mapPostReviewsReviewIdReactionsResponseDTOToModel = (
  response: PostReviewsReviewIdReactionsResponseDto,
) => {
  return response;
};
import type { DeleteReviewsReviewIdReactionsResponseDto } from './dto';
import type { DeleteReviewsReviewIdReactionsParams } from './repository.types';

// DeleteReviewsReviewIdReactions
export const mapDeleteReviewsReviewIdReactionsModelToRequestDTO = (
  model: DeleteReviewsReviewIdReactionsParams,
): DeleteReviewsReviewIdReactionsParams => {
  return model;
};

export const mapDeleteReviewsReviewIdReactionsResponseDTOToModel = (
  response: DeleteReviewsReviewIdReactionsResponseDto,
) => {
  return response;
};
