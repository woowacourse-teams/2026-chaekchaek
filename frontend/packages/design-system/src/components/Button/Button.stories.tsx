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
    align: 'center',
  },
  argTypes: {
    shape: {
      control: 'inline-radio',
      options: ['default', 'text'],
    },
    variant: {
      control: 'select',
      options: ['default', 'primary', 'secondary', 'accent', 'ghost', 'danger', 'danger-weak'],
    },
    size: {
      control: 'inline-radio',
      options: ['x-small', 'small', 'medium', 'large'],
    },
    align: {
      control: 'inline-radio',
      options: ['center', 'left', 'right'],
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

export const DangerVariant: Story = {
  args: {
    children: 'danger',
    variant: 'danger',
  },
};

export const DangerWeakVariant: Story = {
  args: {
    children: 'danger-weak',
    variant: 'danger-weak',
  },
};

export const XSmallSize: Story = {
  args: {
    children: 'x-small',
    size: 'x-small',
    variant: 'primary',
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

export const LeftAlign: Story = {
  args: {
    align: 'left',
    block: true,
    children: 'Left aligned button',
  },
};

export const RightAlign: Story = {
  args: {
    align: 'right',
    block: true,
    children: 'Right aligned button',
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
