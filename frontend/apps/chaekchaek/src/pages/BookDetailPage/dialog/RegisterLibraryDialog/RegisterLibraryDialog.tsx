import { useState, type ChangeEvent } from 'react';

import { Button, ButtonStack, Dialog, Field, Radio } from '@chaekchaek/design-system';

import { track } from '@/analytics/track';

import { postLibrary } from '@/services/apis/library/repository';
import { useExecute } from '@/services/core/useExecute';

import type { ReadingStatus, RegisterLibraryDialogProps } from './RegisterLibraryDialog.types';

const READING_STATUS_OPTIONS: { value: ReadingStatus; text: string }[] = [
  { value: 'WANT_TO_READ', text: '읽고 싶어요' },
  { value: 'READING', text: '읽는 중' },
  { value: 'FINISHED', text: '다 읽음' },
];

export const RegisterLibraryDialog = ({
  isbn,
  onLibraryRegistered,
  onClose,
}: RegisterLibraryDialogProps) => {
  const [status, setStatus] = useState<ReadingStatus>('READING');

  const handleChangeStatus = (nextStatus: ReadingStatus) => {
    setStatus(nextStatus);
  };

  const { mutate: mutatePostLibrary } = useExecute({
    executeFn: postLibrary,
    onSuccess: onLibraryRegistered,
  });

  const handleSubmitRegisterLibrary = async () => {
    await mutatePostLibrary({ isbn13: isbn, status });

    track('library_add', {
      source: 'detail_required',
      status:
        status === 'WANT_TO_READ' ? 'want_to_read' : status === 'READING' ? 'reading' : 'finished',
    });

    onClose();
  };

  return (
    <Dialog size="medium" onClose={onClose}>
      <Dialog.Container>
        <form>
          <Dialog.Header
            subTitle={
              <>
                지금의 독서 상태를 선택해 주세요. <br />내 서재에서 언제든 바꿀 수 있어요.
              </>
            }
          >
            내 서재에 넣으시겠어요?
          </Dialog.Header>

          <Dialog.Body>
            <Field>
              <Field.Label>독서 상태</Field.Label>
              <Field.Content>
                <div>
                  {READING_STATUS_OPTIONS.map((option) => {
                    return (
                      <Radio
                        variant="accent"
                        boxed={true}
                        block
                        name="RegisterLibraryDialog"
                        value={option.value}
                        onChange={(e: ChangeEvent<HTMLInputElement>) => {
                          handleChangeStatus(e.target.value as ReadingStatus);
                        }}
                      >
                        {option.text}
                      </Radio>
                    );
                  })}
                </div>
              </Field.Content>
            </Field>
          </Dialog.Body>

          <Dialog.Footer>
            <ButtonStack direction="vertical">
              <Button
                type="submit"
                variant="primary"
                size="large"
                block
                onClick={handleSubmitRegisterLibrary}
              >
                내 서재에 넣기
              </Button>
              <Button type="button" variant="ghost" size="large" block onClick={onClose}>
                취소
              </Button>
            </ButtonStack>
          </Dialog.Footer>
        </form>
      </Dialog.Container>
    </Dialog>
  );
};
