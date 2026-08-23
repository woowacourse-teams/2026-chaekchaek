import { Dialog } from '@chaekchaek/design-system';

import { LoginHero } from '../../components/LoginHero';
import { SocialLoginButton } from '../../components/SocialLoginButton';

import type { LoginDialogProps } from './LoginDialog.types';
import styles from './LoginDialog.module.css';

export const LoginDialog = ({ onClose }: LoginDialogProps) => {
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container className={styles.container}>
        <Dialog.Body className={styles.body}>
          <LoginHero reverse />
          <div className={styles.loginButtons}>
            <SocialLoginButton provider="google" reverse />
          </div>
        </Dialog.Body>
      </Dialog.Container>
    </Dialog>
  );
};
