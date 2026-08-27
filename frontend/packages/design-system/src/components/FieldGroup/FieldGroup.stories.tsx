import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { FieldGroup } from './';

import { Field } from '../Field/Field';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'FieldGroup/FieldGroup',
  component: FieldGroup,
} satisfies Meta<typeof FieldGroup>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    orientation: 'horizontal',
    children: (
      <>
        <Field>
          <Field.Label>Label</Field.Label>
          <Field.Content>Content</Field.Content>
        </Field>
        <Field>
          <Field.Label>Label</Field.Label>
          <Field.Content>Content</Field.Content>
        </Field>
      </>
    ),
  },
};

export const Vertical: Story = {
  args: {
    ...Example.args,
    orientation: 'vertical',
  },
};
