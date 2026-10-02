import { useState, type MouseEvent } from 'react';
import { generatePath, Link } from 'react-router-dom';

import {
  Avatar,
  Badge,
  Button,
  Dialog,
  Divider,
  Entry,
  Icon,
  ImgBox,
  Media,
  Note,
  Partition,
  Shell,
  Text,
} from '@chaekchaek/design-system';

import { ROUTES } from '@/constants/routes';

import { track } from '@/analytics/track';

import {
  deleteReviewsReviewIdReactions,
  postReviewsReviewIdReactions,
} from '@/services/apis/reviewsReviewIdReactions/repository';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { useExecute } from '@/services/core/useExecute';

import styles from './BookFeed.module.css';

import type { BookFeedProps } from './BookFeed.types';

const SPOILER_PLACEHOLDER_REVIEW = '짹짹짹 짹짹 짹짹짹짹. 짹짹짹 짹짹짹 짹짹짹 짹짹짹짹 짹짹짹짹.';

export const BookFeed = (props: BookFeedProps) => {
  const { review, onFeedRefresh } = props;

  const { isAuthenticated } = useAuthContext();

  const [isSpoilerVisible, setIsSpoilerVisible] = useState(false);

  const showSpoilerVisible = isSpoilerVisible || !review.isSpoiler;

  const handleClickShowSpoiler = (e: MouseEvent<HTMLAnchorElement>) => {
    if (!showSpoilerVisible) {
      e.preventDefault();

      setIsSpoilerVisible(true);
      return;
    }

    track('navigate', {
      destination: 'book_detail',
      source: 'book_feed',
    });
  };

  const handleClickBook = () => {
    track('navigate', {
      destination: 'book_detail',
      source: 'book_feed',
    });
  };

  const handleClickAvatar = (isProfileAvailable: boolean) => {
    if (isProfileAvailable) {
      handleOpenDialog('AlertDialog');
      return;
    }

    track('navigate', {
      destination: 'members_library',
      source: 'book_feed',
    });
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

    await onFeedRefresh?.();
  };

  const [dialog, setDialog] = useState<'AlertDialog' | null>(null);
  const handleOpenDialog = (dialog: 'AlertDialog') => {
    setDialog(dialog);
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: 'AlertDialog' | null) => {
    switch (dialog) {
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

  return (
    <>
      <Divider />
      <Partition key={review.reviewId}>
        <Partition.Item>
          <Link to={`/books/${review.isbn13}`} onClick={handleClickBook}>
            <Media
              media={<ImgBox size="large" img={review.bookCoverImageUrl} />}
              title={review.bookTitle}
              description={review.bookAuthors.join(' · ')}
            />
          </Link>
        </Partition.Item>
        <Partition.Item>
          <Entry line="none" variant="bare">
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
                          <Badge variant="subtle" size="x-small" sx={{ ml: 2 }}>
                            P. {review.currentPage} 까지
                          </Badge>
                        )}
                      </>
                    }
                    content={new Date(review.createdAt).toLocaleDateString('ko-KR')}
                  />
                </Shell>
              </Entry.Header>
              <Entry.Body>
                <Link to={`/books/${review.isbn13}`} onClick={handleClickShowSpoiler}>
                  {showSpoilerVisible ? (
                    review.content
                  ) : (
                    <>
                      <span className={styles.spoiler}>{SPOILER_PLACEHOLDER_REVIEW}</span>
                      <Text size="small" color="error">
                        (스포일러 · 눌러보기)
                      </Text>
                    </>
                  )}
                  {review.quote && (
                    <Note variant="default" sx={{ mt: 4 }}>
                      {showSpoilerVisible ? (
                        review.quote
                      ) : (
                        <span className={styles.spoiler}>{SPOILER_PLACEHOLDER_REVIEW}</span>
                      )}
                    </Note>
                  )}
                </Link>
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
                  readOnly
                >
                  답글 {review.replyCount}
                </Button>
              </Entry.Footer>
            </Entry.Main>
          </Entry>
        </Partition.Item>
      </Partition>
      {dialogElement}
    </>
  );
};
