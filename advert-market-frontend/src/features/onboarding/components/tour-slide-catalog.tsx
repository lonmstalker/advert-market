import { Button, Group, GroupItem, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useOnboardingStore } from '@/features/onboarding';
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
                <GroupItem
                  text="Crypto News Daily"
                  description={t('onboarding.tour.mockup.subscribers', { count: '125K' })}
                  after={
                    <Text type="caption1" color="accent">
                      50 TON
                    </Text>
                  }
                  chevron
                  onClick={handleChannelClick}
                />
                <GroupItem
                  text="Tech Digest"
                  description={t('onboarding.tour.mockup.subscribers', { count: '89K' })}
                  after={
                    <Text type="caption1" color="accent">
                      30 TON
                    </Text>
                  }
                  chevron
                />
                <GroupItem
                  text="AI Weekly"
                  description={t('onboarding.tour.mockup.subscribers', { count: '67K' })}
                  after={
                    <Text type="caption1" color="accent">
                      25 TON
                    </Text>
                  }
                  chevron
                />
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
                  text="Crypto News Daily"
                  description={t('onboarding.tour.mockup.subscribers', { count: '125K' })}
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
                        fontSize: '18px',
                      }}
                    >
                      📰
                    </div>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.postPrice')}
                  after={
                    <Text type="body" color="accent">
                      50 TON
                    </Text>
                  }
                />
                <GroupItem
                  text={t('onboarding.tour.mockup.topics')}
                  after={
                    <Text type="caption1" color="secondary">
                      {t('onboarding.tour.mockup.topicValues')}
                    </Text>
                  }
                />
                <GroupItem text={t('onboarding.tour.mockup.avgReach')} after={<Text type="body">15K</Text>} />
              </Group>
              <div style={{ marginTop: '8px' }}>
                <Button text={t('onboarding.tour.mockup.createDeal')} type="primary" disabled />
              </div>
              <div style={{ textAlign: 'center', marginTop: '8px' }}>
                <Text type="caption1" color="accent">
                  {t('onboarding.tour.slide1.taskDone')}
                </Text>
              </div>
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
