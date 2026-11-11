import { useEffect, useState, type ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';
import { resolveSx } from '#internal/systems/index';

import styles from './ImageGradientBackground.module.css';

import type { Props } from './';

import { extractBookColors, getTextColor } from './logic';
import type { RGB } from './logic';

const classnameDefault = 'ui-ImageGradientBackground';

const toRGB = ({ r, g, b }: RGB) => `rgb(${r}, ${g}, ${b})`;

const fallbackColors = {
  background: 'linear-gradient(to bottom, #ccc, #888)',
  color: getTextColor({ r: 204, g: 204, b: 204 }, { r: 136, g: 136, b: 136 }),
};

export const ImageGradientBackground = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    style,
    sx,

    width,
    height,
    maxWidth,
    maxHeight,

    img,
    ...restProps
  } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const [colors, setColors] = useState(fallbackColors);

  useEffect(() => {
    let cancelled = false;

    const updateGradient = async () => {
      try {
        const { top, bottom } = await extractBookColors(img);

        if (cancelled) return;

        setColors({
          background: `linear-gradient(to bottom, ${toRGB(top)}, ${toRGB(bottom)})`,
          color: getTextColor(top, bottom),
        });
      } catch {
        if (!cancelled) {
          setColors(fallbackColors);
        }
      }
    };

    void updateGradient();

    return () => {
      cancelled = true;
    };
  }, [img]);

  const customStyles = {
    ...spacingStyle,
    ...style,
    width,
    height,
    maxWidth,
    maxHeight,
    ...colors,
  };

  return <View as={as} className={classname} {...restProps} style={customStyles} />;
};
