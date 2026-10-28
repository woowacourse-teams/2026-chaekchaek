import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ScrollArea } from './';

const meta = {
  title: 'ScrollArea/ScrollArea',
  component: ScrollArea,
  argTypes: {
    height: {
      control: 'number',
      description: 'CSS height 값. 숫자는 px 단위로 적용됩니다.',
    },
  },
  args: {
    height: 200,
    children: Array.from({ length: 20 }, (_, index) => (
      <p key={index}>스크롤 영역의 {index + 1}번째 항목입니다.</p>
    )),
  },
} satisfies Meta<typeof ScrollArea>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {};
