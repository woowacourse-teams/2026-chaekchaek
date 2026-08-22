import { Button, Callout, Checkbox, Dialog, Field, Input } from '@chaekchaek/design-system';

import type { WriteReviewDialogProps } from './WriteReviewDialog.types';

export const WriteReviewDialog = ({ onClose }: WriteReviewDialogProps) => {
  return (
    <Dialog size="medium" onClose={onClose}>
      <Dialog.Container>
        <Dialog.Header>감상 남기기</Dialog.Header>

        <Dialog.Body>
          <Field>
            <Field.Label>느낀 점</Field.Label>
            <Field.Content>
              <Input as="textarea" placeholder="이 책을 읽으며 든 생각을 남겨보세요." />
            </Field.Content>
          </Field>

          <Field>
            <Field.Content>
              <Checkbox>스포일러</Checkbox>
            </Field.Content>
          </Field>

          <Field>
            <Field.Label>인상 깊은 문구</Field.Label>
            <Field.Content>
              <Input as="textarea" placeholder="기억하고 싶은 문장을 옮겨 적어보세요." />
            </Field.Content>
          </Field>

          <Field>
            <Field.Label>쪽수</Field.Label>
            <Field.Content>
              <Input block inputMode="numeric" placeholder="80쪽" />
            </Field.Content>
          </Field>

          <Field>
            <Field.Label>목차 / 챕터</Field.Label>
            <Field.Content>
              <Input block placeholder="Chapter 1" />
            </Field.Content>
          </Field>

          <Callout leading={<span aria-hidden="true">i</span>}>
            <strong>익명 공개</strong>
            <span>‘골똘한 참새’로 표시돼요</span>
          </Callout>
        </Dialog.Body>

        <Dialog.Footer>
          <Button variant="primary" size="large" block>
            감상 남기기
          </Button>
        </Dialog.Footer>
      </Dialog.Container>
    </Dialog>
  );
};
