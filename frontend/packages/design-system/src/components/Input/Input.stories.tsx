import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Input } from '.';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Input/Input',
  component: Input,
} satisfies Meta<typeof Input>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    value: 'Input',
  },
};

export const SizeSmall: Story = {
  args: {
    size: 'small',
    value: 'Input',
  },
};

export const SizeMedium: Story = {
  args: {
    size: 'medium',
    value: 'Input',
  },
};

export const SizeLarge: Story = {
  args: {
    size: 'large',
    value: 'Input',
  },
};
