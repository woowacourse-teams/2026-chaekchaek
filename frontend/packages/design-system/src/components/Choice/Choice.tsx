import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Choice.module.css';

import { Option } from './Option';

import type { Props } from './';

const classnameDefault = 'ui-Choice';

export const Choice = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    value,
    options,
    onChange,
    distribution = 'equal',
    disabled = false,
    className,
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    distribution: styles?.[`distribution-${distribution}`],
  };

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const handleChange = (value: any) => {
    if (disabled) return;

    onChange?.(value);
  };

  return (
    <View as={as} className={classname} style={customStyles} {...restProps}>
      {options.map((option) => {
        return (
          <Option
            isActive={option.value === value}
            onClick={() => {
              handleChange(option.value);
            }}
          >
            {option.text}
          </Option>
        );
      })}
    </View>
  );
};

Choice.Option = Option;
