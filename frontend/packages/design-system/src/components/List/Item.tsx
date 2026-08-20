import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './List.module.css';

import { Leading } from './Leading';
import { Content } from './Content';
import { Trailing } from './Trailing';

import type { Props } from './';

const classnameDefault = 'ui-List-Item';

export const Item = <T extends ElementType>(props: Props<T>) => {
  const { as = 'li', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};

Item.Leading = Leading;
Item.Content = Content;
Item.Trailing = Trailing;
