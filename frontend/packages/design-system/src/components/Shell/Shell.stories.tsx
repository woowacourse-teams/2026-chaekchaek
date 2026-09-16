import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Shell } from './';

import { Avatar } from '../Avatar';
import DummyImgAvatar from '../Avatar/imgs/dummy-avatar.png';
import { ImgBox } from '../ImgBox';
import DummyImgBox from '../ImgBox/imgs/dummy.png';

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

export const WithImgBox: Story = {
  args: {
    children: (
      <>
        <Shell.Leading>
          <ImgBox size="small" img={DummyImgBox} />
        </Shell.Leading>
        <Shell.Content
          title="title"
          content={
            <>
              <Avatar size="x-small" img={DummyImgAvatar} />
              Content
            </>
          }
        />
      </>
    ),
  },
};

export const VerticalAlign: Story = {
  render: (args) => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {(['top', 'center', 'bottom'] as const).map((verticalAlign) => (
        <div key={verticalAlign}>
          <p>{verticalAlign}</p>
          <Shell {...args}>
            <Shell.Leading>
              <ImgBox size="small" img={DummyImgBox} />
            </Shell.Leading>
            <Shell.Content
              verticalAlign={verticalAlign}
              title="title"
              content="content"
              style={{ height: 96, backgroundColor: '#f5f5f5' }}
            />
          </Shell>
        </div>
      ))}
    </div>
  ),
  args: {
    children: null,
  },
};

export const Reverse: Story = {
  decorators: [
    (Story) => (
      <div style={{ padding: 16, backgroundColor: '#000000' }}>
        <Story />
      </div>
    ),
  ],
  args: {
    reverse: true,
    children: (
      <>
        <Shell.Leading>
          <ImgBox size="small" img={DummyImgBox} />
        </Shell.Leading>
        <Shell.Content
          title="title"
          content={
            <>
              <Avatar size="x-small" img={DummyImgAvatar} />
              Content
            </>
          }
        />
      </>
    ),
  },
};
