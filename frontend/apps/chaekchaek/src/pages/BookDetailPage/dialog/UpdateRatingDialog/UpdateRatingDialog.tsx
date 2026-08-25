import { useCallback, useState } from 'react';
import type { ChangeEvent } from 'react';

import { Dialog, Slider } from '@chaekchaek/design-system';
import { Button } from '@chaekchaek/design-system';
import { Rating } from '@chaekchaek/design-system';
import { CellList } from '@chaekchaek/design-system';

import { useLoadData } from '@/services/core/useLoadData';
import { useExecute } from '@/services/core/useExecute';
import { getMembersMeRatingsComparison } from '@/services/apis/membersMeRatingsComparison/repository';
import { putLibraryBookIdRating } from '@/services/apis/libraryBookIdRating/repository';

import type { Props } from './UpdateRatingDialog.types';

const RatingMessages = {
  0.5: '아쉬웠어요',
  1.0: '별로였어요',
  1.5: '조금 아쉬웠어요',
  2.0: '아쉬운 점이 많았어요',
  2.5: '그저 그랬어요',
  3.0: '괜찮았어요',
  3.5: '꽤 좋았어요',
  4.0: '좋았어요',
  4.5: '정말 좋았어요',
  5.0: '최고였어요',
};

const getRatingMessageNumber = (rating: number) => {
  return Math.round(rating * 2) / 2;
};

export const UpdateRatingDialog = ({
  isbn13,
  bookId,
  title,
  rating: defaultRating,
  myRatingCount,
  onRatingUpdated,
  onClose,
}: Props) => {
  const [rating, setRating] = useState(defaultRating);

  const handleChangeRating = (rating: number) => {
    setRating(rating);
  };

  const getMembersMeRatingsComparisonLoadData = useCallback(async () => {
    return getMembersMeRatingsComparison({
      isbn13,
      criterion: rating,
    });
  }, [rating]);

  const {
    status: { data: ratingsComparison },
  } = useLoadData({ queryFn: getMembersMeRatingsComparisonLoadData });

  const ratingMessageNumber = getRatingMessageNumber(rating);
  const ratingMessage = RatingMessages[ratingMessageNumber as keyof typeof RatingMessages] || '-';

  const { mutate } = useExecute({
    executeFn: putLibraryBookIdRating,
  });

  const handleSubmit = async () => {
    await mutate({ bookId, rating });
    onRatingUpdated();
    onClose();
  };

  const ratingsComparisonData =
    ratingsComparison &&
    [ratingsComparison?.lower, ratingsComparison.current, ratingsComparison?.higher].filter(
      Boolean,
    );

  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header
          subTitle={`${title}을 읽은 느낌을 별점으로 남겨보세요. 별점은 언제든 수정할 수 있어요.`}
        >
          이 책에 별점 매기기
        </Dialog.Header>
        <Dialog.Body>
          <CellList
            title={
              <>
                <span>내 평점 기록</span>
                <span>{myRatingCount}회</span>
              </>
            }
          >
            {ratingsComparisonData?.map((comparison) => {
              return (
                <CellList.Item
                  headline={comparison.myRating}
                  title={comparison.title}
                  content={comparison?.ratingUpdatedAt}
                />
              );
            })}
          </CellList>
          <Slider
            value={rating || 0}
            step={0.1}
            min={0}
            max={5}
            onChange={(e: ChangeEvent<HTMLInputElement>) => {
              handleChangeRating(e.currentTarget.valueAsNumber);
            }}
          />
          <Rating
            value={rating || 0}
            onChange={handleChangeRating}
            title={'내 별점'}
            description={`${rating || 0} · ${ratingMessage}`}
            block
          />
        </Dialog.Body>
        <Dialog.Footer>
          <Button variant="ghost" block>
            취소
          </Button>
          <Button variant="primary" block onClick={handleSubmit}>
            별점 저장
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
