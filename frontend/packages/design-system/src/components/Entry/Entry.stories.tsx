import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Entry } from './';

import { Avatar } from '../Avatar';
import DummyImgAvatar from '../Avatar/imgs/dummy-avatar.png';
import { Shell } from '../Shell';
import { Note } from '../Note';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Entry/Entry',
  component: Entry,
} satisfies Meta<typeof Entry>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args

export const Example: Story = {
  args: {
    children: (
      <>
        <Entry.Main>
          <Entry.Header>Header</Entry.Header>
          <Entry.Body>Body</Entry.Body>
          <Entry.Footer>Footer</Entry.Footer>
        </Entry.Main>
        <Entry.Extension>Extension</Entry.Extension>
      </>
    ),
  },
};

export const ExampleReview: Story = {
  args: {
    children: (
      <>
        <Entry.Main>
          <Entry.Header>
            <Shell>
              <Shell.Leading>
                <Avatar img={DummyImgAvatar} />
              </Shell.Leading>
              <Shell.Content title="title" content="content" />
              <Shell.Trailing>Trailing</Shell.Trailing>
            </Shell>
          </Entry.Header>
          <Entry.Body>
            Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
            nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
            accusantium magni quis voluptates, velit nisi dolorum id.
            <Note>
              Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
              nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
              accusantium magni quis voluptates, velit nisi dolorum id.
            </Note>
          </Entry.Body>
          <Entry.Footer>Footer</Entry.Footer>
        </Entry.Main>
        <Entry.Extension>
          <Shell>
            <Shell.Leading>
              <Avatar img={DummyImgAvatar} size="small" />
            </Shell.Leading>
            <Shell.Content title="title" content="content" />
          </Shell>
          <Shell>
            <Shell.Leading>
              <Avatar img={DummyImgAvatar} size="small" />
            </Shell.Leading>
            <Shell.Content title="title" content="content" />
          </Shell>
        </Entry.Extension>
      </>
    ),
  },
};

export const VariantSubtle: Story = {
  args: {
    variant: 'subtle',
    children: (
      <>
        <Entry.Main>
          <Entry.Header>Header</Entry.Header>
          <Entry.Body>Body</Entry.Body>
          <Entry.Footer>Footer</Entry.Footer>
        </Entry.Main>
        <Entry.Extension>Extension</Entry.Extension>
      </>
    ),
  },
};
