export interface DeleteRepliesReplyIdParams {
  replyId: number;
}

export type DeleteRepliesReplyId = (params: DeleteRepliesReplyIdParams) => Promise<undefined>;
export interface PatchRepliesReplyIdParams {
  replyId: number;
  content: string;
}

export type PatchRepliesReplyId = (params: PatchRepliesReplyIdParams) => Promise<{
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
