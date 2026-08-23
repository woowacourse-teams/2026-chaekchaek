import { Avatar, Button, Field, Input, Shell, Surface } from '@chaekchaek/design-system';

export const WriteReply = () => {
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
                <Input block />
                <Button variant="ghost">답글 남기기</Button>
              </Field.Content>
            </Field>
          }
        />
      </Shell>
    </Surface>
  );
};
