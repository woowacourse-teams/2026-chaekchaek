import { Layout } from '@chaekchaek/design-system';
import { Header } from '@chaekchaek/design-system';
import { Main } from '@chaekchaek/design-system';

import { Overview } from '@chaekchaek/design-system';
import { ImgBox } from '@chaekchaek/design-system';
// import DummyLargeImgBox from '../../components/ImgBox/imgs/dummy-large.png';
import { Split } from '@chaekchaek/design-system';
import { Title } from '@chaekchaek/design-system';
import { Banner } from '@chaekchaek/design-system';
import { Button } from '@chaekchaek/design-system';
import { ProgressBar } from '@chaekchaek/design-system';
import { SegmentedControl } from '@chaekchaek/design-system';
import { Entry } from '@chaekchaek/design-system';
import { Avatar } from '@chaekchaek/design-system';
// import DummyImgAvatar from '../../components/Avatar/imgs/dummy-avatar.png';
import { Shell } from '@chaekchaek/design-system';
import { Note } from '@chaekchaek/design-system';
import { Surface } from '@chaekchaek/design-system';
import { DataInfo } from '@chaekchaek/design-system';

export const BookDetailPage = () => {
  return (
    <Layout>
      <Header />
      <Main>
        <Overview>
          <Overview.Content
            leading="leading"
            title="Title"
            content="content"
            description="Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
          nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
          accusantium magni quis voluptates, velit nisi dolorum id."
          />
          <Overview.Media>
            <ImgBox img={''} />
          </Overview.Media>
        </Overview>
        <Split>
          <Split.Side>
            <Title level="main" trailing={<Button size="small">별점 주기</Button>}>
              내 독서 기록
            </Title>
            <Banner>
              <Banner.Content title="내 별점" content="아직 평가하지 않았어요" />
              <Banner.Trailing>
                <Button size="small" variant="primary">
                  별점 주기
                </Button>
              </Banner.Trailing>
            </Banner>
            <SegmentedControl
              shape="normal"
              value="wanttoread"
              options={[
                {
                  value: 'wanttoread',
                  text: '읽고 싶어요',
                },
                {
                  value: 'reading',
                  text: '읽는 중',
                },
                {
                  value: 'red',
                  text: '다 읽음',
                },
              ]}
            />

            <ProgressBar value={10} max={100} title="현재 읽은 범위" label="184 / 369쪽" />

            <Button variant="primary" block={true}>
              현재 읽은 쪽수 입력
            </Button>
            <DataInfo heading="책 정보">
              <DataInfo.Item title="장르" content="SF · 생존" />
              <DataInfo.Item title="출간" content="2026 초판" />
              <DataInfo.Item title="분량" content="369쪽" />
              <DataInfo.Item title="ISBN" content="978-89-0000-013" />
              <DataInfo.Item title="옮김" content="박아람" />
            </DataInfo>

            <Note>
              Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
              nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
              accusantium magni quis voluptates, velit nisi dolorum id.
            </Note>
          </Split.Side>
          <Split.Content>
            <Title
              level="main"
              trailing={
                <>
                  <SegmentedControl
                    value="all"
                    options={[
                      {
                        value: 'all',
                        text: '전체 피드',
                      },
                      {
                        value: 'mine',
                        text: '내 피드',
                      },
                    ]}
                  />
                </>
              }
            >
              이 책에 남긴 감상 30
            </Title>
            <Entry>
              <Entry.Main>
                <Entry.Header>
                  <Shell>
                    <Shell.Leading>
                      <Avatar img={''} />
                    </Shell.Leading>
                    <Shell.Content title="title" content="content" />
                    <Shell.Trailing>Trailing</Shell.Trailing>
                  </Shell>
                </Entry.Header>
                <Entry.Body>
                  Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum
                  possimus nobis quas error consequatur cumque nam recusandae dicta ab commodi,
                  reiciendis accusantium magni quis voluptates, velit nisi dolorum id.
                  <Note>
                    Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum
                    possimus nobis quas error consequatur cumque nam recusandae dicta ab commodi,
                    reiciendis accusantium magni quis voluptates, velit nisi dolorum id.
                  </Note>
                </Entry.Body>
                <Entry.Footer>
                  <Button size="small" leading={'♡'}>
                    좋아요 2
                  </Button>
                  <Button size="small" leading={'💬'}>
                    답글 2
                  </Button>
                </Entry.Footer>
              </Entry.Main>
              <Entry.Extension>
                <Surface>
                  <Shell>
                    <Shell.Leading>
                      <Avatar img={''} size="small" />
                    </Shell.Leading>
                    <Shell.Content title="title" content="content" />
                  </Shell>
                </Surface>
                <Surface>
                  <Shell>
                    <Shell.Leading>
                      <Avatar img={''} size="small" />
                    </Shell.Leading>
                    <Shell.Content title="title" content="content" />
                  </Shell>
                </Surface>
              </Entry.Extension>
            </Entry>
            <Entry variant="subtle">
              <Entry.Main>
                <Entry.Header>
                  <Shell>
                    <Shell.Leading>
                      <Avatar img={''} />
                    </Shell.Leading>
                    <Shell.Content title="title" content="content" />
                    <Shell.Trailing>Trailing</Shell.Trailing>
                  </Shell>
                </Entry.Header>
                <Entry.Body>
                  Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum
                  possimus nobis quas error consequatur cumque nam recusandae dicta ab commodi,
                  reiciendis accusantium magni quis voluptates, velit nisi dolorum id.
                  <Note>
                    Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum
                    possimus nobis quas error consequatur cumque nam recusandae dicta ab commodi,
                    reiciendis accusantium magni quis voluptates, velit nisi dolorum id.
                  </Note>
                </Entry.Body>
                <Entry.Footer>
                  <Button size="small" leading={'♡'}>
                    좋아요 2
                  </Button>
                  <Button size="small" leading={'💬'}>
                    답글 2
                  </Button>
                </Entry.Footer>
              </Entry.Main>
              <Entry.Extension>
                <Surface>
                  <Shell>
                    <Shell.Leading>
                      <Avatar img={''} size="small" />
                    </Shell.Leading>
                    <Shell.Content title="title" content="content" />
                  </Shell>
                </Surface>
                <Surface>
                  <Shell>
                    <Shell.Leading>
                      <Avatar img={''} size="small" />
                    </Shell.Leading>
                    <Shell.Content title="title" content="content" />
                  </Shell>
                </Surface>
              </Entry.Extension>
            </Entry>
          </Split.Content>
        </Split>
      </Main>
    </Layout>
  );
};
