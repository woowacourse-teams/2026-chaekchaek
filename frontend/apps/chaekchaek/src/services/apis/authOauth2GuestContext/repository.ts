import * as fetcher from './fetcher';
import {
  mapPostAuthOauth2GuestContextModelToRequestDTO,
  mapPostAuthOauth2GuestContextResponseDTOToModel,
} from './mapper';

import type { PostAuthOauth2GuestContext } from './repository.types';

export const postAuthOauth2GuestContext: PostAuthOauth2GuestContext = async (model, context) => {
  const requestModel = mapPostAuthOauth2GuestContextModelToRequestDTO(model);

  const { guestToken } = context;

  const responseDTO = await fetcher.postAuthOauth2GuestContext({
    ...requestModel,
    headers: { 'X-Guest-Token': guestToken },
  });

  return mapPostAuthOauth2GuestContextResponseDTOToModel(responseDTO);
};
