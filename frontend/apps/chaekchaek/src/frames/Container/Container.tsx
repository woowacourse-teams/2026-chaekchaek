import { View } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import styles from './Container.module.css';

import type { Props } from './';

const classnameDefault = 'frame-Container';

export const Container = (props: Props) => {
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
      {children}
    </View>
  );
};
