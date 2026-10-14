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
    children: 'With Label',
  },
};

export const Disabled: Story = {
  args: {
    disabled: true,
  },
};

export const Medium: Story = {
  args: {
    children: 'Medium',
    defaultChecked: true,
    size: 'medium',
  },
};

export const Large: Story = {
  args: {
    children: 'Large',
    defaultChecked: true,
    size: 'large',
  },
};

export const Accent: Story = {
  args: {
    children: 'Accent',
    defaultChecked: true,
    variant: 'accent',
  },
};

export const Boxed: Story = {
  args: {
    'aria-label': undefined,
  },
  render: () => (
    <div style={{ display: 'flex', width: 310, flexDirection: 'column' }}>
      <Radio boxed name="radioBoxed" value="want-to-read" variant="accent">
        Label1
      </Radio>
      <Radio boxed defaultChecked name="radioBoxed" value="reading" variant="accent">
        Label2
      </Radio>
    </div>
  ),
};

export const Block: Story = {
  args: {
    block: true,
    boxed: true,
    children: 'Block',
  },
};
