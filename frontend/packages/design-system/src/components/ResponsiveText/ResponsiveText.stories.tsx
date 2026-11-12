import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ResponsiveText } from './';

const meta = {
  title: 'ResponsiveText/ResponsiveText',
  component: ResponsiveText,
  parameters: {
    layout: 'padded',
  },
  argTypes: {
    direction: {
      control: 'inline-radio',
      options: ['horizontal', 'vertical'],
      description: '가로쓰기 또는 세로쓰기 방향을 지정합니다.',
      table: { defaultValue: { summary: 'horizontal' } },
    },
  },
  args: {
    children: '책책 함께 읽는 즐거움 ABC 123',
  },
} satisfies Meta<typeof ResponsiveText>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Horizontal: Story = {
  args: {
    direction: 'horizontal',
  },
};

export const Vertical: Story = {
  args: {
    direction: 'vertical',
  },
};

export const VerticalWrapping: Story = {
  args: {
    direction: 'vertical',
    children: '책을 읽으며 만나는 새로운 세상. 함께 읽고 생각을 나눠 보세요.',
    style: { blockSize: '12rem', inlineSize: '10rem' },
  },
};
