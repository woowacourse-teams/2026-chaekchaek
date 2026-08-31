import { useState } from 'react';

import { Button, Field, Input, SegmentedControl, Select, Title } from '@chaekchaek/design-system';

import { BookReview } from './BookReview';
import type { BookReviewsProps } from './BookReviews.types';

import { WriteReviewDialog } from '../../dialog/WriteReviewDialog';

export const BookReviews = ({
  bookId,
  isbn,
  sort,
  feed,
  count,
  reviews,
  onReviewsRefresh,
  onSortChange,
  onFeedChange,
}: BookReviewsProps) => {
  const [dialog, setDialog] = useState<'WriteReviewDialog' | null>(null);
  const handleOpenDialog = (dialog: 'WriteReviewDialog') => {
    setDialog(dialog);
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
              value={sort}
              options={[
                { value: 'LATEST', text: '최신순' },
                { value: 'OLDEST', text: '오래된순' },
                { value: 'POPULAR', text: '인기순' },
                { value: 'PAGE', text: '페이지순' },
              ]}
              onChange={onSortChange}
            />
            <SegmentedControl
              value={feed}
              options={[
                { value: 'ALL', text: '전체 피드' },
                { value: 'MINE', text: '내 피드' },
              ]}
              onChange={onFeedChange}
            />
          </>
        }
      >
        이 책에 남긴 감상 {count}
      </Title>
      <Field sx={{ mt: 4, mb: 6 }}>
        <Field.Content
          onClick={() => {
            handleOpenDialog('WriteReviewDialog');
          }}
        >
          <Input />
          <Button variant="primary">남기기</Button>
        </Field.Content>
      </Field>
      {reviews
        ?.filter((review) => !review.deleted)
        .map((review) => {
          return (
            <BookReview key={review.reviewId} review={review} onReviewsRefresh={onReviewsRefresh} />
          );
        })}
      <Field>
        <Field.Content
          onClick={() => {
            handleOpenDialog('WriteReviewDialog');
          }}
        >
          <Input
            trailing={
              <Button variant="primary" size="small">
                남기기
              </Button>
            }
          />
        </Field.Content>
      </Field>
      {dialogElement}
    </>
  );
};
