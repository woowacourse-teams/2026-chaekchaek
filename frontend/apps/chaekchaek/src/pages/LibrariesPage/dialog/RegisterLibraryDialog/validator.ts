import { validateFormValuesRules } from '@/hooks/useFormValues';
import type { FormValuesRules } from '@/hooks/useFormValues';

import { isString } from '@/utils/validators';

export type LibraryFormValues = {
  name: string;
};

const LIBRARY_NAME_REGEX = /^[가-힣a-zA-Z0-9_ -]+$/;

const isValidLibraryName = (name: unknown): boolean => {
  if (!isString(name)) return false;
  return LIBRARY_NAME_REGEX.test(name);
};

export const validateLibrary = ({ name }: LibraryFormValues) => {
  const rules = {
    name: [
      { type: 'isRequired', message: '책장 이름은 반드시 입력해 주세요.' },
      {
        type: 'rangeLength',
        message: '책장 이름은 1자 이상 10자 이하로 입력해주세요',
        options: { minLength: 1, maxLength: 10 },
      },
      {
        type: 'custom',
        validate: isValidLibraryName,
        message: '한글/영문/숫자/공백/-/_만 입력해주세요',
      },
    ],
  } satisfies FormValuesRules<LibraryFormValues>;

  return validateFormValuesRules({ name }, rules);
};
