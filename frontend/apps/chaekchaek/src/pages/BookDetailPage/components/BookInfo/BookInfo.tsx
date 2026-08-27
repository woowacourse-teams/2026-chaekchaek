import {
  Banner,
  Button,
  DataInfo,
  Icon,
  ProgressBar,
  SegmentedControl,
  Title,
} from '@chaekchaek/design-system';

import type { BookInfoProps } from './BookInfo.types';

export const BookInfo = ({
  myRecord,
  readingStatus,
  currentPage,
  totalPages,
  category,
  publishedDate,
  isbn13,
  authors,
  translators,
  onRegistryLibrary,
  onRatingCreate,
  onReadingStatusChange,
  onCurrentPageUpdate,
}: BookInfoProps) => {
  const readPageCount = currentPage || 0;
  const bookPageCount = totalPages || 0;

  const handleClickRating = () => {
    if (!myRecord) return onRegistryLibrary();
    onRatingCreate();
  };

  const handleClickCurrentPage = () => {
    if (!myRecord) return onRegistryLibrary();
    onCurrentPageUpdate();
  };

  return (
    <>
      <Title level="main">내 독서 기록</Title>
      <Banner sx={{ mt: 6 }}>
        <Banner.Content title="내 별점" content="아직 평가하지 않았어요" />
        <Banner.Trailing>
          <Button
            size="small"
            variant="primary"
            leading={<Icon.StartIcon color="inverse" />}
            onClick={handleClickRating}
          >
            별점 주기
          </Button>
        </Banner.Trailing>
      </Banner>
      <SegmentedControl
        sx={{ mt: 6 }}
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
        sx={{ mt: 6 }}
        value={readPageCount}
        max={bookPageCount}
        title="현재 읽은 범위"
        label={`${readPageCount} / ${bookPageCount}쪽`}
      />
      <Button sx={{ mt: 2 }} variant="primary" block={true} onClick={handleClickCurrentPage}>
        현재 읽은 쪽수 입력
      </Button>
      <DataInfo sx={{ mt: 6 }} heading="책 정보">
        {category && <DataInfo.Item title="장르" content={category} />}
        {publishedDate && <DataInfo.Item title="출간" content={publishedDate} />}
        {isbn13 && <DataInfo.Item title="ISBN" content={isbn13} />}
        {!!authors?.length && <DataInfo.Item title="지은이" content={authors.join(' · ')} />}
        {!!translators?.length && <DataInfo.Item title="옮김" content={translators.join(' · ')} />}
      </DataInfo>
    </>
  );
};
