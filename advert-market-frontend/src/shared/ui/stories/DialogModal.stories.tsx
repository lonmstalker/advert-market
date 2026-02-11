import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, DialogModal } from '@telegram-tools/ui-kit';
import { useState } from 'react';

const meta: Meta<typeof DialogModal> = {
  title: 'UI Kit/DialogModal',
  component: DialogModal,
  parameters: { layout: 'padded' },
};

export default meta;
type Story = StoryObj<typeof DialogModal>;

export const Confirmation: Story = {
  render: function Render() {
    const [active, setActive] = useState(true);
    return (
      <>
        <Button text="Open Dialog" type="primary" onClick={() => setActive(true)} />
        <DialogModal
          active={active}
          title="Confirm Payment"
          description="Send 50 TON to escrow for deal with Crypto News?"
          confirmText="Confirm"
          closeText="Cancel"
          onConfirm={() => setActive(false)}
          onClose={() => setActive(false)}
        />
      </>
    );
  },
};

export const Destructive: Story = {
  render: function Render() {
    const [active, setActive] = useState(true);
    return (
      <>
        <Button text="Delete Deal" type="secondary" onClick={() => setActive(true)} />
        <DialogModal
          active={active}
          title="Cancel Deal?"
          description="This action cannot be undone. The deal will be permanently cancelled."
          confirmText="Cancel Deal"
          closeText="Keep"
          onDelete={() => setActive(false)}
          onClose={() => setActive(false)}
        />
      </>
    );
  },
};

export const DisputeConfirm: Story = {
  render: function Render() {
    const [active, setActive] = useState(true);
    return (
      <>
        <Button text="Open Dispute" type="secondary" onClick={() => setActive(true)} />
        <DialogModal
          active={active}
          title="Open Dispute?"
          description="A moderator will review the case. Both parties will be notified."
          confirmText="Submit Dispute"
          closeText="Go Back"
          onConfirm={() => setActive(false)}
          onClose={() => setActive(false)}
        />
      </>
    );
  },
};
