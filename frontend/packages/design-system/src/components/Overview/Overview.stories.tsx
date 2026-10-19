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
          title="Title"
          content="content"
          description="Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
          nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
          accusantium magni quis voluptates, velit nisi dolorum id."
          meta="meta"
        />
        <Overview.Media>
          <ImgBox img={DummyLargeImgImgBox} />
        </Overview.Media>
      </>
    ),
  },
};

export const ShortDescription: Story = {
  name: '짧은 설명',
  args: {
    children: (
      <Overview.Content
        title="Title"
        content="content"
        description="책과 함께하는 일상을 기록하고, 오래 기억하고 싶은 문장을 나눠 보세요."
        meta="meta"
      />
    ),
  },
};

export const FourLineDescription: Story = {
  name: '정확히 4줄인 설명',
  args: {
    children: (
      <Overview.Content
        title="Title"
        content="content"
        description={
          <>
            첫 번째 줄: 책과 함께하는 일상입니다.
            <br />
            두 번째 줄: 마음에 드는 문장을 기록합니다.
            <br />
            세 번째 줄: 읽고 느낀 생각을 나눕니다.
            <br />네 번째 줄: 설명이 네 줄 안에 모두 표시됩니다.
          </>
        }
        meta="meta"
      />
    ),
  },
};

export const LongDescription: Story = {
  name: '4줄을 넘는 설명 · 더보기',
  args: {
    children: (
      <Overview.Content
        title="Title"
        content="content"
        description={
          <>
            첫 번째 줄: 책과 함께하는 일상입니다.
            <br />
            두 번째 줄: 마음에 드는 문장을 기록합니다.
            <br />
            세 번째 줄: 읽고 느낀 생각을 나눕니다.
            <br />
            네 번째 줄: 아래쪽에 그라데이션이 표시됩니다.
            <br />
            다섯 번째 줄: 더보기 버튼을 눌러 확인합니다.
            <br />
            여섯 번째 줄: 버튼을 다시 누르면 접힙니다.
          </>
        }
        meta="meta"
      />
    ),
  },
};
