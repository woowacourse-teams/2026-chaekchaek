import * as fetcher from './fetcher';
import { mapGetFeedReviewsModelToRequestDTO, mapGetFeedReviewsResponseDTOToModel } from './mapper';

import type { GetFeedReviews } from './repository.types';

export const getFeedReviews: GetFeedReviews = async (model) => {
  const { page } = mapGetFeedReviewsModelToRequestDTO(model);

  const responseDTO = await fetcher.getFeedReviews({
    query: { page },
  });

  return mapGetFeedReviewsResponseDTOToModel(responseDTO);
};
