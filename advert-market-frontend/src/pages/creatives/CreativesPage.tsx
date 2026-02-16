import { Group, GroupItem, Icon, Sheet, Text } from '@telegram-tools/ui-kit';
import { easeOut } from 'motion';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import type { CreativeTemplate } from '@/features/creatives';
import { CreativeListItem, useCreatives } from '@/features/creatives';
import { useHaptic } from '@/shared/hooks/use-haptic';
import { BackButtonHandler, EmptyState, EndOfList, SearchInput, Tappable } from '@/shared/ui';
import { fadeIn, listItem, pressScale, scaleIn, staggerChildren } from '@/shared/ui/animations';
import { PaletteIcon, SearchOffIcon, SortIcon } from '@/shared/ui/icons';

type SortMode = 'newest' | 'oldest' | 'byName';

function useDebounce(value: string, delay: number): string {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}

function sortCreatives(items: CreativeTemplate[], mode: SortMode): CreativeTemplate[] {
  const sorted = [...items];
  switch (mode) {
    case 'newest':
      return sorted.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
    case 'oldest':
      return sorted.sort((a, b) => new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime());
    case 'byName':
      return sorted.sort((a, b) => a.title.localeCompare(b.title));
  }
}

const SORT_OPTIONS: SortMode[] = ['newest', 'oldest', 'byName'];

export default function CreativesPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const haptic = useHaptic();
  const { data, isLoading, hasNextPage, fetchNextPage, isFetchingNextPage } = useCreatives();

  const [searchQuery, setSearchQuery] = useState('');
  const [searchFocused, setSearchFocused] = useState(false);
  const [sortMode, setSortMode] = useState<SortMode>('newest');
  const [sortSheetOpen, setSortSheetOpen] = useState(false);
  const debouncedQuery = useDebounce(searchQuery, 300);

  const allCreatives = data?.pages.flatMap((p) => p.items) ?? [];

  const filteredAndSorted = useMemo(() => {
    let items = allCreatives;
    if (debouncedQuery) {
      const q = debouncedQuery.toLowerCase();
      items = items.filter((c) => c.title.toLowerCase().includes(q) || c.draft.text.toLowerCase().includes(q));
    }
    return sortCreatives(items, sortMode);
  }, [allCreatives, debouncedQuery, sortMode]);

  const handleScroll = useCallback(
    (e: React.UIEvent<HTMLDivElement>) => {
      const el = e.currentTarget;
      if (el.scrollTop + el.clientHeight >= el.scrollHeight - 100 && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    },
    [hasNextPage, isFetchingNextPage, fetchNextPage],
  );

  const SortSheetContent = () => (
    <div className="am-creatives-sort-sheet">
      <Text type="title2" weight="bold">
        {t('creatives.sort.title')}
      </Text>
      <Group>
        {SORT_OPTIONS.map((option) => (
          <GroupItem
            key={option}
            text={t(`creatives.sort.${option}`)}
            after={sortMode === option ? <Icon name="check" color="accent" /> : undefined}
            onClick={() => {
              haptic.selectionChanged();
              setSortMode(option);
              setSortSheetOpen(false);
            }}
          />
        ))}
      </Group>
    </div>
  );

  const sheetMap = { sort: SortSheetContent };

  return (
    <div className="am-creatives-page" onScroll={handleScroll}>
      <BackButtonHandler />

      <div className="am-creatives-page__header">
        <Text type="title1" weight="bold">
          {t('creatives.title')}
        </Text>
        <motion.div {...pressScale}>
          <Tappable
            onClick={() => {
              haptic.impactOccurred('light');
              navigate('/profile/creatives/new');
            }}
            className="am-creatives-page__add"
          >
            +
          </Tappable>
        </motion.div>
      </div>

      {!isLoading && allCreatives.length > 0 && (
        <div className="am-creatives-page__search-wrap">
          <motion.div
            animate={{ scale: searchFocused ? 1.01 : 1 }}
            transition={{ duration: 0.15, ease: easeOut }}
            className="am-creatives-page__search-row"
          >
            <SearchInput
              value={searchQuery}
              onChange={setSearchQuery}
              onFocus={() => setSearchFocused(true)}
              onBlur={() => setSearchFocused(false)}
              placeholder={t('creatives.search.placeholder')}
              focused={searchFocused}
              className="flex-1"
            />
            <motion.div {...pressScale}>
              <Tappable
                onClick={() => {
                  haptic.impactOccurred('light');
                  setSortSheetOpen(true);
                }}
                className="am-creatives-page__sort"
              >
                <SortIcon
                  className={
                    sortMode !== 'newest' ? 'am-creatives-page__sort-icon--active' : 'am-creatives-page__sort-icon'
                  }
                />
              </Tappable>
            </motion.div>
          </motion.div>
        </div>
      )}

      <AnimatePresence mode="wait">
        {isLoading ? (
          <motion.div key="skeleton" {...fadeIn}>
            <Group skeleton={{ show: true }}>
              <div key="skeleton-a" className="am-creatives-page__skeleton-row" />
              <div key="skeleton-b" className="am-creatives-page__skeleton-row" />
              <div key="skeleton-c" className="am-creatives-page__skeleton-row" />
            </Group>
          </motion.div>
        ) : allCreatives.length === 0 ? (
          <motion.div key="empty" {...scaleIn}>
            <EmptyState
              icon={<PaletteIcon className="am-empty-state-icon" />}
              title={t('creatives.empty.title')}
              description={t('creatives.empty.description')}
              actionLabel={t('creatives.empty.cta')}
              onAction={() => navigate('/profile/creatives/new')}
            />
          </motion.div>
        ) : filteredAndSorted.length === 0 ? (
          <motion.div key="no-results" {...scaleIn}>
            <EmptyState
              icon={<SearchOffIcon className="am-empty-state-icon" />}
              title={t('catalog.empty.title')}
              description={t('catalog.empty.description')}
            />
          </motion.div>
        ) : (
          <motion.div key="list" {...staggerChildren} initial="initial" animate="animate">
            <Group>
              {filteredAndSorted.map((creative) => (
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

      <Sheet sheets={sheetMap} activeSheet="sort" opened={sortSheetOpen} onClose={() => setSortSheetOpen(false)} />
    </div>
  );
}
