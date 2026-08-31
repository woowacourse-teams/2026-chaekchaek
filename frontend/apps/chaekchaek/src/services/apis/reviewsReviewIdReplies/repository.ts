import * as fetcher from './fetcher';
import { createRepositoryRequestHeaders } from '@/services/context/requestHeaders';
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

export const postReviewsReviewIdReplies: PostReviewsReviewIdReplies = async (model, context) => {
  const { reviewId, content } = mapPostReviewsReviewIdRepliesModelToRequestDTO(model);

  const { guestToken } = context ?? {};
  const headers = createRepositoryRequestHeaders({ guestToken });

  const responseDTO = await fetcher.postReviewsReviewIdReplies({
    pathParams: [{ name: 'reviewId', value: reviewId }],
    data: { content },
    headers,
  });

  return mapPostReviewsReviewIdRepliesResponseDTOToModel(responseDTO);
};
