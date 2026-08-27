import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Field.module.css';

import { Label } from './Label';
import { Content } from './Content';
import { Description } from './Description';

import type { Props } from './';

const classnameDefault = 'ui-Field';

export const Field = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, sx, style, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return <View as={as} className={classname} style={customStyles} {...restProps} />;
};

Field.Label = Label;
Field.Content = Content;
Field.Description = Description;
