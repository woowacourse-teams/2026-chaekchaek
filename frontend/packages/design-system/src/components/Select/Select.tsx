import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Select.module.css';

import { Option } from './Option';
import type { Props } from './';

const classnameDefault = 'ui-Select';

export const Select = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    value,
    options,
    onChange,
    size = 'medium',
    placeholder,
    disabled = false,
    tabIndex,
    sx,
    style,
    ...restProps
  } = props;

  const selectedOption = options.find((option) => option.value === value);

  const modifiers = {
    size: size && styles?.[`size-${size}`],
    placeholder: !selectedOption && Boolean(placeholder) && styles?.[`is-placeholder`],
    disabled: disabled && styles?.[`is-disabled`],
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
    if (disabled) return;

    onChange?.(value);
  };

  return (
    <View
      as={as}
      aria-disabled={disabled || undefined}
      className={classname}
      style={customStyles}
      tabIndex={disabled ? -1 : (tabIndex ?? 0)}
      {...restProps}
    >
      <div className={styles.box}>{selectedOption?.text ?? placeholder}</div>
      <div className={styles.container}>
        <ul>
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
        </ul>
      </div>
    </View>
  );
};
