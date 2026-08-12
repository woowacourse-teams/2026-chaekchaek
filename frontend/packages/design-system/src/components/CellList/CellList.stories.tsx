import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { CellList } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'CellList/CellList',
  component: CellList,
} satisfies Meta<typeof CellList>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    title: (
      <>
        <span>내 평점 기록</span>
        <span>5회</span>
      </>
    ),
    children: (
      <>
        <CellList.Item headline="3.1" title="불고기는 존재하지…" content="2026.04.03" />
        <CellList.Item headline="3.5" title="보이지 않는 도시" content="2026.05.12" />
        <CellList.Item headline="4.0" title="역병" content="2026.06.21" />
        <CellList.Item headline="4.2" title="아몬드" content="2026.07.18" />
        <CellList.Item headline="4.0" title="마션" content="2026.08.05" />
      </>
    ),
  },
  decorators: [
    (Story) => (
      <div style={{ width: 484 }}>
        <Story />
      </div>
    ),
  ],
};
