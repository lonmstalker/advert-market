import type { Meta, StoryObj } from '@storybook/react-vite';
import { Text } from '@telegram-tools/ui-kit';
import { useState } from 'react';
import { Chip } from '../components/chip';
import { FilterButton } from '../components/filter-button';
import { Popover } from '../components/popover';
import { SearchInput } from '../components/search-input';
import { SegmentControl } from '../components/segment-control';
import { Tappable } from '../components/tappable';
import { Textarea } from '../components/textarea';
import { TextareaField } from '../components/textarea-field';

const meta: Meta = {
  title: 'Ad Market/Controls',
  parameters: {
    route: '/catalog',
  },
};

export default meta;
type Story = StoryObj;

export const SearchAndFilters: Story = {
  render: function Render() {
    const [query, setQuery] = useState('');
    const [selected, setSelected] = useState<string[]>(['crypto']);

    const toggleTopic = (topic: string) => {
      setSelected((prev) => (prev.includes(topic) ? prev.filter((value) => value !== topic) : [...prev, topic]));
    };

    return (
      <div style={{ display: 'grid', gap: 12 }}>
        <div style={{ display: 'flex', gap: 8 }}>
          <div style={{ flex: 1 }}>
            <SearchInput value={query} onChange={setQuery} placeholder="Поиск каналов" focused={query.length > 0} />
          </div>
          <FilterButton activeCount={selected.length} onClick={() => {}} />
        </div>

        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {['crypto', 'tech', 'finance'].map((topic) => (
            <Chip key={topic} label={topic} active={selected.includes(topic)} onClick={() => toggleTopic(topic)} />
          ))}
        </div>
      </div>
    );
  },
};

export const SegmentAndTextarea: Story = {
  render: function Render() {
    const [tab, setTab] = useState<'advertiser' | 'owner'>('advertiser');
    const [notes, setNotes] = useState('Нативная интеграция в первом экране.');

    return (
      <div style={{ display: 'grid', gap: 16 }}>
        <SegmentControl
          tabs={[
            { value: 'advertiser', label: 'Рекламодатель' },
            { value: 'owner', label: 'Владелец канала' },
          ]}
          active={tab}
          onChange={setTab}
        />

        <TextareaField
          label="Комментарий"
          value={notes}
          onChange={setNotes}
          placeholder="Опишите задачу"
          showCharCount
          maxLength={280}
        />

        <Textarea value={notes} onChange={setNotes} placeholder="Autosize textarea" autosize rows={3} />
      </div>
    );
  },
};

export const TappableAndPopover: Story = {
  render: function Render() {
    const [pressed, setPressed] = useState(false);

    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <Tappable
          className="am-chip"
          aria-pressed={pressed}
          onClick={() => setPressed((prev) => !prev)}
          style={{ padding: '8px 14px' }}
        >
          {pressed ? 'Выбрано' : 'Нажмите'}
        </Tappable>

        <Popover content={<Text type="caption1">Нативный tooltip для подсказок</Text>}>
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: 36,
              padding: '0 12px',
              borderRadius: 10,
              border: '1px solid var(--color-border-separator)',
              background: 'var(--color-background-base)',
            }}
          >
            Подсказка
          </span>
        </Popover>
      </div>
    );
  },
};
