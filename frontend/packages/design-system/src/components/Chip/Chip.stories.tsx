import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Chip } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Chip/Chip',
  component: Chip,
  args: {
    children: 'Chip',
    size: 'medium',
    variant: 'ghost',
  },
  argTypes: {
    size: {
      control: 'inline-radio',
      options: ['small', 'medium', 'large'],
    },
    variant: {
      control: 'inline-radio',
      options: ['ghost', 'primary'],
    },
  },
} satisfies Meta<typeof Chip>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Default: Story = {};

export const GhostVariant: Story = {
  args: {
    children: 'ghost',
    variant: 'ghost',
  },
};

export const PrimaryVariant: Story = {
  args: {
    children: 'primary',
    variant: 'primary',
  },
};

export const Selected: Story = {
  args: {
    children: 'selected',
    selected: true,
  },
};

export const SmallSize: Story = {
  args: {
    children: 'small',
    size: 'small',
  },
};

export const MediumSize: Story = {
  args: {
    children: 'medium',
    size: 'medium',
  },
};

export const LargeSize: Story = {
  args: {
    children: 'large',
    size: 'large',
  },
};

export const SmallPrimary: Story = {
  args: {
    children: 'small primary',
    size: 'small',
    variant: 'primary',
  },
};

export const LargePrimary: Story = {
  args: {
    children: 'large primary',
    size: 'large',
    variant: 'primary',
  },
};

export const AsButton: Story = {
  args: {
    as: 'button',
    children: 'button chip',
  },
};
