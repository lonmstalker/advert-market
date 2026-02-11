import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button, Dropdown, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useRef, useState } from 'react';
import { scaleIn } from '../animations';

const meta: Meta<typeof Dropdown> = {
  title: 'UI Kit/Dropdown',
  component: Dropdown,
  parameters: { layout: 'padded' },
};

export default meta;
type Story = StoryObj<typeof Dropdown>;

const sortOptions = [
  { label: 'Price: Low to High', value: 'price_asc' },
  { label: 'Price: High to Low', value: 'price_desc' },
  { label: 'Newest First', value: 'date_desc' },
  { label: 'Most Popular', value: 'popular' },
];

export const Default: Story = {
  render: function Render() {
    const [active, setActive] = useState(false);
    const [selected, setSelected] = useState<string | null>(null);
    const triggerRef = useRef<HTMLDivElement>(null);
    return (
      <div style={{ position: 'relative' }}>
        <div ref={triggerRef}>
          <Button
            text={selected ? (sortOptions.find((o) => o.value === selected)?.label ?? 'Sort') : 'Sort by...'}
            type="secondary"
            onClick={() => setActive(!active)}
          />
        </div>
        <Dropdown
          options={sortOptions}
          active={active}
          selectedValue={selected}
          onSelect={(v) => {
            setSelected(v);
            setActive(false);
          }}
          onClose={() => setActive(false)}
          triggerRef={triggerRef}
        />
      </div>
    );
  },
};

export const AnimatedDropdown: Story = {
  render: function Render() {
    const [active, setActive] = useState(false);
    const [selected, setSelected] = useState<string | null>(null);
    const triggerRef = useRef<HTMLDivElement>(null);
    return (
      <div style={{ position: 'relative' }}>
        <div ref={triggerRef}>
          <Button text="Sort" type="secondary" onClick={() => setActive(!active)} />
        </div>
        <AnimatePresence>
          {active && (
            <motion.div {...scaleIn} style={{ position: 'absolute', top: '100%', left: 0, zIndex: 10, width: '100%' }}>
              <Dropdown
                options={sortOptions}
                active={active}
                selectedValue={selected}
                onSelect={(v) => {
                  setSelected(v);
                  setActive(false);
                }}
                onClose={() => setActive(false)}
                triggerRef={triggerRef}
              />
            </motion.div>
          )}
        </AnimatePresence>
        {selected && (
          <Text type="caption1" color="secondary" style={{ marginTop: '8px' }}>
            Selected: {sortOptions.find((o) => o.value === selected)?.label}
          </Text>
        )}
      </div>
    );
  },
};
