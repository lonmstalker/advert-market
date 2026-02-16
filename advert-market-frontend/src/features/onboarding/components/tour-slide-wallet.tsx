import { Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useOnboardingStore } from '@/features/onboarding';
import type { OnboardingPrimaryRole } from '@/features/onboarding/store/onboarding-store';
import { useHaptic } from '@/shared/hooks';
import { trackOnboardingEvent } from '@/shared/lib/onboarding-analytics';
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
    <div className="am-onboarding-escrow-step">
      <div className="am-onboarding-escrow-step__rail">
        <div aria-hidden="true" className="am-onboarding-escrow-step__icon">
          {icon}
        </div>
        {showLine && <div className="am-onboarding-escrow-step__line" />}
      </div>
      <div className="am-onboarding-escrow-step__copy">
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

type TourSlideWalletProps = {
  primaryRole: OnboardingPrimaryRole;
};

export function TourSlideWallet({ primaryRole }: TourSlideWalletProps) {
  const { t } = useTranslation();
  const haptic = useHaptic();
  const [view, setView] = useState<WalletView>('details');
  const { completeTourTask, getTaskState } = useOnboardingStore();
  const isOwnerPrimary = primaryRole === 'owner';

  function handleEscrowClick() {
    const alreadyCompleted = getTaskState(2) === 'completed';
    haptic.selectionChanged();
    setView('flow');
    completeTourTask(2);
    if (!alreadyCompleted) {
      trackOnboardingEvent('tour_task_complete', { task: 'open_escrow' });
    }
  }

  return (
    <div className="am-onboarding-tour-slide">
      <Text type="title2" weight="bold" align="center">
        {isOwnerPrimary ? t('onboarding.tour.slide3.titleOwner') : t('onboarding.tour.slide3.titleAdvertiser')}
      </Text>
      <Text type="caption1" color="secondary" align="center">
        {isOwnerPrimary ? t('onboarding.tour.slide3.hintOwner') : t('onboarding.tour.slide3.hintAdvertiser')}
      </Text>

      <MockupContainer>
        <AnimatePresence mode="wait">
          {view === 'details' && (
            <motion.div key="details" exit={{ opacity: 0, scale: 0.95 }} transition={{ duration: 0.2 }}>
              <ChannelHeader
                icon={<NewspaperIcon className="am-onboarding-icon-sm am-onboarding-icon-secondary" />}
                name={t('onboarding.tour.mockup.channelName1')}
                detail="5.00 TON"
              />

              <Group header={t('onboarding.tour.mockup.paymentStatus')}>
                <GroupItem
                  text={t('onboarding.tour.mockup.escrow')}
                  description={t('onboarding.tour.mockup.fundsSecured')}
                  before={<LockIcon className="am-onboarding-icon-md am-onboarding-icon-accent" />}
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
              <div className="am-onboarding-escrow-flow">
                <EscrowFlowStep
                  icon={<CoinIcon className="am-onboarding-icon-sm am-onboarding-icon-secondary" />}
                  title={t('onboarding.tour.mockup.escrowFlow1Title')}
                  subtitle={t('onboarding.tour.mockup.escrowFlow1Desc')}
                  showLine
                />
                <EscrowFlowStep
                  icon={<LockIcon className="am-onboarding-icon-sm am-onboarding-icon-secondary" />}
                  title={t('onboarding.tour.mockup.escrowFlow2Title')}
                  subtitle={t('onboarding.tour.mockup.escrowFlow2Desc')}
                  showLine
                />
                <EscrowFlowStep
                  icon={<CheckCircleIcon className="am-onboarding-icon-sm am-onboarding-icon-secondary" />}
                  title={t('onboarding.tour.mockup.escrowFlow3Title')}
                  subtitle={t('onboarding.tour.mockup.escrowFlow3Desc')}
                  showLine={false}
                />
              </div>

              <output aria-live="polite" className="am-onboarding-tour-slide__status">
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

              <div className="am-onboarding-tour-slide__policy-note">
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
