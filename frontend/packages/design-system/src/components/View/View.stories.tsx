import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { View } from './View';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'View/View',
  component: View,
} satisfies Meta<typeof View>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const ViewExample: Story = {
  args: {
    children: '',
  },
};

export const Spacing: Story = {
  args: {
    sx: { mt: 10, mr: 5, mb: 4, ml: 2 },
    children: 'Spacing',
  },
};
