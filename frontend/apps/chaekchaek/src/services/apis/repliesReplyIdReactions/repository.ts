import * as fetcher from './fetcher';
import {
  mapPostRepliesReplyIdReactionsModelToRequestDTO,
  mapPostRepliesReplyIdReactionsResponseDTOToModel,
} from './mapper';

import type { PostRepliesReplyIdReactions } from './repository.types';

export const postRepliesReplyIdReactions: PostRepliesReplyIdReactions = async (model) => {
  const { replyId } = mapPostRepliesReplyIdReactionsModelToRequestDTO(model);

  const responseDTO = await fetcher.postRepliesReplyIdReactions({
    pathParams: [{ name: 'replyId', value: replyId }],
  });

  return mapPostRepliesReplyIdReactionsResponseDTOToModel(responseDTO);
};
import {
  mapDeleteRepliesReplyIdReactionsModelToRequestDTO,
  mapDeleteRepliesReplyIdReactionsResponseDTOToModel,
} from './mapper';

import type { DeleteRepliesReplyIdReactions } from './repository.types';

export const deleteRepliesReplyIdReactions: DeleteRepliesReplyIdReactions = async (model) => {
  const { replyId } = mapDeleteRepliesReplyIdReactionsModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteRepliesReplyIdReactions({
    pathParams: [{ name: 'replyId', value: replyId }],
  });

  return mapDeleteRepliesReplyIdReactionsResponseDTOToModel(responseDTO);
};
