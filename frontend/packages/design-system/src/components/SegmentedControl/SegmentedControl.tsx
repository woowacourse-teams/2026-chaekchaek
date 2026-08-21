import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './SegmentedControl.module.css';

import { Item } from './Item';
import type { Props } from './';

const classnameDefault = 'ui-SegmentedControl';

export const SegmentedControl = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    shape = 'default',
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

  const handleChange = (value: any) => {
    onChange?.(value);
  };

  return (
    <View as={as} className={classname} {...restProps}>
      {options.map((option) => {
        return (
          <Item
            value={option.value}
            isActive={option.value === value}
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
