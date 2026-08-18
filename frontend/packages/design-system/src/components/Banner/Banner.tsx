import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Banner.module.css';

import { Leading } from './Leading';
import { Content } from './Content';
import { Trailing } from './Trailing';

import type { Props } from './';

const classnameDefault = 'ui-Banner';

export const Banner = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};

Banner.Leading = Leading;
Banner.Content = Content;
Banner.Trailing = Trailing;
