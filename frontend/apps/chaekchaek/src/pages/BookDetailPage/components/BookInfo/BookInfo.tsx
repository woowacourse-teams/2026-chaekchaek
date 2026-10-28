import { Link } from 'react-router-dom';

import { Button, DataInfo, Surface, Title } from '@chaekchaek/design-system';

import Yes24LogoImg from './imgs/yes24.svg';

import type { BookInfoProps } from './BookInfo.types';

export const BookInfo = ({
  category,
  publishedDate,
  isbn13,
  authors,
  translators,
  purchaseUrl,
}: BookInfoProps) => {
  const formattedCategory = category?.replace(/\s*>\s*/g, ' > ');

  return (
    <>
      <Surface variant="outlined" spacing="medium">
        <Title level="main">내 독서 기록</Title>
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
