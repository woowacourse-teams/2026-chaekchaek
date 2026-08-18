import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './ImgBox.module.css';

import type { Props } from './';

const classnameDefault = 'ui-ImgBox';

export const ImgBox = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, img, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <img src={img} />
    </View>
  );
};
