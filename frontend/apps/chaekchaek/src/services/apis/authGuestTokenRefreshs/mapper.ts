import type { PostAuthGuestTokenRefreshsResponseDto } from './dto';
import type { PostAuthGuestTokenRefreshsCommand } from './repository.types';

// PostAuthGuestTokenRefreshs
export const mapPostAuthGuestTokenRefreshsModelToRequestDTO = (
  model: PostAuthGuestTokenRefreshsCommand,
): PostAuthGuestTokenRefreshsCommand => {
  return model;
};

export const mapPostAuthGuestTokenRefreshsResponseDTOToModel = (
  response: PostAuthGuestTokenRefreshsResponseDto,
) => {
  return response;
};
