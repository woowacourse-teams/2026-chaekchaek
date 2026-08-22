export interface PostReviewsReviewIdReactionsCommand {
  reviewId: number;
}

export type PostReviewsReviewIdReactions = (
  command: PostReviewsReviewIdReactionsCommand,
) => Promise<undefined>;
export interface DeleteReviewsReviewIdReactionsParams {
  reviewId: number;
}

export type DeleteReviewsReviewIdReactions = (
  params: DeleteReviewsReviewIdReactionsParams,
) => Promise<undefined>;
