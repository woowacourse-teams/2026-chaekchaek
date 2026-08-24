import { Button, ButtonStack, Dialog, Field, Radio } from '@chaekchaek/design-system';

import type { UpdateBookStatusDialogProps } from './UpdateBookStatusDialog.types';

export const UpdateBookStatusDialog = ({ onClose }: UpdateBookStatusDialogProps) => {
  return (
    <Dialog size="medium" onClose={onClose}>
      <Dialog.Container>
        <form>
          <Dialog.Header subTitle={`선택한 1권의 상태를 변경합니다.`}>독서 상태 변경</Dialog.Header>

          <Dialog.Body>
            <Field>
              <Field.Content>
                <Radio>읽고 싶어요</Radio>
                <Radio>읽는 중</Radio>
                <Radio>다 읽음</Radio>
              </Field.Content>
            </Field>
          </Dialog.Body>

          <Dialog.Footer>
            <ButtonStack>
              <Button type="button" variant="ghost" size="large" block onClick={onClose}>
                취소
              </Button>
              <Button type="button" variant="primary" size="large" block>
                변경
              </Button>
            </ButtonStack>
          </Dialog.Footer>
        </form>
      </Dialog.Container>
    </Dialog>
  );
};
