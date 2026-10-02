import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { ContentArea } from './';

import { Surface } from '../Surface';
import { Text } from '../Text';
import { Title } from '../Title';

const meta = {
  title: 'ContentArea/ContentArea',
  component: ContentArea,
  parameters: {
    layout: 'fullscreen',
    docs: {
      description: {
        component:
          '화면 너비를 채우고 메인 콘텐츠에 안쪽 여백을 줍니다. medium은 좌우 70px·상하 36px, large는 좌우 150px·상하 48px입니다. spacingX와 spacingY는 spacing보다 우선하며, spacing과 축별 설정이 모두 없으면 해당 축의 여백은 0입니다.',
      },
    },
  },
  argTypes: {
    spacing: {
      control: 'select',
      options: ['none', 'medium', 'large'],
      description: '좌우와 상하 여백을 함께 설정합니다.',
    },
    spacingX: {
      control: 'select',
      options: [undefined, 'none', 'medium', 'large'],
      description: '좌우 여백을 덮어씁니다. none은 좌우 여백만 제거합니다.',
    },
    spacingY: {
      control: 'select',
      options: [undefined, 'none', 'medium', 'large'],
      description: '상하 여백을 덮어씁니다. none은 상하 여백만 제거합니다.',
    },
    children: { control: false },
  },
  args: {
    style: { backgroundColor: '#fff4df' },
    children: (
      <Surface style={{ minHeight: 240, backgroundColor: '#fcfaf7' }}>
        <Title level="main">메인 콘텐츠</Title>
        <Text as="p" size="medium" color="secondary">
          바깥쪽 노란 영역이 ContentArea의 여백입니다. Controls에서 축별 여백을 비교해 보세요.
        </Text>
      </Surface>
    ),
  },
} satisfies Meta<typeof ContentArea>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {
  args: { spacing: 'medium' },
};

export const Large: Story = {
  args: { spacing: 'large' },
};

export const None: Story = {
  args: { spacing: 'none' },
};

export const WithoutSpacing: Story = {};

export const HorizontalOnly: Story = {
  args: { spacingX: 'medium' },
};

export const VerticalOnly: Story = {
  args: { spacingY: 'large' },
};

export const AxisOverrides: Story = {
  args: {
    spacing: 'medium',
    spacingX: 'large',
    spacingY: 'large',
  },
};

export const WithoutVerticalSpacing: Story = {
  args: { spacing: 'large', spacingY: 'none' },
};

export const WithoutHorizontalSpacing: Story = {
  args: {
    spacing: 'large',
    spacingX: 'none',
  },
};
