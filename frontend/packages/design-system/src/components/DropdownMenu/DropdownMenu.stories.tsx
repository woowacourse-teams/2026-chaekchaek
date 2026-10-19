import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { DropdownMenu } from './';

import moreIcon from './imgs/more.svg';

const meta = {
  title: 'DropdownMenu/DropdownMenu',
  component: DropdownMenu,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    (Story) => (
      <div style={{ minHeight: 180, paddingInline: 80 }}>
        <Story />
      </div>
    ),
  ],
  args: {
    trigger: <img src={moreIcon} alt="" />,
    items: [
      { text: '수정', onClick: () => {} },
      { text: '삭제', onClick: () => {} },
    ],
  },
} satisfies Meta<typeof DropdownMenu>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {};

export const Open: Story = {
  play: async ({ canvasElement }) => {
    canvasElement.querySelector<HTMLButtonElement>('[aria-haspopup="menu"]')?.click();
  },
};
