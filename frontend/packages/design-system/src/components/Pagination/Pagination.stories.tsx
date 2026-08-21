import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { useArgs } from 'storybook/preview-api';

import { Pagination } from './';

const meta = {
  title: 'Pagination/Pagination',
  component: Pagination,
  args: {
    defaultPage: 1,
    totalPages: 10,
  },
} satisfies Meta<typeof Pagination>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const SinglePage: Story = {
  args: {
    defaultPage: 1,
    totalPages: 1,
  },
};

export const ShowAllPages: Story = {
  args: {
    defaultPage: 4,
    totalPages: 7,
  },
};

export const NearStart: Story = {
  args: {
    defaultPage: 3,
    totalPages: 20,
  },
};

export const Middle: Story = {
  args: {
    defaultPage: 10,
    totalPages: 20,
  },
};

export const NearEnd: Story = {
  args: {
    defaultPage: 18,
    totalPages: 20,
  },
};

export const LastPage: Story = {
  args: {
    defaultPage: 20,
    totalPages: 20,
  },
};

export const Controlled: Story = {
  args: {
    defaultPage: 10,
    totalPages: 20,
  },
  render: function Render(args) {
    const [{ defaultPage }, updateArgs] = useArgs();

    return (
      <Pagination
        {...args}
        defaultPage={defaultPage}
        onChange={(page) => updateArgs({ defaultPage: page })}
      />
    );
  },
};
