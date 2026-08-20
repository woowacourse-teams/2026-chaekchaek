import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ProgressBar } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'ProgressBar/ProgressBar',
  component: ProgressBar,
} satisfies Meta<typeof ProgressBar>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    value: 10,
    max: 100,
  },
};

export const WithHeader: Story = {
  args: {
    value: 10,
    max: 100,
    title: 'Title',
    label: <>196쪽 / 196쪽</>,
  },
};
