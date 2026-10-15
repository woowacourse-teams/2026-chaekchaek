import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { InfoMessage } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'InfoMessage/InfoMessage',
  component: InfoMessage,
} satisfies Meta<typeof InfoMessage>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    visual: '▧',
    title: '안내 메시지입니다.',
    content: '내용 보기',
  },
};
