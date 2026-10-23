import { Button, Dialog, Field, FieldGroup, Input } from '@chaekchaek/design-system';

import { useFormValues } from '@/hooks/useFormValues';

import { postLibraries } from '@/services/apis/libraries/repository';
import { useExecute } from '@/services/core/useExecute';

import { validateLibrary, type LibraryFormValues } from './validator';

import type { RegisterLibraryDialogProps } from './RegisterLibraryDialog.types';

export const RegisterLibraryDialog = ({
  onLibraryRegistered,
  onClose,
}: RegisterLibraryDialogProps) => {
  const { values, errors, onChange, isValid, valids } = useFormValues<LibraryFormValues>({
    initialValues: { name: '' },
    validate: validateLibrary,
  });

  const { mutate: postLibrariesMutate } = useExecute({
    executeFn: postLibraries,
  });

  const handleSubmit = async () => {
    await postLibrariesMutate({
      name: values.name,
    });

    await onLibraryRegistered();
    onClose();
  };

  return (
    <Dialog onClose={onClose} role="dialog" aria-label="새 책장 만들기">
      <Dialog.Container>
        <Dialog.Header>새 책장 만들기</Dialog.Header>
        <Dialog.Body>
          <FieldGroup>
            <Field>
              <Field.Label>책장 이름</Field.Label>
              <Field.Content>
                <Input
                  id="name"
                  placeholder="예: 다시 펼치고 싶은 책"
                  value={values.name}
                  onChange={onChange}
                />
              </Field.Content>
              {!valids.name && (
                <Field.Description>
                  {errors.name.filter((item) => !item.valid)[0]?.message}
                </Field.Description>
              )}
            </Field>
          </FieldGroup>
        </Dialog.Body>
        <Dialog.Footer>
          <Button block variant="primary" disabled={!isValid} onClick={handleSubmit}>
            만들기
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
