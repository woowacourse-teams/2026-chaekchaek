import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Select } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Select/Select',
  component: Select,
} satisfies Meta<typeof Select>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    options: [
      { value: 'a', text: 'A' },
      { value: 'b', text: 'B' },
    ],
    value: 'a',
  },
};

export const SizeMedium: Story = {
  args: {
    options: [
      { value: 'a', text: 'A' },
      { value: 'b', text: 'B' },
    ],
    value: 'a',
    size: 'medium',
  },
};

export const SizeSmall: Story = {
  args: {
    options: [
      { value: 'a', text: 'A' },
      { value: 'b', text: 'B' },
    ],
    value: 'a',
    size: 'small',
  },
};
