import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Button } from '../Button';
import { VisuallyHidden } from '.';

const meta = {
  title: 'VisuallyHidden/VisuallyHidden',
  component: VisuallyHidden,
  parameters: {
    docs: {
      description: {
        component:
          '화면에서는 콘텐츠를 숨기고 스크린 리더에는 제공합니다. 기본 요소는 span이며 as로 변경할 수 있습니다. 포커스 가능한 요소 자체를 숨기는 용도로 사용하지 마세요.',
      },
    },
  },
} satisfies Meta<typeof VisuallyHidden>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {
  args: {
    children: '이 문장은 화면에 보이지 않지만 스크린 리더로 읽을 수 있습니다.',
  },
  render: (args) => (
    <div>
      <p>숨겨진 안내 문장이 있습니다. 스크린 리더로 확인해 보세요.</p>
      <VisuallyHidden {...args} />
    </div>
  ),
};

export const IconButton: Story = {
  render: () => (
    <Button type="button">
      <span aria-hidden="true">×</span>
      <VisuallyHidden>닫기</VisuallyHidden>
    </Button>
  ),
};

export const HiddenHeading: Story = {
  render: () => (
    <section aria-labelledby="visually-hidden-heading">
      <VisuallyHidden as="h2" id="visually-hidden-heading">
        추천 도서
      </VisuallyHidden>
      <p>제목은 화면에서 숨겨지지만 스크린 리더의 제목 탐색에 포함됩니다.</p>
      <ul>
        <li>어린 왕자</li>
        <li>데미안</li>
      </ul>
    </section>
  ),
};
