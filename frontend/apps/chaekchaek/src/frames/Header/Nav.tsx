import type { ElementType } from 'react';

import { View } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import { ROUTES } from '@/constants/routes';

import styles from './Header.module.css';

import type { NavProps } from './Header.types';
import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

const navs = [
  { link: ROUTES.HOME, text: '발견' },
  { link: ROUTES.LIBRARY, text: '내 서재' },
  { link: ROUTES.HOME, text: '기록' },
];

const classnameDefault = 'frame-Header-Nav';

export const Nav = <T extends ElementType>(props: NavProps<T>) => {
  const { as = 'div', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const { isAuthenticated } = useAuthContext();

  return (
    <View as={as} className={classname} {...restProps}>
      {navs.map((nav) => {
        return (
          <a
            href={isAuthenticated ? nav.link : ROUTES.LOGIN}
            // className={styles[`is-active`]}
          >
            {nav.text}
          </a>
        );
      })}
    </View>
  );
};
