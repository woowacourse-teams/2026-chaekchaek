import type { Meta, StoryObj } from '@storybook/react-webpack5';

import * as Icons from './';

const meta = {
  title: 'Icon/Icon',
  component: Icons.SearchIcon, // 대표 컴포넌트
  args: {
    size: 'medium',
    color: 'default',
  },
  argTypes: {
    size: {
      control: 'radio',
      options: ['small', 'medium', 'large'],
    },
    color: {
      control: 'radio',
      options: ['default', 'secondary', 'error', 'inverse'],
    },
  },
} satisfies Meta<typeof Icons.SearchIcon>;

export default meta;

type Story = StoryObj<typeof meta>;

const render: Story['render'] = (args) => (
  <div
    style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))',
      gap: 16,
    }}
  >
    {Object.entries(Icons).map(([name, Icon]) => (
      <div
        key={name}
        style={{
          display: 'flex',
          minHeight: 80,
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 12,
          border: '1px solid #e5e5e5',
          borderRadius: 8,
        }}
      >
        <Icon {...args} />
        <span style={{ fontSize: 12 }}>{name}</span>
      </div>
    ))}
  </div>
);

export const Gallery: Story = {
  render,
};

export const SmallSize: Story = {
  args: {
    size: 'small',
  },
  render,
};

export const LargeSize: Story = {
  args: {
    size: 'large',
  },
  render,
};

export const DefaultColor: Story = {
  args: {
    color: 'default',
  },
  render,
};

export const SecondaryColor: Story = {
  args: {
    color: 'secondary',
  },
  render,
};

export const ErrorColor: Story = {
  args: {
    color: 'error',
  },
  render,
};

export const InverseColor: Story = {
  args: {
    color: 'inverse',
  },
  decorators: [
    (Story) => (
      <div style={{ padding: 16, backgroundColor: '#1a1a1a', color: '#ffffff' }}>
        <Story />
      </div>
    ),
  ],
  render,
};
