import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './OptionList.module.css';

import { Item } from './Item';

import type { Props } from './';

const classnameDefault = 'ui-OptionList';

export const OptionList = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    shape = 'default',
    title,
    value,
    options,
    onChange,
    className,
    ...restProps
  } = props;

  const modifiers = {
    shape: shape && styles?.[`shape-${shape}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {title && <div className={styles.title}>{title}</div>}
      {options.map((option) => {
        return (
          <Item value={option.value} isActive={option.value === value} meta={option.meta}>
            {option.text}
          </Item>
        );
      })}
    </View>
  );
};

OptionList.Item = Item;
