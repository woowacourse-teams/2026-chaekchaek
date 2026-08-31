import type { SubmitEvent } from 'react';

import {
  Button,
  Callout,
  Checkbox,
  Dialog,
  Field,
  FieldGroup,
  Icon,
  Input,
  Tag,
  Textarea,
} from '@chaekchaek/design-system';

import { useFormValues } from '@/hooks/useFormValues';
import { useExecute } from '@/services/core/useExecute';
import { postBooksBookIdReviews } from '@/services/apis/booksBookIdReviews/repository';
import { postBooksByIsbnIsbn13Reviews } from '@/services/apis/booksByIsbnIsbn13Reviews/repository';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { validateReview } from './validator';
import type { ReviewFormValues } from './validator';

import type { WriteReviewDialogProps } from './WriteReviewDialog.types';

const compact = <T extends object>(obj: T) =>
  Object.fromEntries(Object.entries(obj).filter(([, value]) => value !== undefined)) as Partial<T>;

const buildReviewRequest = (formValues: ReviewFormValues) => {
  return {
    content: formValues.content,
    isSpoiler: formValues.isSpoiler,
    ...compact({
      quote: formValues.quote || undefined,
      chapter: formValues.chapter || undefined,
      currentPage: formValues.currentPage ? Number(formValues.currentPage) : undefined,
    }),
  };
};

export const WriteReviewDialog = ({
  bookId,
  isbn,
  onReviewWritten,
  onClose,
}: WriteReviewDialogProps) => {
  const { isAuthenticated, guest } = useAuthContext();

  const { values, errors, onChange, isValid, valids } = useFormValues<ReviewFormValues>({
    initialValues: {
      content: '',
      isSpoiler: false,
      quote: '',
      currentPage: '',
      chapter: '',
    },
    validate: validateReview,
  });

  const { mutate: postBookReviewMutate } = useExecute({ executeFn: postBooksBookIdReviews });
  const { mutate: postBookReviewByIsbnMutate } = useExecute({
    executeFn: postBooksByIsbnIsbn13Reviews,
  });

  const handleSubmit = async (e: SubmitEvent) => {
    e.preventDefault();

    const requestData = buildReviewRequest(values);

    if (isAuthenticated) {
      await postBookReviewMutate({
        bookId,
        ...requestData,
      });
    } else {
      if (!guest) return;
      await postBookReviewByIsbnMutate(
        {
          isbn13: isbn,
          ...requestData,
        },
        {
          guestToken: guest.guestToken,
        },
      );
    }

    onReviewWritten();
    onClose();
  };

  return (
    <Dialog size="large" onClose={onClose}>
      <Dialog.Container>
        <form>
          <Dialog.Header>감상 남기기</Dialog.Header>

          <Dialog.Body>
            <Field>
              <Field.Label>
                느낀 점{' '}
                <Tag variant="primary" size="small">
                  필수
                </Tag>
              </Field.Label>
              <Field.Content>
                <Textarea
                  height={100}
                  placeholder="이 책을 읽으며 든 생각을 남겨보세요."
                  id="content"
                  value={values.content}
                  onChange={onChange}
                />
              </Field.Content>
              {!valids.content && (
                <Field.Description>{errors.content[0]?.message}</Field.Description>
              )}
            </Field>

            <Field>
              <Field.Content>
                <Checkbox id="isSpoiler" checked={values.isSpoiler} onChange={onChange}>
                  스포일러
                </Checkbox>
              </Field.Content>
            </Field>

            <Field>
              <Field.Label>인상 깊은 문구</Field.Label>
              <Field.Content>
                <Textarea
                  variant="soft"
                  height={90}
                  placeholder="기억하고 싶은 문장을 옮겨 적어보세요."
                  id="quote"
                  value={values.quote}
                  onChange={onChange}
                />
              </Field.Content>
            </Field>

            <FieldGroup sx={{ mt: 4 }}>
              <Field>
                <Field.Label>쪽수</Field.Label>
                <Field.Content>
                  <Input
                    block
                    type="tel"
                    inputMode="numeric"
                    placeholder="80"
                    id="currentPage"
                    value={values.currentPage}
                    onChange={onChange}
                  />
                </Field.Content>
                {!valids.currentPage && (
                  <Field.Description>{errors.currentPage[0]?.message}</Field.Description>
                )}
              </Field>
              <Field>
                <Field.Label>목차 / 챕터</Field.Label>
                <Field.Content>
                  <Input
                    block
                    placeholder="Chapter 1"
                    id="chapter"
                    values={values.chapter}
                    onChange={onChange}
                  />
                </Field.Content>
              </Field>
            </FieldGroup>

            <Callout sx={{ mt: 4 }} leading={<Icon.InvisibleIcon />}>
              <strong>익명 공개</strong>
              <span>‘골똘한 참새’로 표시돼요</span>
            </Callout>
          </Dialog.Body>

          <Dialog.Footer>
            <Button
              type="button"
              variant="primary"
              size="large"
              block
              disabled={!isValid}
              onClick={handleSubmit}
            >
              감상 남기기
            </Button>
          </Dialog.Footer>
        </form>
      </Dialog.Container>
    </Dialog>
  );
};
