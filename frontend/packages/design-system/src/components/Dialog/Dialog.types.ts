import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  size?: 'medium' | 'large';
  onClose: () => void;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type DimOwnProps = {};

export type DimProps<T extends ElementType = AS> = PolymorphicProps<T, DimOwnProps>;

export type ContainerOwnProps = {
  onClose: () => void;
};

export type ContainerProps<T extends ElementType = AS> = PolymorphicProps<T, ContainerOwnProps>;

export type CloseOwnProps = {
  onClose: () => void;
};

export type CloseProps<T extends ElementType = AS> = PolymorphicProps<T, CloseOwnProps>;

export type HeaderOwnProps = {
  subTitle?: ReactNode;
};

export type HeaderProps<T extends ElementType = AS> = PolymorphicProps<T, HeaderOwnProps>;

export type BodyOwnProps = {};

export type BodyProps<T extends ElementType = AS> = PolymorphicProps<T, BodyOwnProps>;

export type FooterOwnProps = {};

export type FooterProps<T extends ElementType = AS> = PolymorphicProps<T, FooterOwnProps>;
