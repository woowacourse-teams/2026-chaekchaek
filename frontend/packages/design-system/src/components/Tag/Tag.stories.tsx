import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Tag } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Tag/Tag',
  component: Tag,
  args: {
    children: 'Tag',
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
      options: ['default', 'primary', 'ghost'],
    },
  },
} satisfies Meta<typeof Tag>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Default: Story = {};

export const Small: Story = {
  args: {
    children: 'Tag S',
    size: 'small',
  },
};

export const Medium: Story = {
  args: {
    children: 'Tag M',
    size: 'medium',
  },
};

export const Large: Story = {
  args: {
    children: 'Tag L',
    size: 'large',
  },
};

export const Primary: Story = {
  args: {
    variant: 'primary',
  },
};

export const Ghost: Story = {
  args: {
    variant: 'ghost',
  },
};

export const AsButton: Story = {
  args: {
    as: 'button',
    children: 'Button tag',
  },
};
