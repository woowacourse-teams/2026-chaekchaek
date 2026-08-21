import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Button } from '#internal/components/Button';

import { ButtonStack } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'ButtonStack/ButtonStack',
  component: ButtonStack,
  parameters: {
    layout: 'centered',
  },
  decorators: [
    (Story) => (
      <div style={{ width: 420 }}>
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof ButtonStack>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Horizontal: Story = {
  args: {
    children: (
      <>
        <Button variant="ghost">취소</Button>
        <Button variant="primary">확인</Button>
      </>
    ),
  },
};

export const Vertical: Story = {
  args: {
    direction: 'vertical',
    children: (
      <>
        <Button variant="primary">확인</Button>
        <Button variant="ghost">취소</Button>
      </>
    ),
  },
};
