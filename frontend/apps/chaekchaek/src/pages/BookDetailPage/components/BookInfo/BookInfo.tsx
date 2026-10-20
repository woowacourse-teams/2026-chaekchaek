import { Link } from 'react-router-dom';

import {
  Banner,
  Button,
  Choice,
  DataInfo,
  Icon,
  ProgressBar,
  Rating,
  Surface,
  Title,
} from '@chaekchaek/design-system';

import { track } from '@/analytics/track';

import Yes24LogoImg from './imgs/yes24.svg';

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
  purchaseUrl,
  onRegistryLibrary,
  onRatingCreate,
  onReadingStatusChange,
  onCurrentPageUpdate,
}: BookInfoProps) => {
  const readPageCount = currentPage || 0;
  const bookPageCount = totalPages || 0;
  const formattedCategory = category?.replace(/\s*>\s*/g, ' > ');

  const handleClickRating = () => {
    if (!myRecord) return onRegistryLibrary();

    track('rating_open');
    onRatingCreate();
  };

  const handleClickCurrentPage = () => {
    if (!myRecord) return onRegistryLibrary();

    track('current_page_open');

    onCurrentPageUpdate();
  };

  return (
    <>
      <Surface variant="outlined" spacing="medium">
        <Title
          level="main"
          trailing={
            <Button
              size="x-small"
              shape="link"
              variant="ghost"
              leading={<Icon.StartIcon color="inverse" />}
              onClick={handleClickRating}
            >
              <Icon.StartIcon sx={{ mr: 2 }} />
              별점 주기
            </Button>
          }
        >
          내 독서 기록
        </Title>
        <Choice
          sx={{ mt: 4 }}
          shape="normal"
          value={readingStatus}
          options={[
            { value: 'WANT_TO_READ', text: '읽고 싶어요' },
            { value: 'READING', text: '읽는 중' },
            { value: 'FINISHED', text: '다 읽음' },
          ]}
          onChange={onReadingStatusChange}
        />
        <Banner sx={{ mt: 4 }}>
          <Banner.Content
            title="내 별점"
            content={
              myRecord?.myRating ? (
                <Rating size="small" value={myRecord.myRating} />
              ) : (
                '아직 평가하지 않았어요'
              )
            }
          />
        </Banner>
        <Title
          level="caption"
          trailing={
            <Button size="x-small" shape="link" variant="ghost" onClick={handleClickCurrentPage}>
              현재 읽은 쪽수 입력
            </Button>
          }
        >
          현재 읽은 범위
        </Title>
        <ProgressBar
          sx={{ mt: 6 }}
          variant={readPageCount !== bookPageCount ? 'accent' : 'subtle'}
          value={readPageCount}
          max={bookPageCount}
          title={`${readPageCount} / ${bookPageCount}쪽`}
          label={`${Math.ceil(readPageCount / bookPageCount)}%`}
        />
      </Surface>
      <DataInfo sx={{ mt: 4 }} heading="책 정보">
        {formattedCategory && <DataInfo.Item title="장르" content={formattedCategory} />}
        {publishedDate && <DataInfo.Item title="출간" content={publishedDate} />}
        {isbn13 && <DataInfo.Item title="ISBN" content={isbn13} />}
        {!!authors?.length && <DataInfo.Item title="지은이" content={authors.join(' · ')} />}
        {!!translators?.length && <DataInfo.Item title="옮김" content={translators.join(' · ')} />}
      </DataInfo>
      {purchaseUrl && (
        <Button as={Link} variant="ghost" block sx={{ mt: 4 }} to={purchaseUrl} target="_blank">
          <img src={Yes24LogoImg} alt="yes24" />
          에서 보기
        </Button>
      )}
    </>
  );
};
