import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Avatar } from './';

import DummyImg from './imgs/dummy-avatar.png';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Avatar/Avatar',
  component: Avatar,
} satisfies Meta<typeof Avatar>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    img: DummyImg,
  },
};

export const XSmall: Story = {
  args: {
    size: 'x-small',
    img: DummyImg,
  },
};

export const Small: Story = {
  args: {
    size: 'small',
    img: DummyImg,
  },
};

export const Medium: Story = {
  args: {
    size: 'medium',
    img: DummyImg,
  },
};

export const None: Story = {
  args: {
    img: null,
  },
};
