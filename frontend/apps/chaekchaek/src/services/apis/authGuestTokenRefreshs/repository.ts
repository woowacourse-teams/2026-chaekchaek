import * as fetcher from './fetcher';
import {
  mapPostAuthGuestTokenRefreshsModelToRequestDTO,
  mapPostAuthGuestTokenRefreshsResponseDTOToModel,
} from './mapper';

import type { PostAuthGuestTokenRefreshs } from './repository.types';

export const postAuthGuestTokenRefreshs: PostAuthGuestTokenRefreshs = async (model, context) => {
  const requestModal = mapPostAuthGuestTokenRefreshsModelToRequestDTO(model);

  const { guestToken } = context;

  const responseDTO = await fetcher.postAuthGuestTokenRefreshs({
    ...requestModal,
    headers: {
      'X-Guest-Token': guestToken,
    },
  });

  return mapPostAuthGuestTokenRefreshsResponseDTOToModel(responseDTO);
};
