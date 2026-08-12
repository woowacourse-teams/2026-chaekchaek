import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Header.module.css';

import LogoImg from './imgs/logo-img.png';

import type { LogoProps } from './Header.types';

const classnameDefault = 'frame-Header-Logo';

export const Logo = <T extends ElementType>(props: LogoProps<T>) => {
  const { as = 'h1', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <a href="/">
        <span className={styles.img}>
          <img src={LogoImg} alt="책책" />
        </span>
        <span className={styles.text}>책책</span>
      </a>
    </View>
  );
};
