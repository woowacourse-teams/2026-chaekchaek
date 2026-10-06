import * as fetcher from './fetcher';
import {
  mapGetAuthGuestTokenModelToRequestDTO,
  mapGetAuthGuestTokenResponseDTOToModel,
} from './mapper';

import type { GetAuthGuestToken } from './repository.types';

export const getAuthGuestToken: GetAuthGuestToken = async (model, context) => {
  const authGuestTokenRequest = mapGetAuthGuestTokenModelToRequestDTO(model);

  const { guestToken } = context;

  const responseDTO = await fetcher.getAuthGuestToken({
    ...authGuestTokenRequest,
    headers: {
      'X-Guest-Token': guestToken,
    },
  });

  return mapGetAuthGuestTokenResponseDTOToModel(responseDTO);
};

import {
  mapPostAuthGuestTokenModelToRequestDTO,
  mapPostAuthGuestTokenResponseDTOToModel,
} from './mapper';

import type { PostAuthGuestToken } from './repository.types';

export const postAuthGuestToken: PostAuthGuestToken = async (model) => {
  const requestModel = mapPostAuthGuestTokenModelToRequestDTO(model);

  const responseDTO = await fetcher.postAuthGuestToken(requestModel);

  return mapPostAuthGuestTokenResponseDTOToModel(responseDTO);
};
