import { useEffect, useRef, useState } from 'react';
import type { ElementType } from 'react';

import { Button } from '#internal/components/Button';
import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './DropdownMenu.module.css';

import type { Props } from './';

const classnameDefault = 'ui-DropdownMenu';

export const DropdownMenu = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, trigger, items, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const [isOpen, setIsOpen] = useState(false);

  const closeMenu = () => {
    setIsOpen(false);
  };

  const handleToggleClick = () => {
    setIsOpen((open) => !open);
  };

  const rootRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('click', handleClickOutside);
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, []);

  return (
    <View as={as} ref={rootRef} className={classname} {...restProps}>
      <div className={styles.trigger} onClick={handleToggleClick}>
        {trigger}
      </div>
      {isOpen && (
        <div className={styles.menu}>
          {items.map((item, index) => (
            <Button
              as="button"
              type="button"
              role="menuitem"
              key={index}
              className={styles.item}
              tabIndex={-1}
              onClick={() => {
                closeMenu();
                item.onClick();
              }}
            >
              {item.text}
            </Button>
          ))}
        </div>
      )}
    </View>
  );
};
