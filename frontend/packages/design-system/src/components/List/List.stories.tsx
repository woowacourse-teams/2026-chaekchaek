import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ImgBox } from '#internal/components/ImgBox';
import DummyImg from '../../components/ImgBox/imgs/dummy.png';

import { List } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'List/List',
  component: List,
} satisfies Meta<typeof List>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: (
      <>
        <List.Item>
          <List.Item.Leading>Leading</List.Item.Leading>
          <List.Item.Content>Content</List.Item.Content>
          <List.Item.Trailing>Trailing</List.Item.Trailing>
        </List.Item>
      </>
    ),
  },
};

export const WithImgBox: Story = {
  args: {
    children: (
      <>
        <List.Item>
          <List.Item.Leading>
            <ImgBox img={DummyImg} />
          </List.Item.Leading>
          <List.Item.Content title="title" content="content" description="description" />
          <List.Item.Trailing>
            <span>Trailing</span>
            <span>Trailing2</span>
          </List.Item.Trailing>
        </List.Item>
      </>
    ),
  },
};
