import {
  Button,
  ImgBox,
  List,
  OptionList,
  Pagination,
  ProgressBar,
  Select,
  Split,
  Tag,
  Title,
} from '@chaekchaek/design-system';

import { Layout } from '@/frames';
import { Header } from '@/frames';
import { Main } from '@/frames';

export const LibraryPage = () => {
  return (
    <Layout>
      <Header />
      <Main>
        <Title
          level="page"
          trailing={
            <>
              <Button variant="ghost">감상 익명 공개</Button>
              <Button variant="primary">서재 편집</Button>
            </>
          }
        >
          내 서재
        </Title>
        <Split>
          <Split.Side>
            <Title
              level="main"
              orientation="vertical"
              trailing={
                <OptionList
                  value=""
                  options={[
                    {
                      value: '',
                      text: '전체',
                      meta: '0',
                    },
                    {
                      value: 'WANT_TO_READ',
                      text: '읽고 싶어요',
                      meta: '0',
                    },
                    {
                      value: 'READING',
                      text: '읽는 중',
                      meta: '0',
                    },
                    {
                      value: 'FINISHED',
                      text: '다 읽음',
                      meta: '0',
                    },
                  ]}
                />
              }
            >
              독서 상태
            </Title>
          </Split.Side>
          <Split.Content>
            <Title
              level="caption"
              trailing={
                <>
                  <Select
                    value="RECENT"
                    options={[
                      { value: 'RECENT', text: '최근순' },
                      { value: 'OLDEST', text: '오래된순' },
                      { value: 'COMMENT', text: '감상순' },
                      { value: 'RATING', text: '별점순' },
                      { value: 'TITLE', text: '제목순' },
                    ]}
                  />
                </>
              }
            >
              독서 상태
            </Title>
            <List>
              <List.Item>
                <List.Item.Leading>
                  <a href="#">
                    <ImgBox img={''} size="small" />
                  </a>
                </List.Item.Leading>
                <List.Item.Content
                  title={
                    <>
                      <Tag variant="primary">읽는중</Tag>
                      <br />
                      Title
                    </>
                  }
                  content={'content'}
                  description={
                    <>
                      description
                      <ProgressBar value={30} max={300} />
                    </>
                  }
                />
                <List.Item.Trailing>&gt;</List.Item.Trailing>
              </List.Item>
            </List>

            <Pagination defaultPage={1} totalPages={10} />
          </Split.Content>
        </Split>
      </Main>
    </Layout>
  );
};
