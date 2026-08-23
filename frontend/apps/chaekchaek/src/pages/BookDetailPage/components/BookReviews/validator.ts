import { validateFormValuesRules } from '@/hooks/useFormValues';
import type { FormValuesRules } from '@/hooks/useFormValues';

export type ReplyFormValues = {
  content: string;
};

export const validateReply = ({ content }: ReplyFormValues) => {
  const rules = {
    content: [{ type: 'isRequired', message: '답글은 필수값입니다' }],
  } satisfies FormValuesRules<ReplyFormValues>;

  return validateFormValuesRules({ content }, rules);
};
