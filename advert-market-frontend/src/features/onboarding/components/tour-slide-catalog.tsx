import { Button, Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useOnboardingStore } from '@/features/onboarding';
import { LockIcon, NewspaperIcon } from '@/shared/ui/icons';
import { MockupContainer } from './mockup-container';
import { MockupSearchBar } from './mockup-search-bar';
import { MockupTextButton } from './mockup-text-button';
import { TaskHint } from './task-hint';

export function TourSlideCatalog() {
  const { t } = useTranslation();
  const [showDetail, setShowDetail] = useState(false);
  const { completeTourTask } = useOnboardingStore();

  function handleChannelClick() {
    setShowDetail(true);
    completeTourTask(0);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <Text type="title2" weight="bold" align="center">
        {t('onboarding.tour.slide1.title')}
      </Text>
      <Text type="caption1" color="secondary" align="center">
        {t('onboarding.tour.slide1.hint')}
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
                <div
                  style={{
                    padding: 2,
                    borderRadius: 16,
                    background: 'var(--am-soft-accent-bg)',
                  }}
                >
                  <GroupItem
                    text={t('onboarding.tour.mockup.channelName1')}
                    description={`125K ${t('catalog.channel.subs')}`}
                    after={
                      <Text type="callout" weight="bold" color="accent">
                        <span className="tabular-nums">5 TON</span>
                      </Text>
                    }
                    chevron
                    onClick={handleChannelClick}
                  />
                </div>
                <div style={{ opacity: 0.5, pointerEvents: 'none' }}>
                  <GroupItem
                    text={t('onboarding.tour.mockup.channelName2')}
                    description={`89K ${t('catalog.channel.subs')}`}
                    after={
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Text type="callout" weight="bold" color="accent">
                          <span className="tabular-nums">3 TON</span>
                        </Text>
                        <LockIcon size={16} style={{ color: 'var(--color-foreground-tertiary)' }} />
                      </div>
                    }
                  />
                </div>
                <div style={{ opacity: 0.5, pointerEvents: 'none' }}>
                  <GroupItem
                    text={t('onboarding.tour.mockup.channelName3')}
                    description={`67K ${t('catalog.channel.subs')}`}
                    after={
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Text type="callout" weight="bold" color="accent">
                          <span className="tabular-nums">4.5 TON</span>
                        </Text>
                        <LockIcon size={16} style={{ color: 'var(--color-foreground-tertiary)' }} />
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
                      style={{
                        width: '36px',
                        height: '36px',
                        borderRadius: '50%',
                        backgroundColor: 'var(--color-background-secondary)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                      }}
                    >
                      <NewspaperIcon style={{ width: 20, height: 20, color: 'var(--color-foreground-secondary)' }} />
                    </div>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.postPrice')}
                  after={
                    <Text type="callout" weight="bold" color="accent">
                      <span className="tabular-nums">5 TON</span>
                    </Text>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.topics')}
                  after={
                    <Text type="callout" weight="bold" color="primary">
                      {t('onboarding.tour.mockup.topicValues')}
                    </Text>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.avgReach')}
                  after={<Text type="body">{t('onboarding.tour.mockup.reach')}</Text>}
                />
              </Group>
              <div style={{ marginTop: '8px' }}>
                <Button text={t('onboarding.tour.mockup.createDeal')} type="primary" disabled />
              </div>
              <output aria-live="polite" style={{ display: 'block', textAlign: 'center', marginTop: '8px' }}>
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
