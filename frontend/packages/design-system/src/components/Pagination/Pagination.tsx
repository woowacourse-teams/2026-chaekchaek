import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Pagination.module.css';

import { Item } from './Item';

import type { Props } from './';

const classnameDefault = 'ui-Pagination';
const MAX_VISIBLE_PAGES = 7;

type PageItem = number | 'ellipsis-left' | 'ellipsis-right';

const getPageItems = (currentPage: number, totalPages: number): PageItem[] => {
  if (totalPages <= MAX_VISIBLE_PAGES) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  if (currentPage <= 4) {
    return [1, 2, 3, 4, 5, 'ellipsis-right', totalPages];
  }

  if (currentPage >= totalPages - 3) {
    return [
      1,
      'ellipsis-left',
      totalPages - 4,
      totalPages - 3,
      totalPages - 2,
      totalPages - 1,
      totalPages,
    ];
  }

  return [
    1,
    'ellipsis-left',
    currentPage - 1,
    currentPage,
    currentPage + 1,
    'ellipsis-right',
    totalPages,
  ];
};

export const Pagination = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, defaultPage, totalPages, onChange, ...restProps } = props;
  const lastPage = Math.max(1, totalPages);
  const currentPage = Math.min(Math.max(1, defaultPage), lastPage);
  const pageItems = getPageItems(currentPage, lastPage);

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
        disabled={currentPage === 1}
        onClick={() => handleChangePage(currentPage - 1)}
        type="button"
      >
        ‹
      </Item>
      {pageItems.map((item) => {
        if (typeof item !== 'number') {
          return (
            <span aria-hidden="true" className={styles?.['ui-Pagination-Ellipsis']} key={item}>
              …
            </span>
          );
        }

        return (
          <Item
            aria-current={item === currentPage ? 'page' : undefined}
            aria-label={`Page ${item}`}
            as="button"
            isActive={item === currentPage}
            key={item}
            onClick={() => handleChangePage(item)}
            type="button"
          >
            {item}
          </Item>
        );
      })}
      <Item
        aria-label="Next page"
        as="button"
        disabled={currentPage === lastPage}
        onClick={() => handleChangePage(currentPage + 1)}
        type="button"
      >
        ›
      </Item>
    </View>
  );
};
