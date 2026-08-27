import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Entry.module.css';

import type { Props } from './';

import { Main } from './Main';
import { Header } from './Header';
import { Body } from './Body';
import { Footer } from './Footer';
import { Extension } from './Extension';

const classnameDefault = 'ui-Entry';

export const Entry = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, variant = 'plain', sx, style, ...restProps } = props;

  const modifiers = {
    variant: variant && styles?.[`variant-${variant}`],
  };

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

Entry.Main = Main;
Entry.Header = Header;
Entry.Body = Body;
Entry.Footer = Footer;
Entry.Extension = Extension;
