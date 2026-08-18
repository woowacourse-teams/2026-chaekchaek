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

export const BooksPage = () => {
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
              {Array.from({ length: 5 }).map(() => {
                return (
                  <List.Item>
                    <List.Item.Leading>
                      <ImgBox img={''} />
                    </List.Item.Leading>
                    <List.Item.Content
                      title="마션"
                      content="앤디 위어 · 박아람 옮김"
                      description="알에이치코리아 · SF · 2026 · 308쪽"
                    />
                    <List.Item.Trailing>
                      <Button>Button</Button>
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
