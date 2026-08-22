import { useState } from 'react';
import type { ChangeEvent, SubmitEvent } from 'react';

import { Button, Callout, Checkbox, Dialog, Field, Input } from '@chaekchaek/design-system';

import { useExecute } from '@/services/core/useExecute';
import { postBooksBookIdReviews } from '@/services/apis/booksBookIdReviews/repository';

import type { WriteReviewDialogProps } from './WriteReviewDialog.types';

type ReviewFormValues = {
  content: string;
  isSpoiler: boolean;
  quote: string;
  currentPage: string;
  chapter: string;
};

export const WriteReviewDialog = ({ bookId, onClose }: WriteReviewDialogProps) => {
  const [formValues, setFormValues] = useState<ReviewFormValues>({
    content: '',
    isSpoiler: false,
    quote: '',
    currentPage: '',
    chapter: '',
  });

  const handleChangeFormValues = ({
    name,
    value,
  }: {
    name: keyof ReviewFormValues;
    value: ReviewFormValues[keyof ReviewFormValues];
  }) => {
    setFormValues((prev) => {
      return { ...prev, [name]: value };
    });
  };

  const { mutate } = useExecute({ executeFn: postBooksBookIdReviews });

  const handleSubmit = async (e: SubmitEvent) => {
    e.preventDefault();

    const requestData = {
      content: formValues.content,
      isSpoiler: formValues.isSpoiler,
      quote: formValues.quote,
      currentPage: Number(formValues.currentPage),
      chapter: formValues.chapter,
    };
    await mutate({
      bookId,
      ...requestData,
    });
    onClose();
  };

  return (
    <Dialog size="medium" onClose={onClose}>
      <Dialog.Container>
        <form>
          <Dialog.Header>감상 남기기</Dialog.Header>

          <Dialog.Body>
            <Field>
              <Field.Label>느낀 점</Field.Label>
              <Field.Content>
                <Input
                  as="textarea"
                  placeholder="이 책을 읽으며 든 생각을 남겨보세요."
                  value={formValues.content}
                  onChange={(e: ChangeEvent<HTMLTextAreaElement>) => {
                    handleChangeFormValues({ name: 'content', value: e.target.value });
                  }}
                />
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
                <Input
                  as="textarea"
                  placeholder="기억하고 싶은 문장을 옮겨 적어보세요."
                  value={formValues.quote}
                  onChange={(e: ChangeEvent<HTMLTextAreaElement>) => {
                    handleChangeFormValues({ name: 'quote', value: e.target.value });
                  }}
                />
              </Field.Content>
            </Field>

            <Field>
              <Field.Label>쪽수</Field.Label>
              <Field.Content>
                <Input
                  block
                  inputMode="numeric"
                  placeholder="80쪽"
                  value={formValues.currentPage}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => {
                    handleChangeFormValues({ name: 'currentPage', value: e.target.value });
                  }}
                />
              </Field.Content>
            </Field>

            <Field>
              <Field.Label>목차 / 챕터</Field.Label>
              <Field.Content>
                <Input
                  block
                  placeholder="Chapter 1"
                  onChange={(e: ChangeEvent<HTMLInputElement>) => {
                    handleChangeFormValues({ name: 'chapter', value: e.target.value });
                  }}
                />
              </Field.Content>
            </Field>

            <Callout leading={<span aria-hidden="true">i</span>}>
              <strong>익명 공개</strong>
              <span>‘골똘한 참새’로 표시돼요</span>
            </Callout>
          </Dialog.Body>

          <Dialog.Footer>
            <Button type="button" variant="primary" size="large" block onClick={handleSubmit}>
              감상 남기기
            </Button>
          </Dialog.Footer>
        </form>
      </Dialog.Container>
    </Dialog>
  );
};
