import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Split } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Split/Split',
  component: Split,
} satisfies Meta<typeof Split>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: (
      <>
        <Split.Side>Side</Split.Side>
        <Split.Content>Content</Split.Content>
      </>
    ),
  },
};
