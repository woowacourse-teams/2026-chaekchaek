import { useState } from 'react';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { Button, Choice, Field, Input, Select, Text, Title } from '@chaekchaek/design-system';

import { track } from '@/analytics/track';

import { BookReview } from './BookReview';
import type { BookReviewsProps } from './BookReviews.types';

import { WriteReviewDialog } from '../../dialog/WriteReviewDialog';

export const BookReviews = ({
  bookId,
  isbn,
  sort,
  feed,
  count,
  myRating,
  reviews,
  onReviewsRefresh,
  onSortChange,
  onFeedChange,
}: BookReviewsProps) => {
  const { user, guest } = useAuthContext();

  const [dialog, setDialog] = useState<'WriteReviewDialog' | null>(null);
  const handleOpenDialog = (dialog: 'WriteReviewDialog') => {
    setDialog(dialog);

    if (user) {
      track('review_write_open', { actor_type: user.actorType, actor_id: user.actorId, isbn });
    }
    if (guest) {
      track('review_write_open', { actor_type: guest.actorType, actor_id: guest.actorId, isbn });
    }
  };
  const handleCloseDialog = () => {
    setDialog(null);
  };

  const renderDialog = (dialog: 'WriteReviewDialog' | null) => {
    switch (dialog) {
      case 'WriteReviewDialog':
        return (
          bookId && (
            <WriteReviewDialog
              bookId={bookId}
              isbn={isbn}
              myRating={myRating}
              onReviewWritten={onReviewsRefresh}
              onClose={handleCloseDialog}
            />
          )
        );

      default:
        return null;
    }
  };

  const dialogElement = renderDialog(dialog);

  return (
    <>
      <Title
        level="main"
        trailing={
          <>
            <Select
              size="small"
              value={sort}
              options={[
                { value: 'LATEST', text: '최신순' },
                { value: 'OLDEST', text: '오래된순' },
                { value: 'POPULAR', text: '인기순' },
              ]}
              onChange={onSortChange}
            />
          </>
        }
      >
        이 책에 남긴 감상 {count}
      </Title>
      <Choice
        distribution="content"
        sx={{ mt: 4, mb: 4 }}
        value={feed}
        options={[
          { value: 'ALL', text: '전체 피드' },
          { value: 'MINE', text: '내 피드' },
        ]}
        onChange={onFeedChange}
      />

      <Button
        variant="primary"
        block
        align="left"
        sx={{ mb: 4 }}
        leading="✎"
        onClick={() => {
          handleOpenDialog('WriteReviewDialog');
        }}
      >
        이 순간의 감상 남기기{' '}
        <Text color="accent" sx={{ ml: 2 }}>
          ↗
        </Text>
      </Button>

      {reviews
        ?.filter((review) => !review.deleted)
        .map((review) => {
          return (
            <BookReview
              key={review.reviewId}
              isbn={isbn}
              review={review}
              onReviewsRefresh={onReviewsRefresh}
            />
          );
        })}

      {dialogElement}
    </>
  );
};
