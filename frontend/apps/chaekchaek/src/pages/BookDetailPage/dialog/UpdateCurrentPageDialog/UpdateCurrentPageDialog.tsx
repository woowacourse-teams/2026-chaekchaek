import { useState } from 'react';
import type { ChangeEvent } from 'react';

import { Dialog, Text } from '@chaekchaek/design-system';
import { Field } from '@chaekchaek/design-system';
import { Input } from '@chaekchaek/design-system';
import { Button } from '@chaekchaek/design-system';

import { track } from '@/analytics/track';

import { useExecute } from '@/services/core/useExecute';
import { patchLibraryBookId } from '@/services/apis/libraryBookId/repository';

import type { Props } from './UpdateCurrentPageDialog.type';

export const UpdateCurrentPageDialog = ({
  bookId,
  currentPage,
  onCurrentPageUpdated,
  onClose,
}: Props) => {
  const [formValues, setFormValues] = useState({ currentPage });

  const handleChange = ({ name, value }: { name: string; value: unknown }) => {
    setFormValues((prev) => ({ ...prev, [name]: value }));
  };

  const { mutate } = useExecute({
    executeFn: patchLibraryBookId,
  });

  const handleSubmit = async () => {
    await mutate({ bookId, currentPage: Number(formValues.currentPage) });

    track('current_page_submit');

    onCurrentPageUpdated();
    onClose();
  };

  return (
    <Dialog onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header
          subTitle={
            <>
              이 감상은 {currentPage}쪽 이후 내용을 포함해요. <br />
              내가 읽은 쪽수를 입력하면 읽은 범위까지 안전하게 볼 수 있어요.
            </>
          }
        >
          어디까지 읽으셨나요?
        </Dialog.Header>
        <Dialog.Body>
          <Field.Label>
            읽은 쪽수
            <Text as="span">
              (
              <Text as="span" color="error">
                필수
              </Text>
              )
            </Text>
          </Field.Label>
          <Field.Content>
            <Input
              block
              value={formValues.currentPage}
              onChange={(e: ChangeEvent<HTMLInputElement>) => {
                handleChange({ name: 'currentPage', value: e.target.value });
              }}
            />
          </Field.Content>
          {/* <Field.Description></Field.Description> */}
        </Dialog.Body>
        <Dialog.Footer>
          <Button variant="primary" block onClick={handleSubmit}>
            저장
          </Button>
          <Button variant="ghost" block onClick={handleSubmit}>
            취소
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
