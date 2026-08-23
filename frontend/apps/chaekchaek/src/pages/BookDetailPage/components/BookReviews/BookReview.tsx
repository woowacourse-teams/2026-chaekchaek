import { Avatar, Button, Entry, Note, Shell, Surface } from '@chaekchaek/design-system';

import {
  deleteRepliesReplyIdReactions,
  postRepliesReplyIdReactions,
} from '@/services/apis/repliesReplyIdReactions/repository';
import {
  deleteReviewsReviewIdReactions,
  postReviewsReviewIdReactions,
} from '@/services/apis/reviewsReviewIdReactions/repository';
import { useExecute } from '@/services/core/useExecute';

import type { BookReviewProps } from './BookReview.types';
import { WriteReply } from './WriteReply';
import { useState } from 'react';

export const BookReview = ({ review }: BookReviewProps) => {
  const { mutate: postReviewReactionMutate } = useExecute({
    executeFn: postReviewsReviewIdReactions,
  });
  const { mutate: deleteReviewReactionMutate } = useExecute({
    executeFn: deleteReviewsReviewIdReactions,
  });

  const handleClickReviewReaction = async () => {
    if (!review.likedByMe) {
      return await postReviewReactionMutate({ reviewId: review.reviewId });
    }

    await deleteReviewReactionMutate({ reviewId: review.reviewId });
  };

  const { mutate: postReplyReactionMutate } = useExecute({
    executeFn: postRepliesReplyIdReactions,
  });
  const { mutate: deleteReplyReactionMutate } = useExecute({
    executeFn: deleteRepliesReplyIdReactions,
  });

  const handleClickReplyReaction = async ({
    replyId,
    likedByMe,
  }: {
    replyId: number;
    likedByMe: boolean;
  }) => {
    if (!likedByMe) return await postReplyReactionMutate({ replyId });
    await deleteReplyReactionMutate({ replyId });
  };

  const [openWriteReply, setOpenWriteReply] = useState(false);
  const handleClickToggleWriteReply = () => {
    setOpenWriteReply((prev) => !prev);
  };
  const handleClickCloseWriteReply = () => {
    setOpenWriteReply(false);
  };

  return (
    <Entry variant={review.deleted ? 'subtle' : 'plain'}>
      <Entry.Main>
        <Entry.Header>
          <Shell>
            <Shell.Leading>
              <Avatar img={review.author.profileImageUrl} />
            </Shell.Leading>
            <Shell.Content
              title={review.author.displayName}
              content={new Date(review.createdAt).toLocaleDateString('ko-KR')}
            />
            <Shell.Trailing>Trailing</Shell.Trailing>
          </Shell>
        </Entry.Header>
        <Entry.Body>
          {review.content}
          {review.quote && <Note>{review.quote}</Note>}
        </Entry.Body>
        <Entry.Footer>
          <Button
            size="small"
            leading={review.likedByMe ? '♥' : '♡'}
            onClick={handleClickReviewReaction}
          >
            좋아요 {review.likeCount}
          </Button>
          <Button size="small" leading={'💬'} onClick={handleClickToggleWriteReply}>
            답글 {review.replyCount}
          </Button>
        </Entry.Footer>
      </Entry.Main>
      {(review.recentReplies.length > 0 || openWriteReply) && (
        <Entry.Extension>
          {openWriteReply && <WriteReply />}
          {review.recentReplies.map((recentReply) => {
            return (
              <Surface key={recentReply.replyId}>
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
                        handleClickReplyReaction({
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
          })}
        </Entry.Extension>
      )}
    </Entry>
  );
};
