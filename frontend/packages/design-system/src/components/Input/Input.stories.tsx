import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { CloseIcon, SearchIcon } from '../Icon';

import { Input } from '.';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Input/Input',
  component: Input,
} satisfies Meta<typeof Input>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    value: 'Input',
  },
};

export const Reverse: Story = {
  args: {
    reverse: true,
    placeholder: '책, 작가, 감상 검색',
  },
  decorators: [
    (Story) => (
      <div style={{ width: '280px', padding: '16px', backgroundColor: '#16181c' }}>
        <Story />
      </div>
    ),
  ],
};

export const WithLeading: Story = {
  args: {
    leading: <SearchIcon size="small" />,
    placeholder: '책, 작가, 감상 검색',
  },
};

export const WithTrailing: Story = {
  args: {
    trailing: <CloseIcon size="small" />,
    value: 'Input',
    readOnly: true,
  },
};

export const WithLeadingAndTrailing: Story = {
  args: {
    leading: <SearchIcon size="small" />,
    trailing: <CloseIcon size="small" />,
    value: 'Input',
    readOnly: true,
  },
};

export const SizeSmall: Story = {
  args: {
    size: 'small',
    value: 'Input',
  },
};

export const SizeMedium: Story = {
  args: {
    size: 'medium',
    value: 'Input',
  },
};

export const SizeLarge: Story = {
  args: {
    size: 'large',
    value: 'Input',
  },
};
