import type { CSSProperties, ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Partition.module.css';

import { Item } from './Item';

import type { Props } from './';

const classnameDefault = 'ui-Partition';

type PartitionStyle = CSSProperties & {
  '--partition-columns'?: number;
};

export const Partition = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, direction, columns, style, ...restProps } = props;
  const hasColumns = columns !== undefined && Number.isInteger(columns) && columns > 0;

  const modifiers = {
    direction: direction && styles[`direction-${direction}`],
    columns: hasColumns && styles.columns,
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const partitionStyle: PartitionStyle = {
    ...style,
    ...(hasColumns && { '--partition-columns': columns }),
  };

  return <View as={as} className={classname} style={partitionStyle} {...restProps} />;
};

Partition.Item = Item;
