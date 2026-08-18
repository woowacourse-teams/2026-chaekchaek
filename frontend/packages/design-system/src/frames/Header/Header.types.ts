import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type LogoOwnProps = {};

export type LogoProps<T extends ElementType = AS> = PolymorphicProps<T, LogoOwnProps>;

export type NavOwnProps = {};

export type NavProps<T extends ElementType = AS> = PolymorphicProps<T, NavOwnProps>;

export type SearchBarOwnProps = {};

export type SearchBarProps<T extends ElementType = AS> = PolymorphicProps<T, SearchBarOwnProps>;
