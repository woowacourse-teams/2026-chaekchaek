import googleIcon from '@/pages/LoginPage/components/SocialLoginButton/assets/google.svg';

import { landingImages } from '../../imgs';
import typography from './StoryTypography.module.css';
import { technologySlides } from './technologySlides';
import styles from './AuthStory.module.css';

export const AuthStory = () => {
  const auth = technologySlides[2];
  const [opening, closing] = auth.title.split(', ');

  return (
    <div className={styles.story}>
      <div className={`${styles.copy} ${typography.copy}`}>
        <p className={styles.eyebrow}>03 / AUTH</p>
        <h3>
          <span>{opening},</span> <span>{closing}</span>
        </h3>
        <p className={typography.problem}>{auth.problem}</p>
        <div className={typography.solution}>
          <h4>{auth.solution}</h4>
          <ul>
            {auth.details.map((detail) => (
              <li key={detail}>{detail}</li>
            ))}
          </ul>
        </div>
      </div>
      <div
        className={styles.diagram}
        role="group"
        aria-label="로그인 사용자와 게스트 사용자의 인증 방식 비교"
      >
        <div className={styles.cards}>
          <section className={styles.card} aria-labelledby="auth-member-heading">
            <h4 id="auth-member-heading">로그인 사용자</h4>
            <ol className={styles.steps}>
              <li>
                <div className={styles.provider}>
                  <img src={googleIcon} width="18" height="18" alt="" />
                  <span>Google OAuth</span>
                </div>
              </li>
              <li>
                <p>인증 정보로 API 요청</p>
              </li>
            </ol>
          </section>
          <section className={styles.card} aria-labelledby="auth-guest-heading">
            <h4 id="auth-guest-heading">게스트 사용자</h4>
            <ol className={styles.steps}>
              <li>
                <div className={styles.token}>게스트 토큰</div>
              </li>
              <li>
                <p>게스트 인증 정보로 API 요청</p>
              </li>
            </ol>
          </section>
        </div>
        <p className={styles.diagramNote}>이용 가능한 기능과 UI는 인증 상태에 따라 달라집니다.</p>
      </div>
      <div className={styles.message}>
        <p>{auth.outcome}</p>
        <img
          src={landingImages.chaekchaekCharacter}
          width="110"
          height="110"
          alt=""
          loading="lazy"
        />
      </div>
    </div>
  );
};
