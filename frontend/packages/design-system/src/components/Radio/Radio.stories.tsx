import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Radio } from './';

const meta = {
  title: 'Radio/Radio',
  component: Radio,
  args: {
    'aria-label': '책 선택',
  },
} satisfies Meta<typeof Radio>;

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

export const Group: Story = {
  args: {
    'aria-label': undefined,
  },
  render: () => (
    <div style={{ display: 'flex', gap: 16 }}>
      <Radio defaultChecked name="book" value="first">
        첫 번째 책
      </Radio>
      <Radio name="book" value="second">
        두 번째 책
      </Radio>
    </div>
  ),
};
