import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';

import { Rating } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Rating/Rating',
  component: Rating,
} satisfies Meta<typeof Rating>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Medium: Story = {
  args: {
    value: 3,
  },
};

export const SizeSmall: Story = {
  args: {
    value: 3,
    size: 'small',
  },
};

export const WithTitle: Story = {
  args: {
    value: 4,
    title: '내 별점',
  },
};

export const WithDescription: Story = {
  args: {
    value: 4,
    description: '4점 · 좋았어요',
  },
};

export const WithTitleAndDescription: Story = {
  args: {
    value: 4,
    title: '내 별점',
    description: '4점 · 좋았어요',
  },
};

export const Controlled: Story = {
  args: {
    value: 4,
    title: '내 별점',
    description: '4점 · 좋았어요',
  },
  render: function Render(args) {
    const [{ value }, updateArgs] = useArgs();

    return <Rating {...args} value={value} onChange={(value) => updateArgs({ value })} />;
  },
};
