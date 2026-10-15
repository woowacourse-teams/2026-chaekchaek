import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Checkbox } from './';

const meta = {
  title: 'Checkbox/Checkbox',
  component: Checkbox,
  args: {
    'aria-label': 'Checkbox',
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

export const Boxed: Story = {
  args: {
    'aria-label': undefined,
  },
  render: () => (
    <div style={{ display: 'flex', width: 310, flexDirection: 'column' }}>
      <Checkbox boxed name="checkboxBoxed" value="first">
        Label 1
      </Checkbox>
      <Checkbox boxed defaultChecked name="checkboxBoxed" value="second">
        Label 2
      </Checkbox>
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
