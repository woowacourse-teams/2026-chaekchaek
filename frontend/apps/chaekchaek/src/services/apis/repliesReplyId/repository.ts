import * as fetcher from './fetcher';
import {
  mapDeleteRepliesReplyIdModelToRequestDTO,
  mapDeleteRepliesReplyIdResponseDTOToModel,
} from './mapper';

import type { DeleteRepliesReplyId } from './repository.types';

export const deleteRepliesReplyId: DeleteRepliesReplyId = async (model) => {
  const { replyId } = mapDeleteRepliesReplyIdModelToRequestDTO(model);

  const responseDTO = await fetcher.deleteRepliesReplyId({
    pathParams: [{ name: 'replyId', value: replyId }],
  });

  return mapDeleteRepliesReplyIdResponseDTOToModel(responseDTO);
};
import {
  mapPatchRepliesReplyIdModelToRequestDTO,
  mapPatchRepliesReplyIdResponseDTOToModel,
} from './mapper';

import type { PatchRepliesReplyId } from './repository.types';

export const patchRepliesReplyId: PatchRepliesReplyId = async (model) => {
  const { replyId, content } = mapPatchRepliesReplyIdModelToRequestDTO(model);

  const responseDTO = await fetcher.patchRepliesReplyId({
    pathParams: [{ name: 'replyId', value: replyId }],
    data: { content },
  });

  return mapPatchRepliesReplyIdResponseDTOToModel(responseDTO);
};
