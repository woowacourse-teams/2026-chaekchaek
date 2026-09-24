import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { IconButton } from './';

import { ArrowRightIcon } from '../Icon/icons';

const meta = {
  title: 'Button/IconButton',
  component: IconButton,
  args: {
    'aria-label': '다음으로 이동',
    children: <ArrowRightIcon />,
    shape: 'default',
    variant: 'default',
    size: 'medium',
  },
  argTypes: {
    shape: {
      control: 'inline-radio',
      options: ['default', 'normal', 'link'],
    },
    variant: {
      control: 'select',
      options: [
        'default',
        'primary',
        'secondary',
        'accent',
        'ghost',
        'soft',
        'danger',
        'danger-weak',
      ],
    },
    size: {
      control: 'inline-radio',
      options: ['small', 'medium', 'large'],
    },
  },
} satisfies Meta<typeof IconButton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const PrimaryVariant: Story = {
  args: {
    children: <ArrowRightIcon color="inverse" />,
    variant: 'primary',
  },
};

export const SecondaryVariant: Story = {
  args: { variant: 'secondary' },
};

export const AccentVariant: Story = {
  args: { variant: 'accent' },
};

export const GhostVariant: Story = {
  args: { variant: 'ghost' },
};

export const SoftVariant: Story = {
  args: { variant: 'soft' },
};

export const DangerVariant: Story = {
  args: { variant: 'danger' },
};

export const DangerWeakVariant: Story = {
  args: { variant: 'danger-weak' },
};

export const SmallSize: Story = {
  args: { size: 'small' },
};

export const LargeSize: Story = {
  args: { size: 'large' },
};

export const NormalShape: Story = {
  args: { shape: 'normal' },
};

export const LinkShape: Story = {
  args: { shape: 'link' },
};

export const Disabled: Story = {
  args: {
    'aria-disabled': true,
    children: <ArrowRightIcon color="inverse" />,
    variant: 'primary',
  },
};
