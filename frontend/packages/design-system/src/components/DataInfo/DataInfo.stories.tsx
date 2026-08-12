import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { DataInfo } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'DataInfo/DataInfo',
  component: DataInfo,
} satisfies Meta<typeof DataInfo>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    heading: 'Heading',
    children: (
      <>
        <DataInfo.Item title="title" content="content" />
        <DataInfo.Item title="title" content="content" />
      </>
    ),
  },
};

export const WithoutHeading: Story = {
  args: {
    children: (
      <>
        <DataInfo.Item title="저자" content="앤디 위어" />
        <DataInfo.Item title="출판사" content="알에이치코리아" />
      </>
    ),
  },
};
