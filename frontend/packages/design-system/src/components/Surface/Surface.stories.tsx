import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Surface } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Surface/Surface',
  component: Surface,
} satisfies Meta<typeof Surface>;

export default meta;
type Story = StoryObj<typeof meta>;

const ExampleContent = () => (
  <div>
    <strong>Lorem ipsum</strong>
    <p style={{ margin: '8px 0 0' }}>
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer vitae justo vel massa
      facilisis posuere.
    </p>
  </div>
);

export const Filled: Story = {
  args: {
    children: <ExampleContent />,
    variant: 'filled',
  },
};

export const Outlined: Story = {
  args: {
    children: <ExampleContent />,
    variant: 'outlined',
  },
};

export const Dashed: Story = {
  args: {
    children: <ExampleContent />,
    variant: 'dashed',
  },
};

export const Spacing: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: 16 }}>
      <Surface spacing="small">
        <ExampleContent />
      </Surface>
      <Surface spacing="medium">
        <ExampleContent />
      </Surface>
    </div>
  ),
};
