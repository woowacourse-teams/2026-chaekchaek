import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Split.module.css';

import type { Props } from './';

import { Top } from './Top';
import { Side } from './Side';
import { Content } from './Content';

const classnameDefault = 'ui-Split';

export const Split = <T extends ElementType>(props: Props<T>) => {
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

Split.Top = Top;
Split.Content = Content;
Split.Side = Side;
