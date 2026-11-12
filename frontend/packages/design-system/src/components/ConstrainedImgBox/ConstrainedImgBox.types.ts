import type { CSSProperties, ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

type FallbackContent = Exclude<ReactNode, null | undefined | boolean>;

type ImageContent =
  | {
      img: string;
      fallback?: FallbackContent;
    }
  | {
      img?: never;
      fallback: FallbackContent;
    };

export type OwnProps = ImageContent & {
  maxWidth?: CSSProperties['maxWidth'];
  maxHeight?: CSSProperties['maxHeight'];
  width?: CSSProperties['width'];
  height?: CSSProperties['height'];
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
