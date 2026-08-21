import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  variant?: 'plain' | 'subtle';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type MainOwnProps = {};

export type MainProps<T extends ElementType = AS> = PolymorphicProps<T, MainOwnProps>;

export type HeaderOwnProps = {};

export type HeaderProps<T extends ElementType = AS> = PolymorphicProps<T, HeaderOwnProps>;

export type BodyOwnProps = {};

export type BodyProps<T extends ElementType = AS> = PolymorphicProps<T, BodyOwnProps>;

export type FooterOwnProps = {};

export type FooterProps<T extends ElementType = AS> = PolymorphicProps<T, FooterOwnProps>;

export type ExtensionOwnProps = {};

export type ExtensionProps<T extends ElementType = AS> = PolymorphicProps<T, ExtensionOwnProps>;
