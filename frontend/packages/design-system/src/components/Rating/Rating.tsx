import type { ElementType } from 'react';

import clsx from 'clsx';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Rating.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Rating';

export const Rating = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    value,
    onChange,
    size = 'medium',
    title,
    description,
    block,
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    size: styles[`size-${size}`],
    block: block && styles[`is-block`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  const handleChange = (rating: number) => {
    onChange?.(rating);
  };

  return (
    <View as={as} className={classname} style={customStyles} {...restProps}>
      {title && <div className={styles.title}>{title}</div>}
      <div className={styles.stars}>
        {Array.from({ length: 5 }).map((_, index) => {
          return (
            <div
              key={index}
              className={clsx(
                styles.star,
                index < value ? styles['star-active'] : styles['star-inactive'],
              )}
              onClick={() => {
                handleChange(index + 1);
              }}
            ></div>
          );
        })}
      </div>
      {description && <div className={styles.description}>{description}</div>}
    </View>
  );
};
