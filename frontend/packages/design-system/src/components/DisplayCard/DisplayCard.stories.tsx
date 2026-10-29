import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { DisplayCard } from './';

import { ConstrainedImgBox } from '#internal/components/ConstrainedImgBox';
import { Showcase } from '#internal/components/Showcase';

import DummyImg1 from '../ConstrainedImgBox/imgs/dummy1.jpg';
import DummyImg2 from '../ConstrainedImgBox/imgs/dummy2.jpg';
import DummyImg3 from '../ConstrainedImgBox/imgs/dummy3.jpg';

const meta = {
  title: 'DisplayCard/DisplayCard',
  component: DisplayCard,
} satisfies Meta<typeof DisplayCard>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {
  args: {
    title: '컬렉션 제목',
    description: '마음에 드는 항목을 모아 보세요',
    extra: 'Sample',
    children: (
      <Showcase>
        <ConstrainedImgBox img={DummyImg1} height="162px" />
        <ConstrainedImgBox img={DummyImg2} height="182.546px" />
        <ConstrainedImgBox img={DummyImg3} height="149px" />
      </Showcase>
    ),
  },
};
