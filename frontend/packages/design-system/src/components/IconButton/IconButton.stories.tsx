import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { IconButton } from './';

import { ArrowRightIcon } from '../Icon/icons';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'IconButton/IconButton',
  component: IconButton,
} satisfies Meta<typeof IconButton>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: <ArrowRightIcon />,
  },
};
