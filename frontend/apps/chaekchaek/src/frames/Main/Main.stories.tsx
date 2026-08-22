import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Main } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'frames/Main',
  component: Main,
} satisfies Meta<typeof Main>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: '',
  },
};
