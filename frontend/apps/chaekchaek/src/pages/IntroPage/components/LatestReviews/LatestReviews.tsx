import { useCallback, useState } from 'react';
import { generatePath, Link } from 'react-router-dom';

import { Avatar, Entry, Shell, ImgBox, Icon, Button, Dialog } from '@chaekchaek/design-system';

import { useLoadData } from '@/services/core/useLoadData';
import { getHomeLatestReviews } from '@/services/apis/homeLatestReviews/repository';

import { ROUTES } from '@/constants/routes';

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
    <div className={styles['scene-latest-reviews']}>
      <div className={styles['latest-reviews-title']}>
        <h1>방금 남겨진 문장</h1>
      </div>

      {data?.reviews.map((review, index) => {
        return (
          <Entry sx={{ mt: 0 }} reverse line="top" spacing="large" key={`${review.bookId}${index}`}>
            <Entry.Main>
              <Entry.Header>
                <Shell reverse>
                  <Shell.Leading>
                    <Link to={`/books/${review.isbn13}`}>
                      <ImgBox size="small" img={review.bookCoverImageUrl} />
                    </Link>
                  </Shell.Leading>
                  <Shell.Content
                    title={<Link to={`/books/${review.isbn13}`}>{review.bookTitle}</Link>}
                    content={
                      <>
                        <Avatar
                          size="x-small"
                          img={review.author.profileImageUrl}
                          as={review.author?.memberId ? Link : 'div'}
                          {...(review.author?.memberId && {
                            to: generatePath(ROUTES.MEMBER_LIBRARY, {
                              memberId: review.author.memberId.toString(),
                            }),
                          })}
                          onClick={() => {
                            if (review.author?.profileStatus !== 'AVAILABLE') {
                              handleOpenDialog('AlertDialog');
                            }
                          }}
                        />
                        {
                          <>
                            {review.author.displayName ?? review.author.anonymous}
                            {' · '}
                            {new Date(review.createdAt).toLocaleDateString('ko-KR')}
                          </>
                        }
                      </>
                    }
                  />
                </Shell>
              </Entry.Header>
              <Entry.Body>
                <Link to={`/books/${review.isbn13}`}>{review.content}</Link>
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
        );
      })}
      {dialogElement}
    </div>
  );
};
