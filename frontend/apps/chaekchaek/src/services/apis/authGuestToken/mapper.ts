import type { GetAuthGuestTokenResponseDto } from './dto';
import type { GetAuthGuestTokenParams } from './repository.types';

// GetAuthGuestToken
export const mapGetAuthGuestTokenModelToRequestDTO = (
  model: GetAuthGuestTokenParams,
): GetAuthGuestTokenParams => {
  return model;
};

export const mapGetAuthGuestTokenResponseDTOToModel = (response: GetAuthGuestTokenResponseDto) => {
  return response;
};
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
