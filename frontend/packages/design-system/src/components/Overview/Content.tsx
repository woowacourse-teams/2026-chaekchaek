import { useEffect, useRef, useState } from 'react';
import type { ElementType } from 'react';
import clsx from 'clsx';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Overview.module.css';

import type { ContentProps } from './Overview.types';
import { Button } from '../Button';
import { Icon } from '#internal/index';

const classnameDefault = 'ui-Overview-Content';

export const Content = <T extends ElementType>(props: ContentProps<T>) => {
  const { as = 'div', className, title, content, description, meta, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const descriptionRef = useRef<HTMLDivElement>(null);

  const [isExpanded, setIsExpanded] = useState(false);
  const [isOverflowing, setIsOverflowing] = useState(false);

  const handleToggleIsExpanded = () => {
    setIsExpanded((prev) => !prev);
  };

  useEffect(() => {
    const descriptionElement = descriptionRef.current;

    if (!descriptionElement) return;

    setIsOverflowing(descriptionElement.scrollHeight > descriptionElement.clientHeight);
  }, [description]);

  return (
    <View as={as} className={classname} {...restProps}>
      {title && <div className={styles.title}>{title}</div>}
      {content && <div className={styles.content}>{content}</div>}
      {description && (
        <>
          <div
            ref={descriptionRef}
            className={clsx(
              styles.description,
              isOverflowing && styles[`is-overflowing`],
              isOverflowing && isExpanded && styles[`is-expanded`],
            )}
          >
            {description}
          </div>
          {isOverflowing && (
            <Button
              block
              className={styles[`button-expand`]}
              leading={!isExpanded ? <Icon.ArrowDownIcon /> : <Icon.ArrowUpIcon />}
              onClick={handleToggleIsExpanded}
            >
              더 보기
            </Button>
          )}
        </>
      )}

      {meta && <div className={styles.meta}>{meta}</div>}
    </View>
  );
};
