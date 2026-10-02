import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Media } from './';

import { ImgBox } from '../ImgBox';
import DummyLargeImg from '../ImgBox/imgs/dummy-large.png';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Media/Media',
  component: Media,
  args: {
    variant: 'default',
    media: <ImgBox img={DummyLargeImg} size="x-large" />,
    title: '마션',
    description: '앤디 위어',
  },
  argTypes: {
    variant: {
      control: 'inline-radio',
      options: ['default'],
    },
  },
} satisfies Meta<typeof Media>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const VariantDefault: Story = {};
