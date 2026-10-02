import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ConstrainedImgBox } from '.';

import DummyImg1 from './imgs/dummy1.jpg';
import DummyImg2 from './imgs/dummy2.jpg';
import DummyImg3 from './imgs/dummy3.jpg';
import DummyImg4 from './imgs/dummy4.jpg';
import DummyImg5 from './imgs/dummy5.jpg';
import DummyImg6 from './imgs/dummy6.jpg';
import DummyImg7 from './imgs/dummy7.jpg';
import DummyImg8 from './imgs/dummy8.jpg';
import DummyImg9 from './imgs/dummy9.jpg';
import DummyImg10 from './imgs/dummy10.jpg';

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
  title: 'ConstrainedImgBox/ConstrainedImgBox',
  component: ConstrainedImgBox,
} satisfies Meta<typeof ConstrainedImgBox>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    img: DummyImg1,
    height: 'auto',
  },
};

export const Examples: Story = {
  render: () => {
    return (
      <>
        {dummyImgs.map((dummyImg) => {
          return <ConstrainedImgBox img={dummyImg} height="320px" />;
        })}
      </>
    );
  },
};
