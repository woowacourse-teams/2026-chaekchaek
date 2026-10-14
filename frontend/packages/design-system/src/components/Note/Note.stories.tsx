import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Note } from '.';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Note/Note',
  component: Note,
  args: {
    children: '나는 이 행성에서 과학으로 헤쳐 나갈 것이다.',
  },
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
