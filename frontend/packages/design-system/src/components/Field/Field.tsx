import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Field.module.css';

import { Label } from './Label';
import { Content } from './Content';
import { Description } from './Description';

import type { Props } from './';

const classnameDefault = 'ui-Field';

export const Field = <T extends ElementType>(props: Props<T>) => {
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

Field.Label = Label;
Field.Content = Content;
Field.Description = Description;
