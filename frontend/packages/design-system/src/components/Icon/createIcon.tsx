import type { ComponentType, SVGProps } from 'react';

import { Icon } from './';
import type { Props } from './';

const ICON_SIZES = {
  small: 12,
  medium: 15,
  large: 18,
} as const;

const ICON_COLORS = {
  default: '#1A1A1A',
  secondary: '#666666',
  error: '#B3261E',
  inverse: '#FFFFFF',
} as const;

export const createIcon = (Svg: ComponentType<SVGProps<SVGSVGElement>>) => {
  return (props: Props) => {
    const { size = 'medium', color: colorName = 'default' } = props;
    const pixelSize = ICON_SIZES[size as keyof typeof ICON_SIZES];
    const color = ICON_COLORS[colorName as keyof typeof ICON_COLORS];

    return (
      <Icon {...props}>
        <Svg width={pixelSize} height={pixelSize} color={color} />
      </Icon>
    );
  };
};
