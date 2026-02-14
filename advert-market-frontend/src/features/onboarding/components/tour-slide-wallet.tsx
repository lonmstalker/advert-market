import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useOnboardingStore } from '@/features/onboarding';
import { CheckCircleIcon, CoinIcon, LockIcon, NewspaperIcon } from '@/shared/ui/icons';
import { ChannelHeader } from './channel-header';
import { MockupContainer } from './mockup-container';
import { MockupTextButton } from './mockup-text-button';
import { TaskHint } from './task-hint';

type WalletView = 'details' | 'flow' | 'policy';

function EscrowFlowStep({
  icon,
  title,
  subtitle,
  showLine,
}: {
  icon: ReactNode;
  title: string;
  subtitle: string;
  showLine: boolean;
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'stretch' }}>
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          width: '40px',
          flexShrink: 0,
        }}
      >
        <div
          aria-hidden="true"
          style={{
            width: '32px',
            height: '32px',
            borderRadius: '50%',
            backgroundColor: 'var(--color-background-secondary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {icon}
        </div>
        {showLine && (
          <div
            style={{
              width: '1.5px',
              flex: 1,
              minHeight: '8px',
              backgroundColor: 'var(--color-accent-primary)',
            }}
          />
        )}
      </div>
      <div style={{ flex: 1, padding: '4px 0 12px 8px' }}>
        <Text type="subheadline2" weight="medium">
          {title}
        </Text>
        <Text type="caption1" color="secondary">
          {subtitle}
        </Text>
      </div>
    </div>
  );
}

const flowIconStyle = { width: 16, height: 16, color: 'var(--color-foreground-secondary)' };

export function TourSlideWallet() {
  const { t } = useTranslation();
  const [view, setView] = useState<WalletView>('details');
  const { completeTourTask } = useOnboardingStore();

  function handleEscrowClick() {
    setView('flow');
    completeTourTask(2);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <Text type="title2" weight="bold" align="center">
        {t('onboarding.tour.slide3.title')}
      </Text>
      <Text type="caption1" color="secondary" align="center">
        {t('onboarding.tour.slide3.hint')}
      </Text>

      <MockupContainer>
        <AnimatePresence mode="wait">
          {view === 'details' && (
            <motion.div key="details" exit={{ opacity: 0, scale: 0.95 }} transition={{ duration: 0.2 }}>
              <ChannelHeader
                icon={<NewspaperIcon style={{ width: 18, height: 18, color: 'var(--color-foreground-secondary)' }} />}
                name={t('onboarding.tour.mockup.channelName1')}
                detail="5.00 TON"
              />

              <Group header={t('onboarding.tour.mockup.paymentStatus')}>
                <GroupItem
                  text={t('onboarding.tour.mockup.escrow')}
                  description={t('onboarding.tour.mockup.fundsSecured')}
                  before={<LockIcon style={{ width: 20, height: 20, color: 'var(--color-accent-primary)' }} />}
                  onClick={handleEscrowClick}
                  chevron
                />
                <GroupItem text={t('onboarding.tour.mockup.dealAmount')} after={<Text type="body">5.00 TON</Text>} />
                <GroupItem
                  text={t('onboarding.tour.mockup.platformFee')}
                  after={
                    <Text type="body" color="secondary">
                      0.25 TON
                    </Text>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.ownerPayout')}
                  after={
                    <Text type="body" color="accent">
                      4.75 TON
                    </Text>
                  }
                />
              </Group>
            </motion.div>
          )}

          {view === 'flow' && (
            <motion.div
              key="flow"
              initial={{ opacity: 0, scale: 1.02 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
            >
              <div style={{ padding: '8px 0' }}>
                <EscrowFlowStep
                  icon={<CoinIcon style={flowIconStyle} />}
                  title={t('onboarding.tour.mockup.escrowFlow1Title')}
                  subtitle={t('onboarding.tour.mockup.escrowFlow1Desc')}
                  showLine
                />
                <EscrowFlowStep
                  icon={<LockIcon style={flowIconStyle} />}
                  title={t('onboarding.tour.mockup.escrowFlow2Title')}
                  subtitle={t('onboarding.tour.mockup.escrowFlow2Desc')}
                  showLine
                />
                <EscrowFlowStep
                  icon={<CheckCircleIcon style={flowIconStyle} />}
                  title={t('onboarding.tour.mockup.escrowFlow3Title')}
                  subtitle={t('onboarding.tour.mockup.escrowFlow3Desc')}
                  showLine={false}
                />
              </div>

              <output aria-live="polite" style={{ display: 'block', textAlign: 'center', marginTop: '4px' }}>
                <Text type="caption1" color="accent">
                  {t('onboarding.tour.slide3.taskDone')}
                </Text>
              </output>

              <MockupTextButton
                text={t('onboarding.tour.policy.link')}
                color="accent"
                onClick={() => setView('policy')}
              />
              <MockupTextButton text={t('onboarding.tour.mockup.backToDetails')} onClick={() => setView('details')} />
            </motion.div>
          )}

          {view === 'policy' && (
            <motion.div
              key="policy"
              initial={{ opacity: 0, scale: 1.02 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
            >
              <Group header={t('onboarding.tour.policy.title')}>
                <GroupItem
                  text={t('onboarding.tour.policy.tier1')}
                  description={t('onboarding.tour.policy.tier1Desc')}
                />
                <GroupItem
                  text={t('onboarding.tour.policy.tier2')}
                  description={t('onboarding.tour.policy.tier2Desc')}
                />
                <GroupItem
                  text={t('onboarding.tour.policy.tier3')}
                  description={t('onboarding.tour.policy.tier3Desc')}
                />
              </Group>

              <div style={{ textAlign: 'center', padding: '4px 0' }}>
                <Text type="caption1" color="secondary">
                  {t('onboarding.tour.policy.configNote')}
                </Text>
              </div>

              <MockupTextButton text={t('onboarding.tour.policy.back')} onClick={() => setView('flow')} />
            </motion.div>
          )}
        </AnimatePresence>
      </MockupContainer>

      <AnimatePresence>
        {view === 'details' && (
          <motion.div exit={{ opacity: 0 }}>
            <TaskHint text={t('onboarding.tour.slide3.taskHint')} />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
