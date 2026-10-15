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

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  render: () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', gap: 16 }}>
        <Surface variant="filled">
          <ExampleContent />
        </Surface>
        <Surface variant="outlined">
          <ExampleContent />
        </Surface>
      </div>
      <div style={{ display: 'flex', gap: 16 }}>
        <Surface spacing="small">
          <ExampleContent />
        </Surface>
        <Surface spacing="medium">
          <ExampleContent />
        </Surface>
      </div>
    </div>
  ),
};
