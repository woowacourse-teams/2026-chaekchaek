import { Button, Dialog, Field, FieldGroup, Input } from '@chaekchaek/design-system';

import type { RenameLibraryDialogProps } from './RenameLibraryDialog.types';

export const RenameLibraryDialog = ({ onClose }: RenameLibraryDialogProps) => {
  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>책장 이름 바꾸기</Dialog.Header>
        <Dialog.Body>
          <FieldGroup>
            <Field>
              <Field.Label>책장 이름</Field.Label>
              <Field.Content>
                <Input />
              </Field.Content>
            </Field>
          </FieldGroup>
        </Dialog.Body>
        <Dialog.Footer>
          <Button block variant="primary">
            저장
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
