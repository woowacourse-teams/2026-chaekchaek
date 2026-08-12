import type { Meta, StoryObj } from '@storybook/react-webpack5';

import { Dialog } from './';

import { Field } from '../Field';
import { Input } from '../Input';
import { Button } from '../Button';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories#default-export
const meta = {
  title: 'Dialog/Dialog',
  component: Dialog,
} satisfies Meta<typeof Dialog>;

export default meta;
type Story = StoryObj<typeof meta>;

// More on writing stories with args: https://storybook.js.org/docs/writing-stories/args
export const Example: Story = {
  args: {
    children: (
      <>
        <Dialog.Container>
          <Dialog.Header subTitle="lorem ipsum dolor sit amet consectetur adipisicing elit.">
            Header
          </Dialog.Header>
          <Dialog.Body>
            Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
            nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
            accusantium magni quis voluptates, velit nisi dolorum id.{' '}
          </Dialog.Body>
          <Dialog.Footer>Footer</Dialog.Footer>
        </Dialog.Container>
      </>
    ),
  },
};

export const WithField: Story = {
  args: {
    children: (
      <>
        <Dialog.Container>
          <Dialog.Header subTitle="lorem ipsum dolor sit amet consectetur adipisicing elit.">
            Header
          </Dialog.Header>
          <Dialog.Body>
            <Field>
              <Field.Label>Label</Field.Label>
              <Field.Content>
                <Input block />
              </Field.Content>
              <Field.Description>
                lorem ipsum dolor sit amet consectetur adipisicing elit.
              </Field.Description>
            </Field>
            <Field>
              <Field.Label>Label</Field.Label>
              <Field.Content>
                <Input block />
              </Field.Content>
              <Field.Description>
                lorem ipsum dolor sit amet consectetur adipisicing elit.
              </Field.Description>
            </Field>
          </Dialog.Body>
          <Dialog.Footer>
            <Button block>Button</Button>
          </Dialog.Footer>
        </Dialog.Container>
      </>
    ),
  },
};

export const SizeMedium: Story = {
  args: {
    size: 'medium',
    children: (
      <>
        <Dialog.Container>
          <Dialog.Header>Header</Dialog.Header>
          <Dialog.Body>
            Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
            nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
            accusantium magni quis voluptates, velit nisi dolorum id.{' '}
          </Dialog.Body>
          <Dialog.Footer>Footer</Dialog.Footer>
        </Dialog.Container>
      </>
    ),
  },
};

export const SizeLarge: Story = {
  args: {
    size: 'large',
    children: (
      <>
        <Dialog.Container>
          <Dialog.Header>Header</Dialog.Header>
          <Dialog.Body>
            Lorem ipsum dolor sit amet consectetur adipisicing elit. Tenetur, voluptatum possimus
            nobis quas error consequatur cumque nam recusandae dicta ab commodi, reiciendis
            accusantium magni quis voluptates, velit nisi dolorum id.{' '}
          </Dialog.Body>
          <Dialog.Footer>Footer</Dialog.Footer>
        </Dialog.Container>
      </>
    ),
  },
};
