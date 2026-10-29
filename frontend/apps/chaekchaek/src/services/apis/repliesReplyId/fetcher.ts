import { instance } from '@/services/core/http';

import type { DeleteRepliesReplyIdRequestDto, DeleteRepliesReplyIdResponseDto } from './dto';

export const deleteRepliesReplyId = async ({
  pathParams: [{ value: replyId }],
}: DeleteRepliesReplyIdRequestDto): Promise<DeleteRepliesReplyIdResponseDto> => {
  const response = await instance('/api/v1/replies', {
    method: 'delete',
    pathParams: [{ name: 'replyId', value: replyId }],
  });

  return response.data;
};

import type { PatchRepliesReplyIdRequestDto, PatchRepliesReplyIdResponseDto } from './dto';

export const patchRepliesReplyId = async ({
  pathParams: [{ value: replyId }],
  data: { content },
}: PatchRepliesReplyIdRequestDto): Promise<PatchRepliesReplyIdResponseDto> => {
  const response = await instance('/api/v1/replies', {
    method: 'patch',
    pathParams: [{ name: 'replyId', value: replyId }],
    data: { content },
  });

  return response.data;
};
