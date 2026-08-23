import * as fetcher from './fetcher';
import {
  mapGetReviewsReviewIdRepliesModelToRequestDTO,
  mapGetReviewsReviewIdRepliesResponseDTOToModel,
} from './mapper';

import type { GetReviewsReviewIdReplies } from './repository.types';

export const getReviewsReviewIdReplies: GetReviewsReviewIdReplies = async (model) => {
  const { page, reviewId } = mapGetReviewsReviewIdRepliesModelToRequestDTO(model);

  const responseDTO = await fetcher.getReviewsReviewIdReplies({
    query: { page },
    pathParams: [{ name: 'reviewId', value: reviewId }],
  });

  return mapGetReviewsReviewIdRepliesResponseDTOToModel(responseDTO);
};
import {
  mapPostReviewsReviewIdRepliesModelToRequestDTO,
  mapPostReviewsReviewIdRepliesResponseDTOToModel,
} from './mapper';

import type { PostReviewsReviewIdReplies } from './repository.types';

export const postReviewsReviewIdReplies: PostReviewsReviewIdReplies = async (model) => {
  const { reviewId, content } = mapPostReviewsReviewIdRepliesModelToRequestDTO(model);

  const responseDTO = await fetcher.postReviewsReviewIdReplies({
    pathParams: [{ name: 'reviewId', value: reviewId }],
    data: { content },
  });

  return mapPostReviewsReviewIdRepliesResponseDTOToModel(responseDTO);
};
