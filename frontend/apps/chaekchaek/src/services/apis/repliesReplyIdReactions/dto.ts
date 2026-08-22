import type { ResponseDto } from '@/services/apis/api.types';

export interface PostRepliesReplyIdReactionsRequestDto {
  pathParams: [{ name: 'replyId'; value: number }];
}

export type PostRepliesReplyIdReactionsResponseDto = ResponseDto<undefined>;
export interface DeleteRepliesReplyIdReactionsRequestDto {
  pathParams: [{ name: 'replyId'; value: number }];
}

export type DeleteRepliesReplyIdReactionsResponseDto = ResponseDto<undefined>;
