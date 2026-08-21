import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';

import { SegmentedControl } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'SegmentedControl/SegmentedControl',
  component: SegmentedControl,
} satisfies Meta<typeof SegmentedControl>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const ShapeDefault: Story = {
  args: {
    shape: 'default',
    options: [
      { value: 'a', text: 'A' },
      { value: 'b', text: 'B' },
    ],
    value: 'a',
  },
};

export const ShapeNormal: Story = {
  args: {
    shape: 'normal',
    options: [
      { value: 'a', text: 'A' },
      { value: 'b', text: 'B' },
    ],
    value: 'a',
  },
};

export const Controlled: Story = {
  args: {
    options: [
      { value: 'a', text: 'A' },
      { value: 'b', text: 'B' },
    ],
    value: 'a',
  },

  render: function Render(args) {
    const [{ value }, updateArgs] = useArgs();

    return <SegmentedControl {...args} value={value} onChange={(value) => updateArgs({ value })} />;
  },
};
