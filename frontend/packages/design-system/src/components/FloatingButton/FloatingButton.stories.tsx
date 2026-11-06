import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { FloatingButton } from './';

import { LongArrowUpIcon } from '../Icon';

const meta = {
  title: 'Button/FloatingButton',
  component: FloatingButton,
  parameters: {
    layout: 'centered',
  },
  args: {
    'aria-label': '맨 위로 이동',
    children: <LongArrowUpIcon color="inverse" aria-hidden />,
  },
  argTypes: {
    children: { control: false },
  },
} satisfies Meta<typeof FloatingButton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {};
