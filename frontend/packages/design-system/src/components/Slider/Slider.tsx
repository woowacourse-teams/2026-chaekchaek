import type { CSSProperties, ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Slider.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Slider';

type SliderStyle = CSSProperties & {
  '--slider-progress': string;
};

export const Slider = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'input',
    className,
    value,
    defaultValue,
    min = 0,
    max = 100,
    sx,
    style,
    ...restProps
  } = props;

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    className,
  });

  const numericMin = Number(min);
  const numericMax = Number(max);
  const numericValue = Number(value ?? defaultValue ?? (numericMin + numericMax) / 2);
  const range = numericMax - numericMin;
  const progress = range > 0 ? ((numericValue - numericMin) / range) * 100 : 0;
  const clampedProgress = Math.min(100, Math.max(0, progress));
  const spacingStyle = resolveSx({ sx });
  const sliderStyle: SliderStyle = {
    ...spacingStyle,
    ...style,
    '--slider-progress': `${clampedProgress}%`,
  };

  return (
    <View
      {...restProps}
      as={as}
      className={classname}
      defaultValue={defaultValue}
      max={max}
      min={min}
      style={sliderStyle}
      type="range"
      value={value}
    />
  );
};
