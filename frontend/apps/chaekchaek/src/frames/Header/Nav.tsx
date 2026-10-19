import type { ElementType } from 'react';
import { useLocation } from 'react-router-dom';

import { View } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import { ROUTES } from '@/constants/routes';

import styles from './Header.module.css';

import type { NavProps } from './Header.types';

const navs = [
  { link: ROUTES.FEED, text: '발견' },
  { link: ROUTES.LIBRARY, text: '내 서재' },
];

const classnameDefault = 'frame-Header-Nav';

export const Nav = <T extends ElementType>(props: NavProps<T>) => {
  const { as = 'nav', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const { pathname } = useLocation();

  return (
    <View as={as} className={classname} {...restProps}>
      {navs.map((nav) => {
        return (
          <a
            href={nav.link}
            key={nav.link}
            className={pathname === nav.link ? styles['is-active'] : undefined}
            aria-current={pathname === nav.link ? 'page' : undefined}
          >
            {nav.text}
          </a>
        );
      })}
    </View>
  );
};
