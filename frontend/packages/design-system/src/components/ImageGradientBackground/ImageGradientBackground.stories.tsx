import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ImageGradientBackground } from './';

const dummyImg = new URL('./imgs/dummy.jpg', import.meta.url).href;

const meta = {
  title: 'ImageGradientBackground/ImageGradientBackground',
  component: ImageGradientBackground,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    img: {
      control: 'text',
      description: '상단과 하단의 색상을 추출할 이미지 URL',
    },
  },
  args: {
    img: dummyImg,
    style: {
      width: 360,
      minHeight: 320,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: 32,
    },
  },
} satisfies Meta<typeof ImageGradientBackground>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {
  render: (args) => <ImageGradientBackground {...args} />,
};

export const Fallback: Story = {
  args: {
    img: 'data:image/jpeg;base64,invalid',
    children: '이미지를 불러올 수 없으면 기본 회색 그라데이션을 표시합니다.',
  },
};
