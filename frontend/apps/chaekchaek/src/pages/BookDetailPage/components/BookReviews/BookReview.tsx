import { Avatar, Button, Entry, Note, Shell, Surface } from '@chaekchaek/design-system';

import { getReviewsReviewIdReplies } from '@/services/apis/reviewsReviewIdReplies/repository';
import { useLoadData } from '@/services/core/useLoadData';

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
import { useCallback, useState } from 'react';

export const BookReview = ({ review, onReviewsRefresh }: BookReviewProps) => {
  const getReviewsReviewIdRepliesLoadData = useCallback(() => {
    return getReviewsReviewIdReplies({ reviewId: review.reviewId, page: 1 });
  }, [review.reviewId]);

  const {
    refetch: refetchGetReplies,
    status: { data: repliesData },
  } = useLoadData({
    queryFn: getReviewsReviewIdRepliesLoadData,
  });

  const { mutate: postReviewReactionMutate } = useExecute({
    executeFn: postReviewsReviewIdReactions,
  });
  const { mutate: deleteReviewReactionMutate } = useExecute({
    executeFn: deleteReviewsReviewIdReactions,
  });

  const handleClickReviewReaction = async () => {
    if (!review.likedByMe) {
      await postReviewReactionMutate({ reviewId: review.reviewId });
    } else {
      await deleteReviewReactionMutate({ reviewId: review.reviewId });
    }

    await onReviewsRefresh();
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
    if (!likedByMe) {
      await postReplyReactionMutate({ replyId });
    } else {
      await deleteReplyReactionMutate({ replyId });
    }

    await refetchGetReplies();
  };

  const [openWriteReply, setOpenWriteReply] = useState(false);
  const handleClickToggleWriteReply = () => {
    setOpenWriteReply((prev) => !prev);
  };
  const handleClickCloseWriteReply = () => {
    setOpenWriteReply(false);
  };
  const handleReplyWritten = async () => {
    await refetchGetReplies();
    handleClickCloseWriteReply();
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
      {(!!repliesData?.items.length || openWriteReply) && (
        <Entry.Extension>
          {openWriteReply && (
            <WriteReply reviewId={review.reviewId} onReplyWritten={handleReplyWritten} />
          )}
          {repliesData?.items.map((reply) => {
            return (
              <Surface key={reply.replyId}>
                <Shell>
                  <Shell.Leading>
                    <Avatar img={reply.author.profileImageUrl} size="small" />
                  </Shell.Leading>
                  <Shell.Content title={reply.author.displayName} description={reply.content} />
                  <Shell.Trailing>
                    <span
                      onClick={() => {
                        handleClickReplyReaction({
                          replyId: reply.replyId,
                          likedByMe: reply.likedByMe,
                        });
                      }}
                    >
                      {reply.likedByMe ? '♥' : '♡'}
                      {reply.likeCount}
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
