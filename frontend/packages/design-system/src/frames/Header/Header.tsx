import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Header.module.css';

import { Logo } from './Logo';
import { Nav } from './Nav';
import { SearchBar } from './SearchBar';

import type { Props } from './';

const classnameDefault = 'frame-Header';

export const Header = <T extends ElementType>(props: Props<T>) => {
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
      <div className={styles.wrap}>
        <Logo />
        <Nav />
        <SearchBar />
      </div>
    </View>
  );
};
