import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Select.module.css';

import { Option } from './Option';
import type { Props } from './';

const classnameDefault = 'ui-Select';

export const Select = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, value, options, onChange, size, ...restProps } = props;

  const modifiers = {
    size: size && styles?.[`size-${size}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const selectedOption = options.find((option) => option.value === value);

  return (
    <View as={as} className={classname} {...restProps}>
      <div className={styles.box}>{selectedOption?.text}</div>
      <div className={styles.container}>
        <ul>
          {options.map((option) => {
            return (
              <Option
                isActive={option.value === value}
                onClick={() => {
                  onChange?.(option.value);
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
