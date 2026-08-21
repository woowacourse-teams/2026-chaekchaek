import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Callout } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Callout/Callout',
  component: Callout,
} satisfies Meta<typeof Callout>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: 'lorem ipsum dolor sit amet consectetur adipisicing elit.',
  },
};

export const WithLeading: Story = {
  args: {
    children: 'lorem ipsum dolor sit amet consectetur adipisicing elit.',
    leading: 'ⓘ',
  },
};
