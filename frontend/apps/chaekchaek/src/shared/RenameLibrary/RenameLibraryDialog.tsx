import { Button, Dialog, Field, FieldGroup, Input } from '@chaekchaek/design-system';

import { patchLibrariesLibraryId } from '@/services/apis/librariesLibraryId/repository';
import { useExecute } from '@/services/core/useExecute';

import { useFormValues } from '@/hooks/useFormValues';
import { validateLibrary } from './validator';
import type { LibraryFormValues } from './validator';

import type { RenameLibraryDialogProps } from './RenameLibraryDialog.types';

export const RenameLibraryDialog = ({
  libraryId,
  name: defaultName,
  onLibraryRenamed,
  onClose,
}: RenameLibraryDialogProps) => {
  const { values, errors, onChange, isValid, valids } = useFormValues<LibraryFormValues>({
    initialValues: { name: defaultName },
    validate: validateLibrary,
  });

  const { mutate: patchLibrariesMutate } = useExecute({
    executeFn: patchLibrariesLibraryId,
  });

  const handleSubmit = async () => {
    await patchLibrariesMutate({
      libraryId,
      name: values.name,
    });

    await onLibraryRenamed();

    onClose();
  };
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>책장 이름 바꾸기</Dialog.Header>
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
            저장
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
