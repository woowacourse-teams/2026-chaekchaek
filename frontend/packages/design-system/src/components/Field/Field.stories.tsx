import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Field } from './';

import { Input } from '../Input';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Field/Field',
  component: Field,
} satisfies Meta<typeof Field>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: (
      <>
        <Field.Label>label</Field.Label>
        <Field.Content>
          <Input block />
          <Input block />
        </Field.Content>
        <Field.Description>Description</Field.Description>
      </>
    ),
  },
};
