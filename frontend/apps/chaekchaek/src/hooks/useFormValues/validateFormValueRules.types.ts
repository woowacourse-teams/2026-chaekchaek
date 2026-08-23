import type { ReactNode } from 'react';

export type RuleType =
  | 'isRequired'
  | 'isNumericString'
  | 'isValidMonth'
  | 'length'
  | 'minLength'
  | 'maxLength'
  | 'rangeLength';

export type Validators = {
  [type in RuleType]: (value: unknown, options?: any) => boolean;
};

export type ValidatorRule = {
  type: RuleType;
  message: ReactNode;
};
export type CustomRule = {
  type: 'custom';
  message: ReactNode;
  validate: (value: unknown) => boolean;
};

export type Rule = (ValidatorRule | CustomRule) & {
  options?: unknown;
};

export type FormValuesRules<TFormValues extends Record<string, unknown>> = {
  [FormKey in keyof TFormValues]: Rule[];
};

export type ResultValid = Rule & {
  valid: boolean;
};
