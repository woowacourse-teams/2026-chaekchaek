import type { ComponentType, SVGProps } from 'react';

import { Icon } from './';
import type { Props } from './';
const ICON_SIZES = {
  small: 12,
  medium: 15,
  large: 18,
} as const;

export const createIcon = (Svg: ComponentType<SVGProps<SVGSVGElement>>) => {
  return (props: Props) => {
    const { size = 'medium' } = props;
    const pixelSize = ICON_SIZES[size as keyof typeof ICON_SIZES];

    return (
      <Icon {...props}>
        <Svg width={pixelSize} height={pixelSize} />
      </Icon>
    );
  };
};
