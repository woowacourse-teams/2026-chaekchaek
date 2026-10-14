import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Chip } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Chip/Chip',
  component: Chip,
  args: {
    children: 'Chip',
    size: 'medium',
    variant: 'default',
  },
  argTypes: {
    size: {
      control: 'inline-radio',
      options: ['small', 'medium', 'large'],
    },
    variant: {
      control: 'inline-radio',
      options: ['default', 'primary', 'subtle', 'ghost', 'soft'],
    },
  },
} satisfies Meta<typeof Chip>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Default: Story = {};

export const DefaultVariant: Story = {
  args: {
    children: 'Default',
    variant: 'default',
  },
};

export const PrimaryVariant: Story = {
  args: {
    children: 'Primary',
    variant: 'primary',
  },
};

export const SubtleVariant: Story = {
  args: {
    children: 'Subtle',
    variant: 'subtle',
  },
};

export const GhostVariant: Story = {
  args: {
    children: '댓글 46',
    variant: 'ghost',
  },
};

export const SoftVariant: Story = {
  args: {
    children: '다 읽음',
    variant: 'soft',
  },
};

export const Disabled: Story = {
  args: {
    children: 'Disabled',
    disabled: true,
  },
};

export const SmallSize: Story = {
  args: {
    children: 'Small',
    size: 'small',
    variant: 'subtle',
  },
};

export const MediumSize: Story = {
  args: {
    children: 'Medium',
    size: 'medium',
    variant: 'subtle',
  },
};

export const LargeSize: Story = {
  args: {
    children: 'Large',
    size: 'large',
    variant: 'subtle',
  },
};

export const AsButton: Story = {
  args: {
    as: 'button',
    children: 'button chip',
    variant: 'subtle',
  },
};
