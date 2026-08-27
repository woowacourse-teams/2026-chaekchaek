import { cloneElement } from 'react';
import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Dialog.module.css';

import { Dim } from './Dim';
import { Container } from './Container';
import { Header } from './Header';
import { Body } from './Body';
import { Footer } from './Footer';

import type { Props } from './';

const classnameDefault = 'ui-Dialog';

export const Dialog = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    children,
    size = 'medium',
    onClose,
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    size: size && styles[`size-${size}`],
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
      <Dim />
      {children &&
        cloneElement(children, {
          onClose,
        })}
    </View>
  );
};

Dialog.Container = Container;
Dialog.Header = Header;
Dialog.Body = Body;
Dialog.Footer = Footer;
