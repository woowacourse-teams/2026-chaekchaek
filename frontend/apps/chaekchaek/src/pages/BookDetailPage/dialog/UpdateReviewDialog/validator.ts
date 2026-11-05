import { validateFormValuesRules } from '@/hooks/useFormValues';
import type { FormValuesRules } from '@/hooks/useFormValues';

export type ReviewFormValues = {
  content: string;
  isSpoiler: boolean;
  quote: string;
};

export const validateReview = ({ content, isSpoiler, quote }: ReviewFormValues) => {
  const rules = {
    content: [{ type: 'isRequired', message: '감상을 입력해 주세요.' }],
    isSpoiler: [],
    quote: [],
  } satisfies FormValuesRules<ReviewFormValues>;

  return validateFormValuesRules({ content, isSpoiler, quote }, rules);
};
