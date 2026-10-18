import { type KeyboardEvent } from 'react';

import { Avatar, Button, Field, Shell, Surface, Textarea } from '@chaekchaek/design-system';

import { useFormValues } from '@/hooks/useFormValues';

import { useExecute } from '@/services/core/useExecute';

import { patchRepliesReplyId } from '@/services/apis/repliesReplyId/repository';

import { validateReply } from './validator';
import type { ReplyFormValues } from './validator';

import type { UpdateReplyProps } from './UpdateReply.types';
import { useAuthContext } from '@/contexts/AuthContext/useAuthContext';

export const UpdateReply = ({ reply, onReplyUpdated }: UpdateReplyProps) => {
  const { values, errors, onChange, isValid, valids } = useFormValues<ReplyFormValues>({
    initialValues: reply,
    validate: validateReply,
  });

  const { user } = useAuthContext();

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();

      handleSubmit(reply.replyId);
    }
  };

  const { mutate: updateReply } = useExecute({
    executeFn: patchRepliesReplyId,
  });

  const handleSubmit = async (replyId: number) => {
    await updateReply({ replyId, content: values.content });

    await onReplyUpdated();
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
                <Button
                  type="button"
                  variant="ghost"
                  disabled={!isValid}
                  onClick={() => {
                    handleSubmit(reply.replyId);
                  }}
                >
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
