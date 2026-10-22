import { useState, type ChangeEvent, type SubmitEvent } from 'react';

import { Button, Dialog, Field, Radio, Rating, Text, Textarea } from '@chaekchaek/design-system';

import { track } from '@/analytics/track';

import { useFormValues } from '@/hooks/useFormValues';
import { useExecute } from '@/services/core/useExecute';
import { postBooksBookIdReviews } from '@/services/apis/booksBookIdReviews/repository';
import { postBooksByIsbnIsbn13Reviews } from '@/services/apis/booksByIsbnIsbn13Reviews/repository';
import { putLibraryBookIdRating } from '@/services/apis/libraryBookIdRating/repository';

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
    }),
  };
};

const RatingMessages = {
  0.5: '아쉬웠어요',
  1.0: '별로였어요',
  1.5: '조금 아쉬웠어요',
  2.0: '아쉬운 점이 많았어요',
  2.5: '그저 그랬어요',
  3.0: '괜찮았어요',
  3.5: '꽤 좋았어요',
  4.0: '좋았어요',
  4.5: '정말 좋았어요',
  5.0: '최고였어요',
};

const getRatingMessageNumber = (rating: number) => {
  return Math.round(rating * 2) / 2;
};

export const WriteReviewDialog = ({
  bookId,
  isbn,
  myRating: defaultRating,
  onReviewWritten,
  onClose,
}: WriteReviewDialogProps) => {
  const { isAuthenticated, guest, user } = useAuthContext();

  const { values, errors, onChange, isValid, valids } = useFormValues<ReviewFormValues>({
    initialValues: {
      content: '',
      isSpoiler: false,
      quote: '',
    },
    validate: validateReview,
  });

  const { mutate: postBookReviewMutate } = useExecute({ executeFn: postBooksBookIdReviews });
  const { mutate: postBookReviewByIsbnMutate } = useExecute({
    executeFn: postBooksByIsbnIsbn13Reviews,
  });

  const handleSpoilerClick = () => {
    onChange({
      target: {
        id: 'isSpoiler',
        value: !values.isSpoiler,
      },
    } as unknown as ChangeEvent<HTMLInputElement>);
  };

  const handleSubmit = async (e: SubmitEvent) => {
    e.preventDefault();

    await Promise.all([await writeReview(), await updateRating()]);

    onReviewWritten();
    onClose();
  };

  const writeReview = async () => {
    const requestData = buildReviewRequest(values);

    if (isAuthenticated) {
      await postBookReviewMutate({
        bookId,
        ...requestData,
      });

      if (user) {
        track('review_submit', {
          actor_type: user.actorType,
          actor_id: user.actorId,
          isbn,
        });
      }
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

      track('review_submit', {
        actor_type: guest.actorType,
        actor_id: guest.actorId,
        isbn,
      });
    }
  };

  const [rating, setRating] = useState(defaultRating || 0);
  const handleChangeRating = (rating: number) => {
    setRating(rating);
  };

  const ratingMessageNumber = getRatingMessageNumber(rating);
  const ratingMessage = RatingMessages[ratingMessageNumber as keyof typeof RatingMessages] || '-';

  const { mutate } = useExecute({
    executeFn: putLibraryBookIdRating,
  });

  const updateRating = async () => {
    if (rating === defaultRating) return;

    await mutate({ bookId, rating });

    track('rating_submit');
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
                <Text as="span">
                  (
                  <Text as="span" color="error">
                    필수
                  </Text>
                  )
                </Text>
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
                <Radio
                  id="isSpoiler"
                  name="isSpoiler"
                  variant="accent"
                  boxed={true}
                  checked={values.isSpoiler}
                  readOnly
                  onClick={handleSpoilerClick}
                >
                  스포일러
                </Radio>
              </Field.Content>
            </Field>

            <Field>
              <Field.Label>
                인상 깊은 문구
                <Text as="span">
                  (
                  <Text as="span" color="muted">
                    선택
                  </Text>
                  )
                </Text>
              </Field.Label>
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

            <Text color="muted" sx={{ mt: 4 }}>
              {((isAuthenticated && user?.displayAnonymous) || !isAuthenticated) && (
                <strong>익명 공개</strong>
              )}
              <span>
                ‘
                {isAuthenticated
                  ? !user?.displayAnonymous
                    ? user?.nickname
                    : user?.anonymousNickname
                  : guest?.nickname}
                ’(으)로 표시돼요
              </span>
            </Text>

            <Field sx={{ mt: 4 }}>
              <Field.Label>별점 (선택)</Field.Label>
              <Field.Content>
                <Rating
                  value={rating}
                  onChange={handleChangeRating}
                  description={`${rating} · ${ratingMessage}`}
                  block
                />
              </Field.Content>
            </Field>
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
