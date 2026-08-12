import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Overview } from './';

import { ImgBox } from '../ImgBox';
import DummyLargeImgImgBox from '../ImgBox/imgs/dummy-large.png';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Overview/Overview',
  component: Overview,
} satisfies Meta<typeof Overview>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: (
      <>
        <Overview.Content
          leading="leading"
          title="Title"
          content="content"
          description="Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
          nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
          accusantium magni quis voluptates, velit nisi dolorum id."
        />
        <Overview.Media>
          <ImgBox img={DummyLargeImgImgBox} />
        </Overview.Media>
      </>
    ),
  },
};
