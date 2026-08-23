import { validateFormValuesRules } from '@/hooks/useFormValues';
import type { FormValuesRules } from '@/hooks/useFormValues';

export type ReviewFormValues = {
  content: string;
  isSpoiler: boolean;
  quote: string;
  currentPage: string;
  chapter: string;
};

export const validateReview = ({
  content,
  isSpoiler,
  quote,
  currentPage,
  chapter,
}: ReviewFormValues) => {
  const rules = {
    content: [{ type: 'isRequired', message: '느낀점은 필수값입니다' }],
    isSpoiler: [],
    quote: [],
    currentPage: [{ type: 'isNumericString', message: '쪽수는 숫자여여합니다' }],
    chapter: [],
  } satisfies FormValuesRules<ReviewFormValues>;

  return validateFormValuesRules({ content, isSpoiler, quote, currentPage, chapter }, rules);
};
