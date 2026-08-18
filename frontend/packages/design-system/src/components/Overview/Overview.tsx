import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Overview.module.css';

import { Content } from './Content';
import { Media } from './Media';

import type { Props } from './';

const classnameDefault = 'ui-Overview';

export const Overview = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, children, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <div className={styles.wrap}>{children}</div>
    </View>
  );
};

Overview.Content = Content;
Overview.Media = Media;
