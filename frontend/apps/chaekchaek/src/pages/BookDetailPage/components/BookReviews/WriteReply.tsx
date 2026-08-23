import { Avatar, Button, Field, Input, Shell, Surface } from '@chaekchaek/design-system';

import { useFormValues } from '@/hooks/useFormValues';

import { useExecute } from '@/services/core/useExecute';
import { postReviewsReviewIdReplies } from '@/services/apis/reviewsReviewIdReplies/repository';

import { validateReply } from './validator';
import type { ReplyFormValues } from './validator';

import type { WriteReplyProps } from './WriteReply.types';

export const WriteReply = ({ reviewId }: WriteReplyProps) => {
  const { values, errors, onChange, isValid, valids } = useFormValues<ReplyFormValues>({
    initialValues: {
      content: '',
    },
    validate: validateReply,
  });

  const { mutate: postReplyMutate } = useExecute({
    executeFn: postReviewsReviewIdReplies,
  });

  const handleSubmit = async () => {
    await postReplyMutate({
      reviewId,
      content: values.content,
    });
  };

  return (
    <Surface>
      <Shell>
        <Shell.Leading>
          <Avatar img="" size="small" />
        </Shell.Leading>
        <Shell.Content
          title={
            <Field>
              <Field.Content>
                <Input id="content" value={values.content} onChange={onChange} block />
                <Button variant="ghost" disabled={!isValid} onClick={handleSubmit}>
                  답글 남기기
                </Button>
              </Field.Content>
              {!valids.content && (
                <Field.Description>{errors.content[0]?.message}</Field.Description>
              )}
            </Field>
          }
        />
      </Shell>
    </Surface>
  );
};
