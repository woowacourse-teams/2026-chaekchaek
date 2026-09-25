import { Avatar, Entry, Shell, ImgBox, Icon, Button, scrollbar } from '@chaekchaek/design-system';

import { useLoadData } from '@/services/core/useLoadData';
import { getHomeLatestReviews } from '@/services/apis/homeLatestReviews/repository';

import styles from './LatestReviews.module.css';
import { useCallback } from 'react';

export const LatestReviews = () => {
  const getHomeLatestReviewsLoadData = useCallback(async () => {
    return await getHomeLatestReviews({});
  }, []);
  const {
    status: { data },
  } = useLoadData({
    queryFn: getHomeLatestReviewsLoadData,
  });

  return (
    <div className={`${styles['scene-latest-reviews']} ${scrollbar.dark}`}>
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
                    <ImgBox size="small" img={review.bookCoverImageUrl} />
                  </Shell.Leading>
                  <Shell.Content
                    title={review.bookTitle}
                    content={
                      <>
                        <Avatar size="x-small" img={review.author.profileImageUrl} />
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
              <Entry.Body>{review.content}</Entry.Body>
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
    </div>
  );
};
