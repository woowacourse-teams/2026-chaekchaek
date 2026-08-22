import type { PostRepliesReplyIdReactionsResponseDto } from './dto';
import type { PostRepliesReplyIdReactionsCommand } from './repository.types';

// PostRepliesReplyIdReactions
export const mapPostRepliesReplyIdReactionsModelToRequestDTO = (
  model: PostRepliesReplyIdReactionsCommand,
): PostRepliesReplyIdReactionsCommand => {
  return model;
};

export const mapPostRepliesReplyIdReactionsResponseDTOToModel = (
  response: PostRepliesReplyIdReactionsResponseDto,
) => {
  return response;
};
import type { DeleteRepliesReplyIdReactionsResponseDto } from './dto';
import type { DeleteRepliesReplyIdReactionsParams } from './repository.types';

// DeleteRepliesReplyIdReactions
export const mapDeleteRepliesReplyIdReactionsModelToRequestDTO = (
  model: DeleteRepliesReplyIdReactionsParams,
): DeleteRepliesReplyIdReactionsParams => {
  return model;
};

export const mapDeleteRepliesReplyIdReactionsResponseDTOToModel = (
  response: DeleteRepliesReplyIdReactionsResponseDto,
) => {
  return response;
};
