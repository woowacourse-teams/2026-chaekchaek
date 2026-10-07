import * as fetcher from './fetcher';
import {
  mapPostAuthGuestTokenRefreshModelToRequestDTO,
  mapPostAuthGuestTokenRefreshResponseDTOToModel,
} from './mapper';

import type { PostAuthGuestTokenRefresh } from './repository.types';

export const postAuthGuestTokenRefresh: PostAuthGuestTokenRefresh = async (model, context) => {
  const requestModal = mapPostAuthGuestTokenRefreshModelToRequestDTO(model);

  const { guestToken } = context;

  const responseDTO = await fetcher.postAuthGuestTokenRefresh({
    ...requestModal,
    headers: {
      'X-Guest-Token': guestToken,
    },
  });

  return mapPostAuthGuestTokenRefreshResponseDTOToModel(responseDTO);
};
