import { Avatar, Button, Field, Input, Shell, Surface } from '@chaekchaek/design-system';

import { useFormValues } from '@/hooks/useFormValues';

import { validateReply } from './validator';
import type { ReplyFormValues } from './validator';

export const WriteReply = () => {
  const { values, errors, onChange, isValid, valids } = useFormValues<ReplyFormValues>({
    initialValues: {
      content: '',
    },
    validate: validateReply,
  });

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
                <Button variant="ghost" disabled={!isValid}>
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
