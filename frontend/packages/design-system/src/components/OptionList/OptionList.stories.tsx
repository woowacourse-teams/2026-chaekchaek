import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { OptionList } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'OptionList/OptionList',
  component: OptionList,
} satisfies Meta<typeof OptionList>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    title: 'Title',
    shape: 'default',
    options: [
      { value: 'a', text: 'A', meta: 0 },
      { value: 'b', text: 'B', meta: 1 },
    ],
    value: 'a',
  },
};
