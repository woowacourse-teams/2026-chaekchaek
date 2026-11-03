import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';

import { Accordion } from './';
import type { Props } from './';

const meta = {
  title: 'Accordion/Accordion',
  component: Accordion,
  decorators: [
    (Story) => (
      <div style={{ width: 342, maxWidth: '100%' }}>
        <Story />
      </div>
    ),
  ],
  render: function Render(args) {
    const [, updateArgs] = useArgs<Props>();

    return (
      <Accordion
        {...args}
        onChange={(value) => {
          args.onChange(value);
          updateArgs({ value });
        }}
      />
    );
  },
  args: {
    value: [],
    items: [
      { value: 'first', text: '첫 번째 항목', panel: '첫 번째 항목의 내용입니다.' },
      { value: 'second', text: '두 번째 항목', panel: '두 번째 항목의 내용입니다.' },
      { value: 'third', text: '세 번째 항목', panel: '세 번째 항목의 내용입니다.' },
    ],
    onChange: () => {},
  },
} satisfies Meta<typeof Accordion>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Example: Story = {
  args: {
    value: ['first'],
  },
};

export const Open: Story = {
  args: {
    value: ['first'],
  },
};

export const Closed: Story = {
  args: {
    value: [],
  },
};
