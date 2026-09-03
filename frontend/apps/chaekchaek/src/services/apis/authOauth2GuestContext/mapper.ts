import type { PostAuthOauth2GuestContextResponseDto } from './dto';
import type { PostAuthOauth2GuestContextCommand } from './repository.types';

// PostAuthOauth2GuestContext
export const mapPostAuthOauth2GuestContextModelToRequestDTO = (
  model: PostAuthOauth2GuestContextCommand,
): PostAuthOauth2GuestContextCommand => {
  return model;
};

export const mapPostAuthOauth2GuestContextResponseDTOToModel = (
  response: PostAuthOauth2GuestContextResponseDto,
) => {
  return response;
};
