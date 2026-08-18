import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Button } from '../Button';

import { Banner } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Banner/Banner',
  component: Banner,
} satisfies Meta<typeof Banner>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {},
  render: (args) => (
    <Banner {...args}>
      <Banner.Content
        title="Title"
        content="lorem ipsum dolor sit amet consectetur adipisicing elit."
      />
      <Banner.Trailing>
        <Button size="small" variant="primary">
          Button
        </Button>
      </Banner.Trailing>
    </Banner>
  ),
};
