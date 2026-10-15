import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ImgBox } from './';

import DummyImg from './imgs/dummy.png';

const meta = {
  title: 'ImgBox/ImgBox',
  component: ImgBox,
} satisfies Meta<typeof ImgBox>;

export default meta;
type Story = StoryObj<typeof meta>;

export const XSmallSize: Story = {
  args: {
    img: DummyImg,
    size: 'x-small',
  },
};

export const SmallSize: Story = {
  args: {
    img: DummyImg,
    size: 'small',
  },
};

export const MediumSize: Story = {
  args: {
    img: DummyImg,
    size: 'medium',
  },
};

export const LargeSize: Story = {
  args: {
    img: DummyImg,
    size: 'large',
  },
};

export const XLargeSize: Story = {
  args: {
    img: DummyImg,
    size: 'x-large',
  },
};

export const Reverse: Story = {
  args: {
    img: DummyImg,
    size: 'medium',
    reverse: true,
  },
  decorators: [
    (Story) => (
      <div style={{ padding: 16, backgroundColor: '#191919' }}>
        <Story />
      </div>
    ),
  ],
};
