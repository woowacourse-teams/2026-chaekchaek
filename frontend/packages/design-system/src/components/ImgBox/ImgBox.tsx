import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './ImgBox.module.css';

import type { Props } from './';

const classnameDefault = 'ui-ImgBox';

export const ImgBox = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, img, size, sx, style, ...restProps } = props;

  const modifiers = {
    size: styles[`size-${size}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return (
    <View as={as} className={classname} style={customStyles} {...restProps}>
      <img src={img} />
    </View>
  );
};
