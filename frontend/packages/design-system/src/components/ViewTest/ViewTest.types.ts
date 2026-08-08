import type { ElementType } from 'react';

import type { Props as ViewProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {};

export type Props<T extends ElementType> = ViewProps<T>;
