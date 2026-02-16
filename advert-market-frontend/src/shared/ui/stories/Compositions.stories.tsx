import type { Meta, StoryObj } from '@storybook/react-vite';
import {
  Button,
  DialogModal,
  Group,
  GroupItem,
  Image,
  Input,
  SkeletonElement,
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
    <motion.div {...slideUp} className="w-[360px]">
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
            <Text type="callout" weight="bold" color="accent">
              <span className="tabular-nums">50.00 TON</span>
            </Text>
          }
        />
        <GroupItem
          text="Status"
          after={
            <span
              className="inline-flex items-center px-2.5 py-0.5 rounded-[8px]"
              style={{ backgroundColor: 'var(--am-soft-warning-bg)' }}
            >
              <Text type="caption1" weight="bold">
                <span style={{ color: 'var(--color-state-warning)' }}>Awaiting Payment</span>
              </Text>
            </span>
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
      <div className="flex gap-3 mt-4">
        <motion.div {...pressScale} className="flex-1">
          <Button text="Decline" type="secondary" />
        </motion.div>
        <motion.div {...pressScale} className="flex-1">
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
      { name: 'Crypto News', subs: '125K', avgReach: '38K', price: '50 TON', cpm: '1.32 TON', color: '4A90D9' },
      { name: 'Tech Daily', subs: '89K', avgReach: '27K', price: '30 TON', cpm: '1.11 TON', color: '7B61FF' },
      { name: 'Finance Hub', subs: '210K', avgReach: '62K', price: '80 TON', cpm: '1.29 TON', color: '34C759' },
      { name: 'AI Digest', subs: '67K', avgReach: '19K', price: '25 TON', cpm: '1.31 TON', color: 'FF6B35' },
    ];
    return (
      <motion.div {...staggerChildren} initial="initial" animate="animate" className="w-[360px]">
        <Group header="Top Channels">
          {channels.map((ch) => (
            <motion.div key={ch.name} {...listItem}>
              <GroupItem
                text={ch.name}
                description={`${ch.subs} subscribers · reach ${ch.avgReach}`}
                before={
                  <Image
                    src={`https://placehold.co/40/${ch.color}/white?text=${ch.name[0]}`}
                    width="40px"
                    height="40px"
                    borderRadius="50%"
                  />
                }
                after={
                  <div className="grid gap-0.5 justify-items-end">
                    <Text type="callout" color="accent">
                      <span className="tabular-nums">{ch.price}</span>
                    </Text>
                    <Text type="caption1" color="secondary">
                      <span className="tabular-nums">CPM {ch.cpm}</span>
                    </Text>
                  </div>
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
      <motion.div {...fadeIn} className="w-[360px]">
        <Text type="title2" weight="bold">
          New Deal
        </Text>
        <div className="flex flex-col gap-4 mt-4">
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
      <motion.div {...staggerChildren} initial="initial" animate="animate" className="w-[360px]">
        <motion.div {...listItem}>
          <Group header="Account">
            <GroupItem text="Username" description="@advertiser" chevron />
            <GroupItem text="Wallet" description="UQ...x4Kf" chevron />
          </Group>
        </motion.div>

        <motion.div {...listItem} className="mt-4">
          <Group header="Notifications">
            <GroupItem text="Push Notifications" after={<Toggle isEnabled={push} onChange={setPush} />} />
            <GroupItem text="Sound" after={<Toggle isEnabled={sound} onChange={setSound} />} />
            <GroupItem text="Email Digests" after={<Toggle isEnabled={email} onChange={setEmail} />} />
          </Group>
        </motion.div>

        <motion.div {...listItem} className="mt-4">
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
          onConfirm={() => setConfirm(false)}
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
      <div className="w-[360px]">
        <Button
          text={loading ? 'Show Content' : 'Show Loading'}
          type="secondary"
          onClick={() => setLoading(!loading)}
        />
        <div className="mt-4">
          <AnimatePresence mode="wait">
            {loading ? (
              <motion.div key="loading" {...fadeIn} className="flex flex-col items-center gap-3 py-10">
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
    <motion.div {...scaleIn} className="w-[360px] text-center px-5 py-10">
      <MailboxIcon style={{ width: 48, height: 48, color: 'var(--color-foreground-tertiary)' }} />
      <Text type="title3" weight="bold" style={{ marginTop: '16px' }}>
        No deals yet
      </Text>
      <Text type="body" color="secondary" style={{ marginTop: '8px' }}>
        Create your first advertising deal to get started
      </Text>
      <div className="mt-6">
        <motion.div {...pressScale}>
          <Button text="Browse Channels" type="primary" />
        </motion.div>
      </div>
    </motion.div>
  ),
};

export const SkeletonToContent: Story = {
  name: 'Skeleton → Content',
  render: function Render() {
    const [loading, setLoading] = useState(true);

    return (
      <div className="w-[360px]">
        <Button
          text={loading ? 'Show Content' : 'Show Skeleton'}
          type="secondary"
          onClick={() => setLoading(!loading)}
        />
        <div className="mt-4">
          <AnimatePresence mode="wait">
            {loading ? (
              <motion.div key="skeleton" {...fadeIn} className="flex flex-col gap-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="flex gap-3 items-center">
                    <SkeletonElement style={{ width: 48, height: 48, borderRadius: '50%', flexShrink: 0 }} />
                    <div className="flex flex-col gap-1.5 flex-1">
                      <SkeletonElement style={{ width: '60%', height: 14, borderRadius: 6 }} />
                      <SkeletonElement style={{ width: '80%', height: 12, borderRadius: 6 }} />
                    </div>
                  </div>
                ))}
              </motion.div>
            ) : (
              <motion.div key="content" {...scaleIn}>
                <Group header="Channels">
                  <GroupItem text="Crypto News" description="125K subscribers" chevron />
                  <GroupItem text="Tech Daily" description="89K subscribers" chevron />
                  <GroupItem text="Finance Hub" description="210K subscribers" chevron />
                </Group>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    );
  },
};

export const GlassBlurSurface: Story = {
  name: 'Glass Blur Surface',
  render: () => (
    <div className="w-[360px] relative overflow-hidden rounded-card p-6" style={{ minHeight: 200 }}>
      {/* Background content to show blur effect */}
      <div className="absolute inset-0 flex items-center justify-center">
        <div
          className="w-32 h-32 rounded-full"
          style={{ background: 'var(--color-accent-primary)', opacity: 0.3, filter: 'blur(40px)' }}
        />
      </div>
      {/* Glass card */}
      <div className="am-surface-card relative p-4">
        <Text type="title3" weight="bold">
          Glass Surface
        </Text>
        <Text type="body" color="secondary" style={{ marginTop: 4 }}>
          Cards use blur(12px), chrome uses blur(20px)
        </Text>
      </div>
    </div>
  ),
};

export const FinancialDisplay: Story = {
  name: 'Financial Data Display',
  render: () => (
    <div className="w-[360px] flex flex-col gap-4">
      {/* Hero balance */}
      <div className="am-surface-card p-6 text-center">
        <Text type="caption1" color="secondary" weight="bold">
          Total Balance
        </Text>
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          <Text type="largeTitle" weight="bold">
            <span className="tabular-nums">1 250.50 TON</span>
          </Text>
        </motion.div>
        <Text type="subheadline2" color="tertiary">
          <span className="tabular-nums">≈ $3 751.50</span>
        </Text>
      </div>

      {/* Metric row */}
      <div className="flex overflow-hidden rounded-card" style={{ border: '1px solid var(--am-card-border)' }}>
        <div className="flex-1 text-center py-3.5 px-3" style={{ background: 'var(--am-card-surface)' }}>
          <Text type="title3" weight="bold">
            <span className="tabular-nums">850.00</span>
          </Text>
          <Text type="caption1" color="secondary">
            In Escrow
          </Text>
        </div>
        <div className="self-stretch" style={{ width: 1, background: 'var(--color-border-separator)' }} />
        <div className="flex-1 text-center py-3.5 px-3" style={{ background: 'var(--am-card-surface)' }}>
          <Text type="title3" weight="bold">
            <span className="tabular-nums">12</span>
          </Text>
          <Text type="caption1" color="secondary">
            Active Deals
          </Text>
        </div>
      </div>

      {/* Transaction list items */}
      <Group header="Recent">
        <GroupItem
          text="Escrow Deposit"
          description="Crypto News — Deal #1042"
          after={
            <Text type="callout" weight="bold" color="danger">
              <span className="tabular-nums">-50.00 TON</span>
            </Text>
          }
        />
        <GroupItem
          text="Payout"
          description="Tech Daily — Deal #1038"
          after={
            <Text type="callout" weight="bold" color="accent">
              <span className="tabular-nums">+30.00 TON</span>
            </Text>
          }
        />
      </Group>
    </div>
  ),
};
