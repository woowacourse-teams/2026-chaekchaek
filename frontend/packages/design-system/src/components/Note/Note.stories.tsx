import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Note } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Note/Note',
  component: Note,
} satisfies Meta<typeof Note>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children:
      'Lorem ipsum dolor sit amet consectetur adipisicing elit. Illum vitae, cumque assumenda rerum aut nisi blanditiis, architecto praesentium itaque recusandae nostrum voluptate, aliquam tenetur temporibus ipsam! Soluta minima consequatur beatae?',
  },
};

export const WithTitle: Story = {
  args: {
    title: 'Title',
    children:
      'Lorem ipsum dolor sit amet consectetur adipisicing elit. Illum vitae, cumque assumenda rerum aut nisi blanditiis, architecto praesentium itaque recusandae nostrum voluptate, aliquam tenetur temporibus ipsam! Soluta minima consequatur beatae?',
  },
};

export const VariantSubtle: Story = {
  args: {
    variant: 'subtle',
    children:
      'Lorem ipsum dolor sit amet consectetur adipisicing elit. Illum vitae, cumque assumenda rerum aut nisi blanditiis, architecto praesentium itaque recusandae nostrum voluptate, aliquam tenetur temporibus ipsam! Soluta minima consequatur beatae?',
  },
};
