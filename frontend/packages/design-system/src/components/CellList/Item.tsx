import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import { Rating } from '#internal/components/Rating';

import styles from './CellList.module.css';

import type { ItemProps } from './CellList.types';

const classnameDefault = 'ui-CellList-Item';

export const Item = <T extends ElementType>(props: ItemProps<T>) => {
  const { as = 'div', className, headline, title, content, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {headline && (
        <span className={styles.headline}>
          <Rating.Start as="span" active className={styles.ratingStar} />
          {headline}
        </span>
      )}
      {title && <span className={styles.title}>{title}</span>}
      {content && <span className={styles.content}>{content}</span>}
    </View>
  );
};
