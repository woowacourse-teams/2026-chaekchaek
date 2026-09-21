import { Badge, ImgBox, Overview } from '@chaekchaek/design-system';

import type { Props } from './BookOverview.types';

export const BookOverview = ({
  category,
  publishedDate,
  title,
  authors,
  publisher,
  description,
  averageRating,
  reviewCount,
  replyCount,
  coverImageUrl,
}: Props) => {
  const formattedCategory = category?.replace(/\s*>\s*/g, ' > ');

  return (
    <Overview>
      <Overview.Content
        leading={`ARCHIVE / ${formattedCategory} / ${publishedDate}`}
        title={title}
        content={`${authors} · ${publisher}`}
        description={description}
        meta={
          <>
            <Badge variant="ghost" reverse>
              ★ {averageRating || 0}
            </Badge>
            <Badge variant="ghost" reverse>
              감상 {reviewCount || 0} · 답글: {replyCount}
            </Badge>
          </>
        }
      />
      <Overview.Media>{coverImageUrl && <ImgBox img={coverImageUrl} />}</Overview.Media>
    </Overview>
  );
};
