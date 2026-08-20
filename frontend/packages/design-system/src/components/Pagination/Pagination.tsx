import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Pagination.module.css';

import { Item } from './Item';

import type { Props } from './';

const classnameDefault = 'ui-Pagination';

export const Pagination = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, defaultPage, totalPages, onChange, ...restProps } = props;
  const lastPage = Math.max(1, totalPages);

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const handleChangePage = (page: number) => {
    onChange?.(page);
  };

  return (
    <View aria-label="Pagination" as={as} className={classname} role="navigation" {...restProps}>
      <Item
        aria-label="Previous page"
        as="button"
        disabled={defaultPage === 1}
        onClick={() => handleChangePage(defaultPage - 1)}
        type="button"
      >
        ‹
      </Item>
      {Array.from({ length: lastPage }, (_, index) => index + 1).map((page) => (
        <Item
          aria-current={page === defaultPage ? 'page' : undefined}
          aria-label={`Page ${page}`}
          as="button"
          isActive={page === defaultPage}
          key={page}
          onClick={() => handleChangePage(page)}
          type="button"
        >
          {page}
        </Item>
      ))}
      <Item
        aria-label="Next page"
        as="button"
        disabled={defaultPage === lastPage}
        onClick={() => handleChangePage(defaultPage + 1)}
        type="button"
      >
        ›
      </Item>
    </View>
  );
};
