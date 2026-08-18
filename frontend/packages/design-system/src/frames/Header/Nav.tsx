import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Header.module.css';

import type { NavProps } from './Header.types';

const navs = [
  { link: '/', text: '발견' },
  { link: '', text: '내 서재' },
  { link: '/', text: '기록' },
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

  return (
    <View as={as} className={classname} {...restProps}>
      {navs.map((nav) => {
        return (
          <a
            href={nav.link}
            // className={styles[`is-active`]}
          >
            {nav.text}
          </a>
        );
      })}
    </View>
  );
};
