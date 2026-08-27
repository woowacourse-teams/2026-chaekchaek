import { useCallback, useState } from 'react';

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
import { deleteReviewsReviewId } from '@/services/apis/reviewsReviewId/repository';

import { useExecute } from '@/services/core/useExecute';

import type { BookReviewProps } from './BookReview.types';
import { WriteReply } from './WriteReply';

import { UpdateReviewDialog } from '../../dialog/UpdateReviewDialog';

const SPOILER_PLACEHOLDER_REVIEW = '짹짹짹 짹짹 짹짹짹짹. 짹짹짹 짹짹짹 짹짹짹 짹짹짹짹 짹짹짹짹.';
const SPOILER_PLACEHOLDER_REPLY = '“짹짹짹 짹짹 짹짹짹짹 짹짹.”';

export const BookReview = ({ review, onReviewsRefresh }: BookReviewProps) => {
  const isSpoilerVisible = false;
  const getReviewsReviewIdRepliesLoadData = useCallback(() => {
    return getReviewsReviewIdReplies({ reviewId: review.reviewId, page: 1 });
  }, [review.reviewId]);

  const {
    refetch: refetchGetReplies,
    status: { data: repliesData },
  } = useLoadData({
    queryFn: getReviewsReviewIdRepliesLoadData,
  });

  const { mutate: deleteReview } = useExecute({
    executeFn: deleteReviewsReviewId,
  });

  const handleClickDeleteReview = async (reviewId: number) => {
    await deleteReview({ reviewId });

    onReviewsRefresh();
  };

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

  const [dialog, setDialog] = useState<'UpdateReviewDialog' | null>(null);
  const handleOpenDialog = (dialog: 'UpdateReviewDialog') => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: 'UpdateReviewDialog' | null) => {
    switch (dialog) {
      case 'UpdateReviewDialog':
        return (
          <UpdateReviewDialog
            review={review}
            onReviewUpdated={onReviewsRefresh}
            onClose={handleCloseDialog}
          />
        );

      default:
        return null;
    }
  };

  const dialogElement = renderDialog(dialog);

  const showSpoilerVisible = isSpoilerVisible || review.isSpoiler;

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
            {review.author.mine && (
              <Shell.Trailing>
                <Button
                  size="small"
                  onClick={() => {
                    handleOpenDialog('UpdateReviewDialog');
                  }}
                >
                  수정
                </Button>
                <Button size="small" onClick={() => handleClickDeleteReview(review.reviewId)}>
                  삭제
                </Button>
              </Shell.Trailing>
            )}
          </Shell>
        </Entry.Header>
        <Entry.Body>
          {!showSpoilerVisible ? review.content : SPOILER_PLACEHOLDER_REVIEW}
          {review.quote && (
            <Note>{!showSpoilerVisible ? review.quote : SPOILER_PLACEHOLDER_REVIEW}</Note>
          )}
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
                  <Shell.Content
                    title={reply.author.displayName}
                    description={!showSpoilerVisible ? reply.content : SPOILER_PLACEHOLDER_REPLY}
                  />
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
      {dialogElement}
    </Entry>
  );
};
