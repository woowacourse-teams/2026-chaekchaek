import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Title } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Title/Title',
  component: Title,
} satisfies Meta<typeof Title>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: 'Title',
    trailing: 'trailing',
  },
};

export const LevelPage: Story = {
  args: {
    level: 'page',
    children: 'Title',
    trailing: 'trailing',
  },
};

export const LevelMain: Story = {
  args: {
    level: 'main',
    children: 'Title',
    trailing: 'trailing',
  },
};

export const LevelCaption: Story = {
  args: {
    level: 'caption',
    children: 'Title',
    trailing: 'trailing',
  },
};
