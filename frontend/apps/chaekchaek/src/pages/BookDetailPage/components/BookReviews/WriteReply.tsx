import { type KeyboardEvent } from 'react';

import { Avatar, Button, Field, Shell, Surface, Textarea } from '@chaekchaek/design-system';

import { useFormValues } from '@/hooks/useFormValues';

import { useExecute } from '@/services/core/useExecute';
import { postReviewsReviewIdReplies } from '@/services/apis/reviewsReviewIdReplies/repository';

import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

import { validateReply } from './validator';
import type { ReplyFormValues } from './validator';

import type { WriteReplyProps } from './WriteReply.types';
import { track } from '@/analytics/track';

export const WriteReply = ({ reviewId, onReplyWritten }: WriteReplyProps) => {
  const { user, guest } = useAuthContext();
  const { values, errors, onChange, isValid, valids } = useFormValues<ReplyFormValues>({
    initialValues: {
      content: '',
    },
    validate: validateReply,
  });

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();

      handleSubmit();
    }
  };

  const { mutate: postReplyMutate } = useExecute({
    executeFn: postReviewsReviewIdReplies,
  });

  const handleSubmit = async () => {
    await postReplyMutate(
      {
        reviewId,
        content: values.content,
      },
      guest?.guestToken
        ? {
            guestToken: guest?.guestToken,
          }
        : undefined,
    );

    track('reply_submit', { user_type: guest ? 'guest' : 'member' });

    await onReplyWritten();
  };

  return (
    <Surface>
      <Shell>
        <Shell.Leading>
          <Avatar img={user?.profileImageUrl || ''} size="small" />
        </Shell.Leading>
        <Shell.Content
          content={
            <Field>
              <Field.Content>
                <Textarea
                  id="content"
                  value={values.content}
                  onChange={onChange}
                  onKeyDown={handleKeyDown}
                  height="100px"
                  block
                />
                <Button type="button" variant="ghost" disabled={!isValid} onClick={handleSubmit}>
                  답글 남기기
                </Button>
              </Field.Content>
            </Field>
          }
        />
      </Shell>
    </Surface>
  );
};
