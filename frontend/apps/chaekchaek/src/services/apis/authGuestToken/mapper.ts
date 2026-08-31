import type { PostAuthGuestTokenResponseDto } from './dto';
import type { PostAuthGuestTokenCommand } from './repository.types';

// PostAuthGuestToken
export const mapPostAuthGuestTokenModelToRequestDTO = (
  model: PostAuthGuestTokenCommand,
): PostAuthGuestTokenCommand => {
  return model;
};

export const mapPostAuthGuestTokenResponseDTOToModel = (
  response: PostAuthGuestTokenResponseDto,
) => {
  return response;
};
