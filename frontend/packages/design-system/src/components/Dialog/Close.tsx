import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Dialog.module.css';

import DialogCloseIcon from './imgs/dialog-close.svg';

import type { CloseProps } from './Dialog.types';

const classnameDefault = 'ui-Dialog-Close';

export const Close = <T extends ElementType>(props: CloseProps<T>) => {
  const { as = 'div', className, onClose, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps} onClick={onClose}>
      <img src={DialogCloseIcon} alt="close" />
    </View>
  );
};
