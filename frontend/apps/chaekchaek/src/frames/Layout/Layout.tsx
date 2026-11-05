import { useEffect, useState, type ElementType } from 'react';

import { View } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import { ENV } from '@/configs/env';

import styles from './Layout.module.css';

import type { Props } from '.';

const classnameDefault = 'frame-Layout';

const envIndicators = {
  production: '',
  development: 'env-development',
  local: 'env-local',
};

export const Layout = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, children, floating, ...restProps } = props;

  const modifiers = {
    env: styles[envIndicators[ENV.APP_ENV]],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const [showFloating, setShowFloating] = useState(false);
  useEffect(() => {
    const handleScroll = () => {
      const scrollTop = document.documentElement.scrollTop;

      if (scrollTop > 0) return setShowFloating(true);
      else setShowFloating(false);
    };
    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [floating]);

  return (
    <View as={as} className={classname} {...restProps}>
      {children}
      {floating && (
        <div className={`${styles.floating} ${showFloating ? styles['is-active'] : ''}`}>
          {floating}
        </div>
      )}
    </View>
  );
};
