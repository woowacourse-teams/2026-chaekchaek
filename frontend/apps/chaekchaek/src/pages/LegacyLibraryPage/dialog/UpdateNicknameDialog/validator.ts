import { validateFormValuesRules } from '@/hooks/useFormValues';
import type { FormValuesRules } from '@/hooks/useFormValues';

export type NicknameFormValues = {
  nickname: string;
};

export const validateNickname = ({ nickname }: NicknameFormValues) => {
  const rules = {
    nickname: [
      { type: 'isRequired', message: '닉네임은 필수값입니다' },
      {
        type: 'rangeLength',
        message: '닉네임은 2자 이상 15자 이하로 입력해주세요',
        options: { minLength: 2, maxLength: 15 },
      },
    ],
  } satisfies FormValuesRules<NicknameFormValues>;

  return validateFormValuesRules({ nickname }, rules);
};
