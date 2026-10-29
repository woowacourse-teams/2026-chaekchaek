import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useState } from 'react';

import { Tabs } from './';
import type { OwnProps } from './Tabs.types';

const items = [
  { value: 'all', text: '전체', panel: '전체 panel' },
  { value: 'reading', text: '읽고 있는 책', panel: '읽고 있는 책 panel' },
  { value: 'finished', text: '다 읽은 책', panel: '다 읽은 책 panel' },
];

const meta = {
  title: 'Tabs/Tabs',
  component: Tabs,
  args: {
    distribution: 'equal',
    items,
    active: 'all',
    onChange: () => {},
  },
  render: function InteractiveTabs(args) {
    const [active, setActive] = useState<OwnProps['active']>(args.active);

    return (
      <Tabs
        {...args}
        active={active}
        onChange={(nextActive) => {
          setActive(nextActive);
          args.onChange(nextActive);
        }}
      />
    );
  },
} satisfies Meta<typeof Tabs>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {};

export const Content: Story = {
  args: {
    distribution: 'content',
  },
};
