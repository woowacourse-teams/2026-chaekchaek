import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
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
    sx,
    style,
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

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  const handleChange = (value: any) => {
    onChange?.(value);
  };

  return (
    <View as={as} className={classname} style={customStyles} {...restProps}>
      {title && <div className={styles.title}>{title}</div>}
      {options.map((option) => {
        return (
          <Item
            value={option.value}
            isActive={option.value === value}
            meta={option.meta}
            onClick={() => {
              handleChange(option.value);
            }}
          >
            {option.text}
          </Item>
        );
      })}
    </View>
  );
};

OptionList.Item = Item;
