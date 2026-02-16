import { Button, Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useOnboardingStore } from '@/features/onboarding';
import type { OnboardingPrimaryRole } from '@/features/onboarding/store/onboarding-store';
import { useHaptic } from '@/shared/hooks';
import { trackOnboardingEvent } from '@/shared/lib/onboarding-analytics';
import { NewspaperIcon } from '@/shared/ui/icons';
import { MockupContainer } from './mockup-container';
import { MockupSearchBar } from './mockup-search-bar';
import { MockupTextButton } from './mockup-text-button';
import { TaskHint } from './task-hint';

type TourSlideCatalogProps = {
  primaryRole: OnboardingPrimaryRole;
};

export function TourSlideCatalog({ primaryRole }: TourSlideCatalogProps) {
  const { t } = useTranslation();
  const haptic = useHaptic();
  const [showDetail, setShowDetail] = useState(false);
  const { completeTourTask, getTaskState } = useOnboardingStore();
  const isOwnerPrimary = primaryRole === 'owner';

  function handleChannelClick() {
    const alreadyCompleted = getTaskState(0) === 'completed';
    haptic.selectionChanged();
    setShowDetail(true);
    completeTourTask(0);
    if (!alreadyCompleted) {
      trackOnboardingEvent('tour_task_complete', { task: 'open_channel_detail' });
    }
  }

  return (
    <div className="am-onboarding-tour-slide">
      <Text type="title2" weight="bold" align="center">
        {isOwnerPrimary ? t('onboarding.tour.slide1.titleOwner') : t('onboarding.tour.slide1.titleAdvertiser')}
      </Text>
      <Text type="caption1" color="secondary" align="center">
        {isOwnerPrimary ? t('onboarding.tour.slide1.hintOwner') : t('onboarding.tour.slide1.hintAdvertiser')}
      </Text>

      <MockupContainer>
        <AnimatePresence mode="wait">
          {!showDetail ? (
            <motion.div
              key="list"
              initial={{ opacity: 1 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.2 }}
            >
              <MockupSearchBar placeholder={t('onboarding.tour.mockup.searchPlaceholder')} />
              <Group header={t('onboarding.tour.mockup.popular')}>
                <div className="p-0.5 rounded-[16px] bg-soft-accent">
                  <GroupItem
                    text={t('onboarding.tour.mockup.channelName1')}
                    description={`125K ${t('catalog.channel.subs')}`}
                    after={
                      <Text type="body" weight="bold" color="accent">
                        <span className="tabular-nums">5 TON</span>
                      </Text>
                    }
                    chevron
                    onClick={handleChannelClick}
                  />
                </div>
                <div className="opacity-40 pointer-events-none grayscale-[0.5]">
                  <GroupItem
                    text={t('onboarding.tour.mockup.channelName2')}
                    description={`89K ${t('catalog.channel.subs')}`}
                    after={
                      <div className="flex items-center gap-1.5">
                        <Text type="body" weight="bold" color="accent">
                          <span className="tabular-nums">3 TON</span>
                        </Text>
                        <span aria-hidden="true" className="text-sm">
                          🔐
                        </span>
                      </div>
                    }
                  />
                </div>
                <div className="opacity-40 pointer-events-none grayscale-[0.5]">
                  <GroupItem
                    text={t('onboarding.tour.mockup.channelName3')}
                    description={`67K ${t('catalog.channel.subs')}`}
                    after={
                      <div className="flex items-center gap-1.5">
                        <Text type="body" weight="bold" color="accent">
                          <span className="tabular-nums">4.5 TON</span>
                        </Text>
                        <span aria-hidden="true" className="text-sm">
                          🔐
                        </span>
                      </div>
                    }
                  />
                </div>
              </Group>
            </motion.div>
          ) : (
            <motion.div
              key="detail"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              transition={{ duration: 0.2 }}
            >
              <Group>
                <GroupItem
                  text={t('onboarding.tour.mockup.channelName1')}
                  description={`125K ${t('catalog.channel.subs')}`}
                  before={
                    <div
                      aria-hidden="true"
                      className="size-9 rounded-full bg-bg-secondary flex items-center justify-center"
                    >
                      <NewspaperIcon className="w-5 h-5 text-fg-secondary" />
                    </div>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.postPrice')}
                  after={
                    <Text type="body" weight="bold" color="accent">
                      <span className="tabular-nums">5 TON</span>
                    </Text>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.topics')}
                  after={
                    <Text type="body" weight="bold">
                      {t('onboarding.tour.mockup.topicValues')}
                    </Text>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.avgReach')}
                  after={<Text type="body">{t('onboarding.tour.mockup.reach')}</Text>}
                />
              </Group>
              <div className="mt-2">
                <Button text={t('onboarding.tour.mockup.createDeal')} type="primary" disabled />
              </div>
              <output aria-live="polite" className="block text-center mt-2">
                <Text type="caption1" color="accent">
                  {t('onboarding.tour.slide1.taskDone')}
                </Text>
              </output>
              <MockupTextButton text={t('onboarding.tour.mockup.backToList')} onClick={() => setShowDetail(false)} />
            </motion.div>
          )}
        </AnimatePresence>
      </MockupContainer>

      <AnimatePresence>
        {!showDetail && (
          <motion.div exit={{ opacity: 0 }}>
            <TaskHint text={t('onboarding.tour.slide1.taskHint')} />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
