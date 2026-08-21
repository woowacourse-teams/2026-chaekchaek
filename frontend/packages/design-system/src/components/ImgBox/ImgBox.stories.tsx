import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ImgBox } from './';

import DummyImg from './imgs/dummy.png';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'ImgBox/ImgBox',
  component: ImgBox,
} satisfies Meta<typeof ImgBox>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    img: DummyImg,
    size: 'medium',
  },
};
