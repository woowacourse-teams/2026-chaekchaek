import type { ElementType } from 'react';

import { View } from '@chaekchaek/design-system';
import { Input } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import styles from './Header.module.css';

import type { SearchBarProps } from './Header.types';

const classnameDefault = 'frame-Header-SearchBar';

export const SearchBar = <T extends ElementType>(props: SearchBarProps<T>) => {
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
      <Input shape="default" size="medium" style={{ width: '250px' }} />
    </View>
  );
};
