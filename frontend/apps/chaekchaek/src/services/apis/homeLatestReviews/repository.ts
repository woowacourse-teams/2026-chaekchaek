import * as fetcher from './fetcher';
import {
  mapGetHomeLatestReviewsModelToRequestDTO,
  mapGetHomeLatestReviewsResponseDTOToModel,
} from './mapper';

import type { GetHomeLatestReviews } from './repository.types';

export const getHomeLatestReviews: GetHomeLatestReviews = async (model) => {
  const requestModel = mapGetHomeLatestReviewsModelToRequestDTO(model);

  const responseDTO = await fetcher.getHomeLatestReviews(requestModel);

  return mapGetHomeLatestReviewsResponseDTOToModel(responseDTO);
};
