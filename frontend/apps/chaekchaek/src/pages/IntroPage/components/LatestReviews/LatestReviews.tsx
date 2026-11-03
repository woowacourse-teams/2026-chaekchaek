import { useCallback, useState } from 'react';
import { generatePath, Link } from 'react-router-dom';

import {
  Avatar,
  Entry,
  Shell,
  ImgBox,
  Icon,
  IconButton,
  Button,
  Dialog,
  Partition,
  scrollbar,
  Media,
} from '@chaekchaek/design-system';

import { useLoadData } from '@/services/core/useLoadData';
import { getHomeLatestReviews } from '@/services/apis/homeLatestReviews/repository';

import { ROUTES } from '@/constants/routes';

import { track } from '@/analytics/track';

import styles from './LatestReviews.module.css';

export const LatestReviews = () => {
  const getHomeLatestReviewsLoadData = useCallback(async () => {
    return await getHomeLatestReviews({});
  }, []);
  const {
    status: { data },
  } = useLoadData({
    queryFn: getHomeLatestReviewsLoadData,
  });

  const handleClickReview = () => {
    track('navigate', {
      destination: 'book_detail',
      source: 'intro_latest_reviews',
    });
  };

  const handleClickAvatar = (isProfileAvailable: boolean) => {
    if (isProfileAvailable) {
      handleOpenDialog('AlertDialog');
      return;
    }
    track('navigate', {
      destination: 'members_library',
      source: 'intro_latest_reviews',
    });
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
    <div className={`${styles['scene-latest-reviews']} ${scrollbar.dark}`}>
      <div className={styles['latest-reviews-title']}>
        <h1>방금 남겨진 문장</h1>
        <div className={styles['latest-reviews-title-right']}>
          <IconButton as={Link} to={ROUTES.FEED} target="_blank">
            <Icon.ExternalLinkIcon color="secondary" />
          </IconButton>
        </div>
      </div>

      {data?.reviews.map((review, index) => {
        return (
          <>
            <Partition>
              <Partition.Item>
                <Link to={`/books/${review.isbn13}`} onClick={handleClickReview}>
                  <Media
                    media={<ImgBox size="medium" reverse img={review.bookCoverImageUrl} />}
                    sx={{ ml: 8 }}
                  />
                </Link>
              </Partition.Item>
              <Partition.Item>
                <Entry reverse line="top" spacing="large" key={`${review.bookId}${index}`}>
                  <Entry.Main>
                    <Entry.Header>
                      <Shell reverse>
                        <Shell.Content
                          title={
                            <Link to={`/books/${review.isbn13}`} onClick={handleClickReview}>
                              {review.bookTitle}
                            </Link>
                          }
                          content={
                            <>
                              <Avatar
                                size="small"
                                img={review.author.profileImageUrl}
                                as={review.author?.memberId ? Link : 'div'}
                                {...(review.author?.memberId && {
                                  to: generatePath(ROUTES.LEGACY_MEMBER_LIBRARY, {
                                    memberId: review.author.memberId.toString(),
                                  }),
                                })}
                                onClick={() =>
                                  handleClickAvatar(review.author.profileStatus === 'AVAILABLE')
                                }
                              />
                              {review.author.displayName ?? review.author.anonymous}
                              {' · '}
                              {new Date(review.createdAt).toLocaleDateString('ko-KR')}
                            </>
                          }
                        />
                      </Shell>
                    </Entry.Header>
                    <Entry.Body>
                      <Link to={`/books/${review.isbn13}`} onClick={handleClickReview}>
                        {review.content}
                      </Link>
                    </Entry.Body>
                    <Entry.Footer>
                      <Button
                        shape="link"
                        size="small"
                        inverse
                        leading={<Icon.CommentIcon color="inverse" />}
                        disabled
                      >
                        {review.replyCount}
                      </Button>
                    </Entry.Footer>
                  </Entry.Main>
                </Entry>
              </Partition.Item>
            </Partition>
          </>
        );
      })}
      {dialogElement}
    </div>
  );
};
