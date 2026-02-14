import { Group, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { type CSSProperties, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { CreativeListItem, useCreatives } from '@/features/creatives';
import { BackButtonHandler, EmptyState, EndOfList, Tappable } from '@/shared/ui';
import { fadeIn, listItem, pressScale, scaleIn, staggerChildren } from '@/shared/ui/animations';
import { PaletteIcon } from '@/shared/ui/icons';

const addButtonStyle: CSSProperties = {
  width: 36,
  height: 36,
  borderRadius: 10,
  border: '1px solid var(--color-border-separator)',
  background: 'var(--color-background-secondary)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'pointer',
  padding: 0,
  fontSize: 20,
  color: 'var(--color-accent-primary)',
};

export default function CreativesPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data, isLoading, hasNextPage, fetchNextPage, isFetchingNextPage } = useCreatives();

  const allCreatives = data?.pages.flatMap((p) => p.items) ?? [];

  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      const el = e.currentTarget;
      if (el.scrollTop + el.clientHeight >= el.scrollHeight - 100 && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    },
    [hasNextPage, isFetchingNextPage, fetchNextPage],
  );

  return (
    <div style={{ padding: 16 }} onScroll={handleScroll}>
      <BackButtonHandler />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Text type="title1" weight="bold">
          {t('creatives.title')}
        </Text>
        <motion.div {...pressScale}>
          <Tappable onClick={() => navigate('/profile/creatives/new')} style={addButtonStyle}>
            +
          </Tappable>
        </motion.div>
      </div>

      <AnimatePresence mode="wait">
        {isLoading ? (
          <motion.div key="skeleton" {...fadeIn}>
            <Group skeleton={{ show: true }}>
              <div key="skeleton-a" style={{ height: 56 }} />
              <div key="skeleton-b" style={{ height: 56 }} />
              <div key="skeleton-c" style={{ height: 56 }} />
            </Group>
          </motion.div>
        ) : allCreatives.length === 0 ? (
          <motion.div key="empty" {...scaleIn}>
            <EmptyState
              icon={<PaletteIcon style={{ width: 28, height: 28, color: 'var(--color-foreground-tertiary)' }} />}
              title={t('creatives.empty.title')}
              description={t('creatives.empty.description')}
              actionLabel={t('creatives.empty.cta')}
              onAction={() => navigate('/profile/creatives/new')}
            />
          </motion.div>
        ) : (
          <motion.div key="list" {...staggerChildren} initial="initial" animate="animate">
            <Group>
              {allCreatives.map((creative) => (
                <motion.div key={creative.id} {...listItem}>
                  <CreativeListItem
                    creative={creative}
                    onClick={() => navigate(`/profile/creatives/${creative.id}/edit`)}
                  />
                </motion.div>
              ))}
            </Group>
            {!hasNextPage && allCreatives.length > 0 && <EndOfList label={t('creatives.endOfList')} />}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
