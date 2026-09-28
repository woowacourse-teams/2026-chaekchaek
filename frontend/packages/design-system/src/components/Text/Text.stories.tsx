import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Text } from './';

const colors = ['default', 'secondary', 'muted', 'accent', 'error', 'inverse'] as const;
const sizes = ['xx-small', 'x-small', 'small', 'medium', 'large', 'x-large'] as const;

const meta = {
  title: 'Text/Text',
  component: Text,
  args: {
    children: '책과 함께하는 일상을 기록해 보세요.',
    as: 'div',
    color: 'default',
    size: 'medium',
    strong: false,
  },
  argTypes: {
    children: {
      control: 'text',
      description: '표시할 텍스트입니다.',
    },
    as: {
      control: 'select',
      options: ['div', 'p', 'span'],
      description: '렌더링할 HTML 요소입니다. 문맥에 맞게 선택할 수 있습니다.',
    },
    color: {
      control: 'select',
      options: colors,
      description: '본문, 보조 본문, 부가 설명, 강조, 오류, 반전 색상을 선택합니다.',
    },
    size: {
      control: 'select',
      options: sizes,
      description: 'xx-small부터 순서대로 11, 12, 13, 14, 15, 16px에 대응하는 rem 크기입니다.',
    },
    strong: {
      control: 'boolean',
      description: '텍스트를 굵게 표시합니다. 기본 굵기는 400, 강조 굵기는 700입니다.',
    },
  },
  decorators: [
    (Story, { args }) => (
      <div
        style={{
          padding: '24px',
          background: args.color === 'inverse' ? '#1a1a1a' : '#fff',
          color: args.color === 'inverse' ? '#fff' : '#1a1a1a',
        }}
      >
        <Story />
      </div>
    ),
  ],
} satisfies Meta<typeof Text>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const AllColors: Story = {
  argTypes: {
    color: { control: false },
  },
  render: (args) => (
    <div style={{ display: 'grid', gap: '8px' }}>
      {colors.map((color) => (
        <div
          key={color}
          style={{
            padding: '12px 16px',
            background: color === 'inverse' ? '#1a1a1a' : '#fff',
          }}
        >
          <Text {...args} color={color}>
            {color} — {args.children}
          </Text>
        </div>
      ))}
    </div>
  ),
};

export const AllSizes: Story = {
  argTypes: {
    size: { control: false },
  },
  render: (args) => (
    <div style={{ display: 'grid', gap: '16px' }}>
      {sizes.map((size, index) => (
        <Text {...args} key={size} size={size}>
          {size} ({11 + index}px) — {args.children}
        </Text>
      ))}
    </div>
  ),
};

export const Strong: Story = {
  args: {
    children: '오래 기억하고 싶은 문장을 강조해 보세요.',
    strong: true,
  },
};

export const Inverse: Story = {
  args: {
    children: '어두운 배경 위에서도 선명하게 읽을 수 있어요.',
    color: 'inverse',
  },
};

export const AsSpan: Story = {
  args: {
    as: 'span',
    children: '함께 읽는 즐거움',
    strong: true,
  },
  render: (args) => (
    <div>
      책책에서 <Text {...args} />을 만나 보세요.
    </div>
  ),
};
