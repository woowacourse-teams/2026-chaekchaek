import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Button } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Button/Button',
  component: Button,
  args: {
    children: 'Button',
    shape: 'default',
    variant: 'default',
    size: 'medium',
  },
  argTypes: {
    shape: {
      control: 'inline-radio',
      options: ['default', 'text'],
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
} satisfies Meta<typeof Button>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Default: Story = {};

export const DefaultVariant: Story = {
  args: {
    children: 'default',
    variant: 'default',
  },
};

export const PrimaryVariant: Story = {
  args: {
    children: 'primary',
    variant: 'primary',
  },
};

export const SecondaryVariant: Story = {
  args: {
    children: 'secondary',
    variant: 'secondary',
  },
};

export const AccentVariant: Story = {
  args: {
    children: 'accent',
    variant: 'accent',
  },
};

export const GhostVariant: Story = {
  args: {
    children: 'ghost',
    variant: 'ghost',
  },
};

export const SoftVariant: Story = {
  args: {
    children: '감상 익명 공개',
    variant: 'soft',
  },
};

export const DangerVariant: Story = {
  args: {
    children: 'danger',
    variant: 'danger',
  },
};

export const DangerWeakVariant: Story = {
  args: {
    children: '2권 삭제',
    variant: 'danger-weak',
  },
};

export const SmallSize: Story = {
  args: {
    children: 'small',
    size: 'small',
    variant: 'primary',
  },
};

export const MediumSize: Story = {
  args: {
    children: 'medium',
    size: 'medium',
    variant: 'primary',
  },
};

export const LargeSize: Story = {
  args: {
    children: 'large',
    size: 'large',
    variant: 'primary',
  },
};

export const TextShape: Story = {
  args: {
    children: 'Text button',
    shape: 'link',
    variant: 'ghost',
    size: 'medium',
  },
};

export const Block: Story = {
  args: {
    children: 'button',
    block: true,
  },
};

export const Disabled: Story = {
  args: {
    'aria-disabled': true,
    children: 'Disabled button',
    variant: 'primary',
  },
};

export const WithLeading: Story = {
  args: {
    children: 'button',
    leading: 'ⓘ',
  },
};

export const WithTrailing: Story = {
  args: {
    children: 'button',
    trailing: 'ⓘ',
  },
};
