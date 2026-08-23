import type { ElementType } from 'react';

import { View } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import styles from './Header.module.css';

import { Logo } from './Logo';
import { Nav } from './Nav';
import { ProfileButton } from './ProfileButton';
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
        <div className={styles.actions}>
          <SearchBar />
          <ProfileButton />
        </div>
      </div>
    </View>
  );
};
