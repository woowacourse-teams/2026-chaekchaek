import * as fetcher from './fetcher';
import {
  mapDeleteReviewsReviewIdModelToRequestDTO,
  mapDeleteReviewsReviewIdResponseDTOToModel,
} from './mapper';

import type { DeleteReviewsReviewId } from './repository.types';

export const deleteReviewsReviewId: DeleteReviewsReviewId = async (model) => {
  const { reviewId } = mapDeleteReviewsReviewIdModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteReviewsReviewId({
    pathParams: [{ name: 'reviewId', value: reviewId }],
  });

  return mapDeleteReviewsReviewIdResponseDTOToModel(responseDTO);
};
import {
  mapPatchReviewsReviewIdModelToRequestDTO,
  mapPatchReviewsReviewIdResponseDTOToModel,
} from './mapper';

import type { PatchReviewsReviewId } from './repository.types';

export const patchReviewsReviewId: PatchReviewsReviewId = async (model, context) => {
  const { reviewId, chapter, isSpoiler, quote, totalPages, currentPage, content } =
    mapPatchReviewsReviewIdModelToRequestDTO(model);

  const guestToken = context?.guestToken;

  const responseDTO = await fetcher.patchReviewsReviewId({
    pathParams: [{ name: 'reviewId', value: reviewId }],
    data: { chapter, isSpoiler, quote, totalPages, currentPage, content },
    ...(guestToken && {
      headers: {
        'X-Guest-Token': guestToken,
      },
    }),
  });

  return mapPatchReviewsReviewIdResponseDTOToModel(responseDTO);
};
