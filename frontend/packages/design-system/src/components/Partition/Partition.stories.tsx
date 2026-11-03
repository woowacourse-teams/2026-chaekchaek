import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Partition } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Partition/Partition',
  component: Partition,
  argTypes: {
    columns: { control: { type: 'number', min: 1, step: 1 } },
  },
} satisfies Meta<typeof Partition>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: (
      <>
        <Partition.Item>Test</Partition.Item>
        <Partition.Item>
          Lorem ipsum dolor sit amet consectetur adipisicing elit. Esse, alias quisquam ipsa
          voluptatibus consequuntur quidem soluta? Tempore nesciunt sit rem, voluptate quod,
          maiores, placeat recusandae consequatur ut inventore nostrum facilis!
        </Partition.Item>
      </>
    ),
  },
};

export const TwoColumns: Story = {
  args: {
    columns: 2,
    children: Array.from({ length: 6 }, (_, index) => (
      <Partition.Item key={index} style={{ padding: 16, border: '1px solid currentColor' }}>
        Column item {index + 1}
      </Partition.Item>
    )),
  },
};

export const ThreeColumns: Story = {
  args: {
    ...TwoColumns.args,
    columns: 3,
  },
};
