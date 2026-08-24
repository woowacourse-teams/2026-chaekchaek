import { useState } from 'react';
import { type ChangeEvent } from 'react';

import { Button, ButtonStack, Dialog, Field, Radio } from '@chaekchaek/design-system';

import type { UpdateBookStatusDialogProps } from './UpdateBookStatusDialog.types';

import { READING_STATUS, READING_STATUS_LABELS } from '../../LibraryPage';
import type { ReadingStatus } from '../../LibraryPage';

export const UpdateBookStatusDialog = ({ onClose }: UpdateBookStatusDialogProps) => {
  const [status, setStatus] = useState<ReadingStatus>(READING_STATUS.READING);
  const handleChangeStatus = (e: ChangeEvent<HTMLInputElement>) => {
    setStatus(e.target.value as ReadingStatus);
  };

  return (
    <Dialog size="medium" onClose={onClose}>
      <Dialog.Container>
        <form>
          <Dialog.Header subTitle={`선택한 1권의 상태를 변경합니다.`}>독서 상태 변경</Dialog.Header>

          <Dialog.Body>
            <Field>
              <Field.Content>
                {Object.entries(READING_STATUS_LABELS)
                  .filter(([key]) => key !== READING_STATUS.ALL)
                  .map(([key, value]) => {
                    return (
                      <Radio
                        name="status"
                        defaultChecked={status === key}
                        value={key}
                        onChange={handleChangeStatus}
                      >
                        {value}
                      </Radio>
                    );
                  })}
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
