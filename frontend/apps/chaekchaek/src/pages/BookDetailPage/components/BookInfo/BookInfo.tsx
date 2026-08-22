import {
  Banner,
  Button,
  DataInfo,
  ProgressBar,
  SegmentedControl,
  Title,
} from '@chaekchaek/design-system';

import type { BookInfoProps } from './BookInfo.types';

export const BookInfo = ({
  readingStatus,
  currentPage,
  totalPages,
  category,
  publishedDate,
  isbn13,
  authors,
  translators,
  onRatingCreate,
  onReadingStatusChange,
  onCurrentPageUpdate,
}: BookInfoProps) => {
  const readPageCount = currentPage || 0;
  const bookPageCount = totalPages || 0;

  return (
    <>
      <Title level="main">내 독서 기록</Title>
      <Banner>
        <Banner.Content title="내 별점" content="아직 평가하지 않았어요" />
        <Banner.Trailing>
          <Button size="small" variant="primary" onClick={onRatingCreate}>
            별점 주기
          </Button>
        </Banner.Trailing>
      </Banner>
      <SegmentedControl
        shape="normal"
        value={readingStatus}
        options={[
          { value: 'WANT_TO_READ', text: '읽고 싶어요' },
          { value: 'READING', text: '읽는 중' },
          { value: 'FINISHED', text: '다 읽음' },
        ]}
        onChange={onReadingStatusChange}
      />
      <ProgressBar
        value={readPageCount}
        max={bookPageCount}
        title="현재 읽은 범위"
        label={`${readPageCount} / ${bookPageCount}쪽`}
      />
      <Button variant="primary" block={true} onClick={onCurrentPageUpdate}>
        현재 읽은 쪽수 입력
      </Button>
      <DataInfo heading="책 정보">
        {category && <DataInfo.Item title="장르" content={category} />}
        {publishedDate && <DataInfo.Item title="출간" content={publishedDate} />}
        {isbn13 && <DataInfo.Item title="ISBN" content={isbn13} />}
        {!!authors?.length && <DataInfo.Item title="지은이" content={authors.join(' · ')} />}
        {!!translators?.length && <DataInfo.Item title="옮김" content={translators.join(' · ')} />}
      </DataInfo>
    </>
  );
};
