import * as fetcher from './fetcher';
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
