import { useState } from 'react';

import { Dialog } from '@chaekchaek/design-system';
import { Button } from '@chaekchaek/design-system';
import { Rating } from '@chaekchaek/design-system';
import { CellList } from '@chaekchaek/design-system';

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

export const UpdateRatingDialog = ({ bookId, title, rating: defaultRating }: Props) => {
  const [rating, setRating] = useState(defaultRating);

  const handleChangeRating = (rating: number) => {
    setRating(rating);
  };

  const ratingMessage = RatingMessages[rating as keyof typeof RatingMessages] || '-';

  return (
    <Dialog>
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
                <span>5회</span>
              </>
            }
          >
            <CellList.Item headline="3.1" title="불고기는 존재하지…" content="2026.04.03" />
            <CellList.Item headline="3.5" title="보이지 않는 도시" content="2026.05.12" />
            <CellList.Item headline="4.0" title="역병" content="2026.06.21" />
            <CellList.Item headline="4.2" title="아몬드" content="2026.07.18" />
            <CellList.Item headline="4.0" title="마션" content="2026.08.05" />
          </CellList>
          <Rating
            value={rating || 0}
            onChange={handleChangeRating}
            title={'내 별점'}
            description={`${rating || 0} · ${ratingMessage}`}
          />
        </Dialog.Body>
        <Dialog.Footer>
          <Button variant="ghost" block>
            취소
          </Button>
          <Button variant="primary" block>
            별점 저장
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
