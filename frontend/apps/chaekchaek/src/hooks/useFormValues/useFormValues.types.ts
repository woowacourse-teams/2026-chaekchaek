import type { ResultValid } from './validateFormValueRules.types';

export interface Options<TFormValues> {
  initialValues: TFormValues;
  validate: (formValues: TFormValues) => { [formKey in keyof TFormValues]: ResultValid[] };
}

export type FormTouched<TFormValues> = {
  [formKey in keyof TFormValues]: boolean;
};
