import { Button, Dialog, Field, FieldGroup, Input } from '@chaekchaek/design-system';

import type { RegisterLibraryDialogProps } from './RegisterLibraryDialog.types';

export const RegisterLibraryDialog = ({ onClose }: RegisterLibraryDialogProps) => {
  return (
    <Dialog onClose={onClose} role="dialog" aria-label="새 책장 만들기">
      <Dialog.Container>
        <Dialog.Header>새 책장 만들기</Dialog.Header>
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
            만들기
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
