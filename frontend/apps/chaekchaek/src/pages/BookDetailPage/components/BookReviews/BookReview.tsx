import { memo, useCallback, useEffect, useState } from 'react';
import { generatePath, Link } from 'react-router-dom';

import {
  Avatar,
  Badge,
  Button,
  Dialog,
  DropdownMenu,
  Entry,
  Icon,
  IconButton,
  InfoMessage,
  Note,
  Shell,
  Surface,
  Text,
} from '@chaekchaek/design-system';

import { ROUTES } from '@/constants/routes';

import { track } from '@/analytics/track';

import { getReviewsReviewIdReplies } from '@/services/apis/reviewsReviewIdReplies/repository';
import { useLoadData } from '@/services/core/useLoadData';

import {
  deleteReviewsReviewIdReactions,
  postReviewsReviewIdReactions,
} from '@/services/apis/reviewsReviewIdReactions/repository';
import { deleteReviewsReviewId } from '@/services/apis/reviewsReviewId/repository';
import { useExecute } from '@/services/core/useExecute';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import type { BookReviewProps } from './BookReview.types';
import { WriteReply } from './WriteReply';

import { UpdateReviewDialog } from '../../dialog/UpdateReviewDialog';

import styles from './BookReview.module.css';
import { BookReply } from './BookReply';
import { useInfiniteLoadData } from '@/services/core/useInfiniteLoadData';

export const BookReview = ({ isbn, review, onReviewsRefresh }: BookReviewProps) => {
  const { isAuthenticated, user, guest } = useAuthContext();
  const getReviewsReviewIdRepliesLoadData = useCallback(() => {
    return getReviewsReviewIdReplies({ reviewId: review.reviewId, page: 1 });
  }, [review.reviewId]);

  const {
    status: { data: repliesDataAll },
    refetch: refetchGetReplies,
    fetchNextPage,
  } = useInfiniteLoadData({
    queryFn: getReviewsReviewIdRepliesLoadData,
    initialPageParam: 1,
    getNextPageParam: (lastPage) => {
      return lastPage?.nextPage || 1;
    },
  });

  const repliesData = (repliesDataAll?.pages ?? []).flatMap((page) => page?.items).filter(Boolean);

  const extensionRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const handleScroll = async () => {
      const scrollTop = document.documentElement.scrollTop;
      const scrollHeight = document.documentElement.scrollHeight;
      const clientHeight = document.documentElement.clientHeight;

      if (clientHeight >= scrollHeight - scrollTop) {
        if (repliesDataAll?.pages?.[repliesDataAll?.pages.length - 1]?.nextPage === null) {
          return;
        }

        await fetchNextPage();
      }
    };
    window.addEventListener('scroll', handleScroll);

    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [repliesDataAll]);

  const { mutate: deleteReview } = useExecute({
    executeFn: deleteReviewsReviewId,
  });

  const handleClickDeleteReview = async (reviewId: number) => {
    await deleteReview(
      { reviewId },
      guest?.guestToken
        ? {
            guestToken: guest?.guestToken,
          }
        : undefined,
    );

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

  const [openWriteReply, setOpenWriteReply] = useState(false);
  const handleClickToggleWriteReply = () => {
    if (!openWriteReply) {
      setOpenWriteReply(true);

      if (user) {
        track('reply_write_open', { actor_type: user.actorType, actor_id: user.actorId, isbn });
      }
      if (guest) {
        track('reply_write_open', { actor_type: guest.actorType, actor_id: guest.actorId, isbn });
      }
    } else {
      setOpenWriteReply(false);
    }
  };
  const handleClickCloseWriteReply = () => {
    setOpenWriteReply(false);
  };
  const handleReplyWritten = async () => {
    await refetchGetReplies();
    handleClickCloseWriteReply();
  };

  const handleClickAvatar = (isProfileAvailable: boolean) => {
    if (isProfileAvailable) {
      handleOpenDialog('AlertDialog');
      return;
    }
    track('navigate', {
      destination: 'members_library',
      source: 'book_reviews',
    });
  };

  const [dialog, setDialog] = useState<'UpdateReviewDialog' | 'AlertDialog' | null>(null);
  const handleOpenDialog = (dialog: 'UpdateReviewDialog' | 'AlertDialog') => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: 'UpdateReviewDialog' | 'AlertDialog' | null) => {
    switch (dialog) {
      case 'UpdateReviewDialog':
        return (
          <UpdateReviewDialog
            review={review}
            onReviewUpdated={onReviewsRefresh}
            onClose={handleCloseDialog}
          />
        );
      case 'AlertDialog':
        return (
          <Dialog onClose={handleCloseDialog}>
            <Dialog.Container>
              <Dialog.Body>접근이 불가능한 프로필입니다</Dialog.Body>
            </Dialog.Container>
          </Dialog>
        );

      default:
        return null;
    }
  };

  const dialogElement = renderDialog(dialog);

  const [isSpoilerVisible, setIsSpoilerVisible] = useState(false);
  const handleClickShowSpoiler = () => {
    if (!review.isSpoiler) return;

    setIsSpoilerVisible(true);
  };

  const showSpoilerVisible = isSpoilerVisible || !review.isSpoiler;

  return (
    <Entry line="bottom">
      <Entry.Main>
        <Entry.Header>
          <Shell>
            <Shell.Leading>
              <Avatar
                as={review.author?.memberId ? Link : 'div'}
                {...(review.author?.memberId && {
                  to: generatePath(ROUTES.MEMBER_LIBRARY, {
                    memberId: review.author.memberId.toString(),
                  }),
                })}
                onClick={() => {
                  handleClickAvatar(review.author.profileStatus !== 'AVAILABLE');
                }}
                img={review.author.profileImageUrl}
              />
            </Shell.Leading>
            <Shell.Content
              title={
                <>
                  {review.author.displayName}{' '}
                  {review.currentPage && (
                    <Badge variant="subtle" size="small">
                      P. {review.currentPage} 까지
                    </Badge>
                  )}
                </>
              }
              content={new Date(review.createdAt).toLocaleDateString('ko-KR')}
            />
            {review.author.mine && (
              <Shell.Trailing>
                <DropdownMenu
                  trigger={
                    <IconButton>
                      <Icon.EllipsisIcon />
                    </IconButton>
                  }
                  items={[
                    {
                      text: '수정',
                      onClick: () => {
                        handleOpenDialog('UpdateReviewDialog');
                      },
                    },
                    {
                      text: '식제',
                      onClick: () => {
                        handleClickDeleteReview(review.reviewId);
                      },
                    },
                  ]}
                />
              </Shell.Trailing>
            )}
          </Shell>
        </Entry.Header>
        <Entry.Body
          className={!showSpoilerVisible && review.isSpoiler && styles.clickable}
          onClick={handleClickShowSpoiler}
        >
          {showSpoilerVisible ? (
            <>
              {review.content}
              {review.quote && <Note>{review.quote}</Note>}
            </>
          ) : (
            <Surface variant="filled">
              <InfoMessage
                visual={<Icon.EyeOffIcon />}
                title="스포일러가 포함된 감상이에요"
                content={<Text underline>감상과 인용 보기</Text>}
              />
            </Surface>
          )}
        </Entry.Body>
        <Entry.Footer>
          <Button
            shape="link"
            variant="ghost"
            leading={
              review.likedByMe ? (
                <Icon.HeartOnIcon color="secondary" />
              ) : (
                <Icon.HeartOffIcon color="secondary" />
              )
            }
            disabled={!isAuthenticated}
            onClick={handleClickReviewReaction}
          >
            좋아요 {review.likeCount}
          </Button>
          <Button
            shape="link"
            variant="ghost"
            leading={<Icon.CommentIcon color="secondary" />}
            onClick={handleClickToggleWriteReply}
          >
            답글 {review.replyCount}
          </Button>
        </Entry.Footer>
      </Entry.Main>
      {(!!repliesData?.length || openWriteReply) && (
        <Entry.Extension>
          {openWriteReply && (
            <WriteReply
              isbn={isbn}
              reviewId={review.reviewId}
              onReplyWritten={handleReplyWritten}
            />
          )}
          {repliesData
            ?.filter((reply) => !reply.deleted)
            .map((reply) => {
              return (
                <BookReply
                  key={reply.replyId}
                  reply={reply}
                  showSpoilerVisible={showSpoilerVisible}
                  onShowSpoiler={handleClickShowSpoiler}
                  onRefetchGetReplies={refetchGetReplies}
                  onClickAvatar={handleClickAvatar}
                />
              );
            })}
        </Entry.Extension>
      )}
      {dialogElement}
    </Entry>
  );
};
