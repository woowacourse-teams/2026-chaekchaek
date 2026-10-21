import { ImgBox, Input, List, ScrollArea } from '@chaekchaek/design-system';

import ImgBoxDummyImg1 from '../../../../../../../../../packages/design-system/src/components/ImgBox/imgs/dummy.png';
import { Link } from 'react-router-dom';

export const SearchPanel = () => {
  return (
    <>
      <Input sx={{ mt: 2, mb: 4 }} size="small" />

      <ScrollArea height="320px">
        <List>
          {Array.from({ length: 10 }).map(() => {
            const item = {
              bookId: 1,
              isbn13: '',
              coverImageUrl: ImgBoxDummyImg1,
              title: 'Title Title Title Title Title Title',
              authors: ['author1', 'author2'],
            };

            return (
              <List.Item key={item.bookId}>
                <List.Item.Leading>
                  <Link to={`/books/${item.isbn13}`}>
                    <ImgBox img={item.coverImageUrl} size="x-small" />
                  </Link>
                </List.Item.Leading>
                <List.Item.Content
                  ellipsis
                  title={<>{item.title}</>}
                  content={item.authors.join(' · ')}
                />
                <List.Item.Trailing>담긴 책</List.Item.Trailing>
              </List.Item>
            );
          })}
        </List>
      </ScrollArea>
    </>
  );
};
