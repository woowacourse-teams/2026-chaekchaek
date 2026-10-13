import { LandingCtaLink } from './LandingCtaLink';
import { LandingHeader } from './LandingHeader';

import type { CSSProperties } from 'react';

import { landingImages } from '../imgs';
import styles from '../LandingPage.module.css';

const books = [
  ['아몬드', '손원평', '#f6a9c5', 310, 70], ['소년이 온다', '한강', '#2d6a4f', 244, 44], ['불편한 편의점', '김호연', '#f15a24', 402, 86],
  ['물고기는 존재하지 않는다', '룰루 밀러', '#232d2a', 278, 55], ['사랑의 기술', '에리히 프롬', '#e39ac0', 444, 95], ['밝은 밤', '최은영', '#c96e7d', 330, 50],
  ['작별하지 않는다', '한강', '#183d50', 260, 52], ['참을 수 없는 존재의 가벼움', '밀란 쿤데라', '#ef4d38', 460, 45], ['모순', '양귀자', '#2166a7', 388, 82],
  ['채식주의자', '한강', '#f1eee3', 315, 48], ['데미안', '헤르만 헤세', '#171d3c', 425, 62], ['긴긴밤', '루리', '#f36a1d', 325, 64],
] as const;

const spineMotions = [
  { tilt: -1.4, sway: 0.7, lift: 4, duration: 11.8, delay: -7.2, gap: 3 },
  { tilt: 1.8, sway: -0.5, lift: 5, duration: 13.4, delay: -10.1, gap: 1 },
  { tilt: -0.4, sway: 0.9, lift: 3, duration: 12.9, delay: -3.8, gap: 4 },
  { tilt: 2.2, sway: -0.8, lift: 5, duration: 15.2, delay: -11.4, gap: 2 },
  { tilt: -1.1, sway: 0.6, lift: 4, duration: 13.6, delay: -9.7, gap: 5 },
  { tilt: 0.7, sway: -0.7, lift: 5, duration: 14.1, delay: -5.1, gap: 1 },
] as const;

type SpineMotionStyle = CSSProperties & {
  '--spine-tilt': string;
  '--spine-sway': string;
  '--spine-sway-mid': string;
  '--spine-lift': string;
  '--spine-lift-mid': string;
  '--spine-duration': string;
  '--spine-delay': string;
  '--spine-gap': string;
};

const Spines = ({ start, end, right = false }: { start: number; end: number; right?: boolean }) => (
  <div className={`${styles.spines} ${right ? styles.rightSpines : ''}`}>
    <div className={styles.spineTrack}>
      {books.slice(start, end).map(([title, author, color, height, width], index) => {
        const motion = spineMotions[(index + (right ? 3 : 0)) % spineMotions.length]!;
        const style: SpineMotionStyle = {
          '--spine-tilt': `${right ? -motion.tilt : motion.tilt}deg`,
          '--spine-sway': `${motion.sway}deg`,
          '--spine-sway-mid': `${motion.sway * 0.45}deg`,
          '--spine-lift': `${motion.lift * -1}px`,
          '--spine-lift-mid': `${motion.lift * -0.35}px`,
          '--spine-duration': `${motion.duration}s`,
          '--spine-delay': `${motion.delay}s`,
          '--spine-gap': `${motion.gap}px`,
          background: color,
          height,
          width,
        };

        return <div className={styles.spine} style={style} key={title}><small>{String(index + start + 1).padStart(2, '0')}</small><span>{title}</span><small>{author}</small></div>;
      })}
    </div>
  </div>
);

export const HeroSection = () => <section className={styles.hero}>
  <LandingHeader />
  <div className={styles.heroCopy}><p className={styles.eyebrow}>READ · RECORD · DISCOVER</p><h1>같은 책, 다른 시선</h1><p>같은 책을 읽어도, 우리는 서로 다른 생각을 합니다.<br />책췍에서 다른 사람의 생각을 발견하고, 나의 감상을 나눠보세요.</p><LandingCtaLink className={styles.outlineButton} to="/login">책췍 시작하기　→</LandingCtaLink></div>
  <div className={styles.heroBooks}><Spines start={0} end={6} /><div className={styles.selectedBook}><span>SELECTED　12</span><img src={landingImages.stranger} alt="알베르 카뮈의 이방인" /></div><LandingCtaLink className={styles.heroNote} to="/books/9791190818490"><b>독서 감상 04</b><p>“타인의 시선이 만든 현실에서<br />한 번 벗어나 세상을 바라보기.”</p><small>다른 독자의 감상 이어보기 ↗</small></LandingCtaLink><Spines start={6} end={12} right /></div>
</section>;
