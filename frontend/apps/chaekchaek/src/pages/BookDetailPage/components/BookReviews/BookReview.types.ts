import type { RecentReply } from './BookReviews.types';

export type BookReviewProps = {
  recentReply: RecentReply;
  onReplyReactionClick: (reply: { replyId: number; likedByMe: boolean }) => void;
};
