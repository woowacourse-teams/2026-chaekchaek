import * as fetcher from './fetcher';
import {
  mapPostReviewsReviewIdReactionsModelToRequestDTO,
  mapPostReviewsReviewIdReactionsResponseDTOToModel,
} from './mapper';

import type { PostReviewsReviewIdReactions } from './repository.types';

export const postReviewsReviewIdReactions: PostReviewsReviewIdReactions = async (model) => {
  const { reviewId } = mapPostReviewsReviewIdReactionsModelToRequestDTO(model);

  const responseDTO = await fetcher.postReviewsReviewIdReactions({
    pathParams: [{ name: 'reviewId', value: reviewId }],
  });

  return mapPostReviewsReviewIdReactionsResponseDTOToModel(responseDTO);
};
import {
  mapDeleteReviewsReviewIdReactionsModelToRequestDTO,
  mapDeleteReviewsReviewIdReactionsResponseDTOToModel,
} from './mapper';

import type { DeleteReviewsReviewIdReactions } from './repository.types';

export const deleteReviewsReviewIdReactions: DeleteReviewsReviewIdReactions = async (model) => {
  const { reviewId } = mapDeleteReviewsReviewIdReactionsModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteReviewsReviewIdReactions({
    pathParams: [{ name: 'reviewId', value: reviewId }],
  });

  return mapDeleteReviewsReviewIdReactionsResponseDTOToModel(responseDTO);
};
