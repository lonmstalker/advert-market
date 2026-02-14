import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Button,
  DialogModal,
  Group,
  GroupItem,
  Image,
  Input,
  Spinner,
  Text,
  Toggle,
  useToast,
} from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useState } from 'react';
import { fadeIn, listItem, pressScale, scaleIn, slideUp, staggerChildren } from '../animations';
import { MailboxIcon } from '../icons';

const meta: Meta = {
  title: 'Compositions',
  parameters: { layout: 'padded' },
};

export default meta;
type Story = StoryObj;

export const DealCard: Story = {
  name: 'Deal Card',
  render: () => (
    <motion.div {...slideUp} style={{ width: '360px' }}>
      <Group header="Deal #1042">
        <GroupItem
          text="Channel"
          description="Crypto News Daily"
          before={
            <Image src="https://placehold.co/40/4A90D9/white?text=CN" width="40px" height="40px" borderRadius="50%" />
          }
        />
        <GroupItem
          text="Amount"
          after={
            <Text type="body" weight="bold" color="accent">
              50.00 TON
            </Text>
          }
        />
        <GroupItem
          text="Status"
          after={
            <Text type="body" color="secondary">
              Pending Deposit
            </Text>
          }
        />
        <GroupItem
          text="Post Type"
          after={
            <Text type="body" color="secondary">
              Standard
            </Text>
          }
        />
      </Group>
      <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
        <motion.div {...pressScale} style={{ flex: 1 }}>
          <Button text="Decline" type="secondary" />
        </motion.div>
        <motion.div {...pressScale} style={{ flex: 1 }}>
          <Button text="Accept" type="primary" />
        </motion.div>
      </div>
    </motion.div>
  ),
};

export const ChannelListing: Story = {
  name: 'Channel Listing',
  render: () => {
    const channels = [
      { name: 'Crypto News', subs: '125K', price: '50 TON', color: '4A90D9' },
      { name: 'Tech Daily', subs: '89K', price: '30 TON', color: '7B61FF' },
      { name: 'Finance Hub', subs: '210K', price: '80 TON', color: '34C759' },
      { name: 'AI Digest', subs: '67K', price: '25 TON', color: 'FF6B35' },
    ];
    return (
      <motion.div {...staggerChildren} initial="initial" animate="animate" style={{ width: '360px' }}>
        <Group header="Top Channels">
          {channels.map((ch) => (
            <motion.div key={ch.name} {...listItem}>
              <GroupItem
                text={ch.name}
                description={`${ch.subs} subscribers`}
                before={
                  <Image
                    src={`https://placehold.co/40/${ch.color}/white?text=${ch.name[0]}`}
                    width="40px"
                    height="40px"
                    borderRadius="50%"
                  />
                }
                after={
                  <Text type="callout" color="accent">
                    {ch.price}
                  </Text>
                }
                chevron
              />
            </motion.div>
          ))}
        </Group>
      </motion.div>
    );
  },
};

export const CreateDealForm: Story = {
  name: 'Create Deal Form',
  render: function Render() {
    const [amount, setAmount] = useState('');
    const [message, setMessage] = useState('');
    const [loading, setLoading] = useState(false);
    const { showToast } = useToast();

    const handleSubmit = () => {
      setLoading(true);
      setTimeout(() => {
        setLoading(false);
        showToast('Deal created!', { type: 'success' });
      }, 1500);
    };

    return (
      <motion.div {...fadeIn} style={{ width: '360px' }}>
        <Text type="title2" weight="bold">
          New Deal
        </Text>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '16px' }}>
          <Input value={amount} onChange={setAmount} placeholder="Amount in TON" numeric />
          <Input value={message} onChange={setMessage} placeholder="Message to channel owner" />
          <motion.div {...pressScale}>
            <Button
              text={loading ? 'Creating...' : 'Create Deal'}
              type="primary"
              loading={loading}
              onClick={handleSubmit}
            />
          </motion.div>
        </div>
      </motion.div>
    );
  },
};

export const SettingsPage: Story = {
  name: 'Settings Page',
  render: function Render() {
    const [push, setPush] = useState(true);
    const [sound, setSound] = useState(false);
    const [email, setEmail] = useState(true);
    const [confirm, setConfirm] = useState(false);

    return (
      <motion.div {...staggerChildren} initial="initial" animate="animate" style={{ width: '360px' }}>
        <motion.div {...listItem}>
          <Group header="Account">
            <GroupItem text="Username" description="@advertiser" chevron />
            <GroupItem text="Wallet" description="UQ...x4Kf" chevron />
          </Group>
        </motion.div>

        <motion.div {...listItem} style={{ marginTop: '16px' }}>
          <Group header="Notifications">
            <GroupItem text="Push Notifications" after={<Toggle isEnabled={push} onChange={setPush} />} />
            <GroupItem text="Sound" after={<Toggle isEnabled={sound} onChange={setSound} />} />
            <GroupItem text="Email Digests" after={<Toggle isEnabled={email} onChange={setEmail} />} />
          </Group>
        </motion.div>

        <motion.div {...listItem} style={{ marginTop: '16px' }}>
          <Group header="Danger Zone">
            <GroupItem text="Delete Account" onClick={() => setConfirm(true)} />
          </Group>
        </motion.div>

        <DialogModal
          active={confirm}
          title="Delete Account?"
          description="All your data will be permanently removed."
          confirmText="Delete"
          closeText="Keep"
          onDelete={() => setConfirm(false)}
          onClose={() => setConfirm(false)}
        />
      </motion.div>
    );
  },
};

export const LoadingToContent: Story = {
  name: 'Loading → Content Transition',
  render: function Render() {
    const [loading, setLoading] = useState(true);

    return (
      <div style={{ width: '360px' }}>
        <Button
          text={loading ? 'Show Content' : 'Show Loading'}
          type="secondary"
          onClick={() => setLoading(!loading)}
        />
        <div style={{ marginTop: '16px' }}>
          <AnimatePresence mode="wait">
            {loading ? (
              <motion.div
                key="loading"
                {...fadeIn}
                style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px', padding: '40px' }}
              >
                <Spinner size="40px" color="accent" />
                <Text type="callout" color="secondary">
                  Loading deals...
                </Text>
              </motion.div>
            ) : (
              <motion.div key="content" {...scaleIn}>
                <Group header="My Deals">
                  <GroupItem text="Crypto News — Banner" description="50 TON · Active" chevron />
                  <GroupItem text="Tech Daily — Post" description="30 TON · Pending" chevron />
                  <GroupItem text="Finance Hub — Story" description="20 TON · Completed" />
                </Group>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    );
  },
};

export const EmptyState: Story = {
  name: 'Empty State',
  render: () => (
    <motion.div {...scaleIn} style={{ width: '360px', textAlign: 'center', padding: '40px 20px' }}>
      <MailboxIcon style={{ width: 48, height: 48, color: 'var(--color-foreground-tertiary)' }} />
      <Text type="title3" weight="bold" style={{ marginTop: '16px' }}>
        No deals yet
      </Text>
      <Text type="body" color="secondary" style={{ marginTop: '8px' }}>
        Create your first advertising deal to get started
      </Text>
      <div style={{ marginTop: '24px' }}>
        <motion.div {...pressScale}>
          <Button text="Browse Channels" type="primary" />
        </motion.div>
      </div>
    </motion.div>
  ),
};
