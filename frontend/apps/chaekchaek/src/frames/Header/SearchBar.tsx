import type { ElementType } from 'react';
import { useNavigate } from 'react-router-dom';

import { View } from '@chaekchaek/design-system';
import { Input } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import styles from './Header.module.css';

import type { SearchBarProps } from './Header.types';
import { ROUTES } from '@/constants/routes';

const classnameDefault = 'frame-Header-SearchBar';

export const SearchBar = <T extends ElementType>(props: SearchBarProps<T>) => {
  const { as = 'div', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const navigation = useNavigate();

  const handleClickMoveBooks = () => {
    console.log('handleClickMoveBooks');
    navigation(ROUTES.BOOK_SEARCH);
  };

  return (
    <View as={as} className={classname} {...restProps}>
      <Input
        shape="default"
        size="medium"
        reverse
        placeholder="책, 작가, 감상 검색"
        style={{ width: '250px' }}
        onClick={handleClickMoveBooks}
      />
    </View>
  );
};
