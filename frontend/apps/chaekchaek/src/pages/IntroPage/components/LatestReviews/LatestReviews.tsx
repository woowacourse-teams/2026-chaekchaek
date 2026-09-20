import { Avatar, Entry, Shell, ImgBox, Icon, Button } from '@chaekchaek/design-system';

import styles from './LatestReviews.module.css';

export const LatestReviews = () => {
  return (
    <div className={styles['scene-latest-reviews']}>
      <div className={styles['latest-reviews-title']}>
        <h1>방금 남겨진 문장</h1>
        <div className="right">
          <Button shape="link" variant="ghost" trailing={<Icon.LongArrowRightIcon />}>
            모두보기
          </Button>
        </div>
      </div>

      {Array.from({ length: 7 }).map((_) => {
        return (
          <Entry reverse line="top" spacing="large">
            <Entry.Main>
              <Entry.Header>
                <Shell reverse>
                  <Shell.Leading>
                    <ImgBox size="small" img="" />
                  </Shell.Leading>
                  <Shell.Content
                    title="title"
                    content={
                      <>
                        <Avatar size="x-small" img={null} />
                        Content
                      </>
                    }
                  />
                </Shell>
              </Entry.Header>
              <Entry.Body>
                "기억은 우리가 과거와 맺는 관계의 이름이 아니라, 현재 우리가 누 구인지를 결정짓는
                살아있는 풍경이다. 우리는 매 순간 기억을 통 해..."
              </Entry.Body>
              <Entry.Footer>
                <Button
                  shape="link"
                  size="small"
                  variant="ghost"
                  leading={<Icon.HeartOffIcon color="secondary" />}
                >
                  20
                </Button>
                <Button
                  shape="link"
                  size="small"
                  variant="ghost"
                  leading={<Icon.CommentIcon color="secondary" />}
                >
                  20
                </Button>
              </Entry.Footer>
            </Entry.Main>
          </Entry>
        );
      })}
    </div>
  );
};
