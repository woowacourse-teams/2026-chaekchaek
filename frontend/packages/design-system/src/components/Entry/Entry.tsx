import type { ElementType } from 'react';

import { View } from '#internal/components/View';
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
  const { as = 'div', className, variant = 'plain', ...restProps } = props;

  const modifiers = {
    variant: variant && styles?.[`variant-${variant}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};

Entry.Main = Main;
Entry.Header = Header;
Entry.Body = Body;
Entry.Footer = Footer;
Entry.Extension = Extension;
