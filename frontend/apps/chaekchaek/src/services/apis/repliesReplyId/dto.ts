import type { ResponseDto } from '@/services/apis/api.types';

export interface DeleteRepliesReplyIdRequestDto {
  pathParams: [{ name: 'replyId'; value: number }];
}

export type DeleteRepliesReplyIdResponseDto = ResponseDto<undefined>;
export interface PatchRepliesReplyIdRequestDto {
  pathParams: [{ name: 'replyId'; value: number }];
  data: { content: string };
}

export type PatchRepliesReplyIdResponseDto = ResponseDto<{
  createdAt: string;
  likedByMe: boolean;
  author: {
    mine: boolean;
    actorType: string;
    displayName: string;
    profileStatus: string;
    anonymous: boolean;
    profileImageUrl: string;
    memberId: number;
  };
  replyId: number;
  likeCount: number;
  content: string;
}>;
