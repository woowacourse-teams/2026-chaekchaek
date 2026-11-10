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
    variant: 'default',
    children: <LongArrowUpIcon aria-hidden />,
  },
  argTypes: {
    children: { control: false },
    variant: {
      control: 'inline-radio',
      options: ['default', 'primary', 'secondary'],
    },
  },
  render: (args) => (
    <FloatingButton {...args}>
      <LongArrowUpIcon color={args.variant === 'primary' ? 'inverse' : 'default'} aria-hidden />
    </FloatingButton>
  ),
} satisfies Meta<typeof FloatingButton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {};

export const PrimaryVariant: Story = {
  args: { variant: 'primary' },
};

export const SecondaryVariant: Story = {
  args: { variant: 'secondary' },
};
