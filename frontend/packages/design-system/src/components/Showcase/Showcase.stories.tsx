import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Showcase } from './';

import { ConstrainedImgBox } from '#internal/components/ConstrainedImgBox';

import DummyImg1 from '../ConstrainedImgBox/imgs/dummy1.jpg';
import DummyImg2 from '../ConstrainedImgBox/imgs/dummy2.jpg';
import DummyImg3 from '../ConstrainedImgBox/imgs/dummy3.jpg';
import DummyImg4 from '../ConstrainedImgBox/imgs/dummy4.jpg';
import DummyImg5 from '../ConstrainedImgBox/imgs/dummy5.jpg';
import DummyImg6 from '../ConstrainedImgBox/imgs/dummy6.jpg';
import DummyImg7 from '../ConstrainedImgBox/imgs/dummy7.jpg';
import DummyImg8 from '../ConstrainedImgBox/imgs/dummy8.jpg';
import DummyImg9 from '../ConstrainedImgBox/imgs/dummy9.jpg';
import DummyImg10 from '../ConstrainedImgBox/imgs/dummy10.jpg';

const dummyImgs = [
  DummyImg1,
  DummyImg2,
  DummyImg3,
  DummyImg4,
  DummyImg5,
  DummyImg6,
  DummyImg7,
  DummyImg8,
  DummyImg9,
  DummyImg10,
];

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Showcase/Showcase',
  component: Showcase,
} satisfies Meta<typeof Showcase>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: (
      <>
        {dummyImgs.map((dummyImg) => {
          return <ConstrainedImgBox img={dummyImg} height="170px" />;
        })}
      </>
    ),
  },
};
