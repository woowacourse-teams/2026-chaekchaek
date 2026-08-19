import { Layout } from '@chaekchaek/design-system';
import { Header } from '@chaekchaek/design-system';
import { Main } from '@chaekchaek/design-system';

import { Split } from '@chaekchaek/design-system';
import { OptionList } from '@chaekchaek/design-system';
import { Title } from '@chaekchaek/design-system';
import { List } from '@chaekchaek/design-system';
import { ImgBox } from '@chaekchaek/design-system';
// import DummyImgImgBox from '../../ImgBox/imgs/dummy.png';
import { Button } from '@chaekchaek/design-system';
import { Input } from '@chaekchaek/design-system';

import { getBooks } from '@/services/apis/books/repository';
import { useLoadData } from '@/services/core/useLoadData';

const getBooksLoadData = async () => {
  return await getBooks({ page: 1, query: '마션' });
};

export const BooksPage = () => {
  const {
    status: { data },
  } = useLoadData({
    queryFn: getBooksLoadData,
  });

  return (
    <Layout>
      <Header />
      <Main>
        <Split>
          <Split.Side>
            <Title
              level="page"
              orientation="vertical"
              trailing={
                <>
                  <Input block />
                </>
              }
            >
              책 찾기
            </Title>
            <OptionList
              title="필터"
              value="all"
              options={[
                { value: 'all', text: '전체' },
                { value: 'novel', text: '소설' },
              ]}
            />
          </Split.Side>
          <Split.Content>
            <Title level="main" trailing={<></>}>
              '마션' 검색 결과
            </Title>
            <List>
              {!!data?.items.length &&
                data?.items.map((item) => {
                  return (
                    <List.Item>
                      <List.Item.Leading>
                        <ImgBox img={item.coverImageUrl} />
                      </List.Item.Leading>
                      <List.Item.Content
                        title={item.title}
                        content={item.authors.join(' · ')}
                        description={`${item.publisher} · ${item.publishedDate}`}
                      />
                      <List.Item.Trailing>
                        {item.commentCount && (
                          <Button variant="ghost" size="small">
                            댓글 {item.commentCount}
                          </Button>
                        )}
                        <Button variant="primary">읽는 중 시작</Button>
                      </List.Item.Trailing>
                    </List.Item>
                  );
                })}
            </List>
          </Split.Content>
        </Split>
      </Main>
    </Layout>
  );
};
