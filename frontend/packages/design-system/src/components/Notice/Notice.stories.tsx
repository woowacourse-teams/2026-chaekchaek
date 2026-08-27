import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Notice } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Notice/Notice',
  component: Notice,
} satisfies Meta<typeof Notice>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: '표시할 데이터가 없습니다.',
    height: 200,
  },
};
