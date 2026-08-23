import { useState, useRef } from 'react';
import type { ChangeEvent, FocusEvent } from 'react';

import type { Options, FormTouched } from './useFormValues.types';

export const useFormValues = <TFormValues extends Record<string, unknown>>({
  initialValues,
  validate,
}: Options<TFormValues>) => {
  const [formValues, setFormValues] = useState<TFormValues>(initialValues);

  const handleChange = (
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
  ) => {
    setFormValues((prev) => ({ ...prev, [e.target.id]: e.target.value }));
  };

  const [blur, setBlur] = useState(() =>
    Object.keys(initialValues).reduce((acc, key) => {
      return { ...acc, [key]: false };
    }, {} as FormTouched<TFormValues>),
  );

  const handleBlur = (
    e: FocusEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
  ) => {
    setBlur((prev) => ({ ...prev, [e.target.id]: true }));
  };

  const refs = useRef<Record<string, HTMLElement>>({});
  const ref = (refElement: HTMLElement) => {
    if (!refElement) return;
    if (!refElement.id) return;
    refs.current[refElement.id] = refElement;
  };

  const errors = validate(formValues);
  const valids = Object.entries(errors).reduce(
    (acc, [key, errorValue]) => {
      acc[key] = errorValue.every((error) => error.valid);
      return acc;
    },
    {} as Record<string, boolean>,
  );
  const isValid = Object.values(errors).every((errorsValue) =>
    errorsValue.every((error) => error.valid),
  );

  const reset = () => {
    setFormValues({ ...initialValues });
    setBlur(
      Object.keys(initialValues).reduce((acc, key) => {
        return { ...acc, [key]: false };
      }, {} as FormTouched<TFormValues>),
    );
  };

  return {
    values: formValues,
    onChange: handleChange,
    blur,
    onBlur: handleBlur,
    refs,
    ref,
    errors,
    valids,
    isValid,
    reset,
  };
};
