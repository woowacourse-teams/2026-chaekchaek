import {
  isRequired,
  isNumericString,
  isValidMonth,
  length,
  minLength,
  maxLength,
  rangeLength,
} from '../../utils/validators';

import type {
  RuleType,
  Validators,
  Rule,
  FormValuesRules,
  ResultValid,
} from './validateFormValueRules.types';

const validators: Validators = {
  isRequired,
  isNumericString,
  isValidMonth,
  length,
  minLength,
  maxLength,
  rangeLength,
};

const validateFormValueRules = <T>(value: T, rules: Rule[]) => {
  return rules.map((rule) => {
    if (rule.type === 'custom') return { ...rule, valid: rule.validate(value) };
    const validator = validators[rule.type as RuleType];
    return { ...rule, valid: validator(value, rule.options || {}) };
  });
};

export const validateFormValuesRules = <TFormValues extends Record<string, unknown>>(
  formValues: TFormValues,
  formValuesRules: FormValuesRules<TFormValues>,
) => {
  return Object.entries(formValues).reduce((acc, [key, value]: [string, unknown]) => {
    const rules = formValuesRules[key];
    if (!rules) return acc;
    const valid = validateFormValueRules(value, rules);
    return { ...acc, [key]: valid };
  }, {}) as {
    [key in keyof TFormValues]: ResultValid[];
  };
};
