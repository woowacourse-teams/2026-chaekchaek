import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Surface } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Surface/Surface',
  component: Surface,
} satisfies Meta<typeof Surface>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: '',
  },
};
