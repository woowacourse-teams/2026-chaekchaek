import { useState } from 'react';
import type { ChangeEvent } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Slider } from './';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Slider/Slider',
  component: Slider,
} satisfies Meta<typeof Slider>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    value: 40,
    max: 100,
    onChange: () => {},
  },
};

export const Disabled: Story = {
  args: {
    value: 60,
    max: 100,
    onChange: () => {},
    disabled: true,
  },
};

export const Controlled: Story = {
  args: {
    value: 40,
    max: 100,
    onChange: () => {},
  },
  render: function Render(args) {
    const [value, setValue] = useState(Number(args.value));

    const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
      args.onChange?.(event);
      setValue(event.currentTarget.valueAsNumber);
    };

    return <Slider {...args} value={value} onChange={handleChange} />;
  },
};
