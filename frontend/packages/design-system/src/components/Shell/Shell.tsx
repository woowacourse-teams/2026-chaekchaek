import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Shell.module.css';

import type { Props } from './';

import { Leading } from './Leading';
import { Content } from './Content';
import { Trailing } from './Trailing';

const classnameDefault = 'ui-Shell';

export const Shell = <T extends ElementType>(props: Props<T>) => {
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

Shell.Leading = Leading;
Shell.Content = Content;
Shell.Trailing = Trailing;
