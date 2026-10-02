import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './ConstrainedImgBox.module.css';

import type { Props } from '.';

const classnameDefault = 'ui-ConstrainedImgBox';

export const ConstrainedImgBox = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    style,
    img,
    width,
    height,
    maxWidth,
    maxHeight,
    ...restProps
  } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const customStyle = { ...style, width, height, maxWidth, maxHeight };

  return (
    <View as={as} className={classname} {...restProps} style={customStyle}>
      <img src={img} alt="" style={{ width, height, maxWidth, maxHeight }} />
    </View>
  );
};
