import { Avatar, Shell, Surface } from '@chaekchaek/design-system';

import type { BookReviewProps } from './BookReview.types';

export const BookReview = ({ recentReply, onReplyReactionClick }: BookReviewProps) => {
  return (
    <Surface>
      <Shell>
        <Shell.Leading>
          <Avatar img={recentReply.author.profileImageUrl} size="small" />
        </Shell.Leading>
        <Shell.Content
          title={recentReply.author.displayName}
          description={recentReply.content}
        />
        <Shell.Trailing>
          <span
            onClick={() => {
              onReplyReactionClick({
                replyId: recentReply.replyId,
                likedByMe: recentReply.likedByMe,
              });
            }}
          >
            {recentReply.likedByMe ? '♥' : '♡'}
            {recentReply.likeCount}
          </span>
        </Shell.Trailing>
      </Shell>
    </Surface>
  );
};
