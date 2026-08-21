import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Checkbox } from './';

const meta = {
  title: 'Checkbox/Checkbox',
  component: Checkbox,
  args: {
    'aria-label': '책 선택',
  },
} satisfies Meta<typeof Checkbox>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Unchecked: Story = {};

export const Checked: Story = {
  args: {
    defaultChecked: true,
  },
};

export const WithLabel: Story = {
  args: {
    children: '책 선택',
  },
};

export const Disabled: Story = {
  args: {
    disabled: true,
  },
};
