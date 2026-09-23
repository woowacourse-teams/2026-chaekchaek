import type { ElementType } from 'react';

import { View } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import { ENV } from '@/configs/env';

import styles from './Layout.module.css';

import type { Props } from '.';

const classnameDefault = 'frame-Layout';

const envIndicators = {
  production: '',
  development: 'env-development',
  local: 'env-local',
};

export const Layout = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, ...restProps } = props;

  const modifiers = {
    env: styles[envIndicators[ENV.APP_ENV]],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
