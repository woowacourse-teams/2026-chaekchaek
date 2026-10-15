import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useState } from 'react';

import { Choice } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Choice/Choice',
  component: Choice,
  args: {
    distribution: 'equal',
    value: 'option-2',
    options: [
      { value: 'option-1', text: 'Option 1' },
      { value: 'option-2', text: 'Option 2' },
      { value: 'option-3', text: 'Option 3' },
    ],
  },
  render: function InteractiveChoice(args) {
    const [value, setValue] = useState(args.value);

    return <Choice {...args} value={value} onChange={setValue} />;
  },
} satisfies Meta<typeof Choice>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {};

export const Content: Story = {
  args: {
    distribution: 'content',
    value: 'option-1',
    options: [
      { value: 'option-1', text: 'Option 1' },
      { value: 'option-2', text: 'Option 2' },
    ],
  },
};
