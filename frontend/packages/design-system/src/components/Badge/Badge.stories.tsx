import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Badge } from '.';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Badge/Badge',
  component: Badge,
  args: {
    children: 'Badge',
    size: 'medium',
    variant: 'default',
    reverse: false,
  },
  argTypes: {
    size: {
      control: 'inline-radio',
      options: ['small', 'medium'],
    },
    variant: {
      control: 'inline-radio',
      options: ['default', 'ghost', 'subtle'],
    },
  },
} satisfies Meta<typeof Badge>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Default: Story = {};

export const SmallSize: Story = {
  args: {
    children: 'small',
    size: 'small',
    variant: 'ghost',
  },
};

export const MediumSize: Story = {
  args: {
    children: 'medium',
    size: 'medium',
    variant: 'ghost',
  },
};

export const Ghost: Story = {
  args: {
    children: 'Badge',
    variant: 'ghost',
  },
};

export const GhostReverse: Story = {
  args: {
    children: '서재 124',
    variant: 'ghost',
    reverse: true,
  },
  decorators: [
    (Story) => (
      <div style={{ padding: '16px', background: '#17191d' }}>
        <Story />
      </div>
    ),
  ],
};

export const Subtle: Story = {
  args: {
    children: '댓글 46',
    size: 'small',
    variant: 'subtle',
  },
};
