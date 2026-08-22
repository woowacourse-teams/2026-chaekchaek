export interface PostRepliesReplyIdReactionsCommand {
  replyId: number;
}

export type PostRepliesReplyIdReactions = (
  command: PostRepliesReplyIdReactionsCommand,
) => Promise<undefined>;
export interface DeleteRepliesReplyIdReactionsParams {
  replyId: number;
}

export type DeleteRepliesReplyIdReactions = (
  params: DeleteRepliesReplyIdReactionsParams,
) => Promise<undefined>;
