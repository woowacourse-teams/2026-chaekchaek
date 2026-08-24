import type {ReactNode} from 'react';
import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';

import styles from './index.module.css';

const principles = [
  {
    title: '문제를 기록합니다',
    description: '제품을 만들며 마주친 선택과 맥락을 팀의 언어로 남깁니다.',
  },
  {
    title: '배움을 나눕니다',
    description: '혼자 알기에는 아까운 시행착오와 새로운 발견을 함께 풀어냅니다.',
  },
  {
    title: '다음 선택을 돕습니다',
    description: '오늘의 기록이 내일 더 나은 결정을 위한 출발점이 되길 바랍니다.',
  },
];

export default function Home(): ReactNode {
  return (
    <Layout
      title="함께 읽고, 만들고, 기록합니다"
      description="책책 팀의 기술적 고민과 배움을 기록하는 공간입니다.">
      <header className={styles.hero}>
        <div className="container">
          <p className={styles.eyebrow}>CHAEKCHAEK ENGINEERING</p>
          <Heading as="h1" className={styles.title}>
            함께 읽고, 만들고,
            <br />
            기록합니다.
          </Heading>
          <p className={styles.lead}>
            책책이 제품을 만드는 과정에서 마주친 기술적 고민과 배움을
            차곡차곡 쌓아갑니다.
          </p>
          <Link className={styles.cta} to="/blog">
            글 모아보기 <span aria-hidden="true">→</span>
          </Link>
        </div>
      </header>
      <main>
        <section className={styles.principles}>
          <div className="container">
            <p className={styles.sectionLabel}>OUR NOTES</p>
            <div className={styles.cards}>
              {principles.map((principle, index) => (
                <article className={styles.card} key={principle.title}>
                  <span className={styles.cardNumber}>
                    0{index + 1}
                  </span>
                  <Heading as="h2">{principle.title}</Heading>
                  <p>{principle.description}</p>
                </article>
              ))}
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}
