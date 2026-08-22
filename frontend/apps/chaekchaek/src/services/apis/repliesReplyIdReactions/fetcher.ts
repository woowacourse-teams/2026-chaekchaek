import { requestAjax } from '@/services/core/http';

import type {
  PostRepliesReplyIdReactionsRequestDto,
  PostRepliesReplyIdReactionsResponseDto,
} from './dto';

export const postRepliesReplyIdReactions = async ({
  pathParams: [{ value: replyId }],
}: PostRepliesReplyIdReactionsRequestDto): Promise<PostRepliesReplyIdReactionsResponseDto> => {
  const response = await requestAjax(`/api/v1/replies/${replyId}/reactions`, {
    method: 'post',
    // pathParams: [{ name: 'replyId', value: replyId }],
  });

  return response.data;
};

import type {
  DeleteRepliesReplyIdReactionsRequestDto,
  DeleteRepliesReplyIdReactionsResponseDto,
} from './dto';

export const deleteRepliesReplyIdReactions = async ({
  pathParams: [{ value: replyId }],
}: DeleteRepliesReplyIdReactionsRequestDto): Promise<DeleteRepliesReplyIdReactionsResponseDto> => {
  const response = await requestAjax(`/api/v1/replies/${replyId}/reactions`, {
    method: 'delete',
    // pathParams: [{ name: 'replyId', value: replyId }],
  });

  return response.data;
};
