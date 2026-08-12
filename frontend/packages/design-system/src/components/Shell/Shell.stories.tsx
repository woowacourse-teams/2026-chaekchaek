import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Shell } from './';

import { Avatar } from '../Avatar';
import DummyImgAvatar from '../Avatar/imgs/dummy-avatar.png';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Shell/Shell',
  component: Shell,
} satisfies Meta<typeof Shell>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: (
      <>
        <Shell.Leading>Leading</Shell.Leading>
        <Shell.Content title="title" content="content" />
        <Shell.Trailing>Trailing</Shell.Trailing>
      </>
    ),
  },
};

export const WithAvatar: Story = {
  args: {
    children: (
      <>
        <Shell.Leading>
          <Avatar img={DummyImgAvatar} />
        </Shell.Leading>
        <Shell.Content title="title" content="content" description="description" />
        <Shell.Trailing>Trailing</Shell.Trailing>
      </>
    ),
  },
};
