import { Input, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import type { CSSProperties } from 'react';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';
import { pressScale } from '@/shared/ui/animations';
import { CrossIcon, GlobeIcon } from '@/shared/ui/icons';
import type { InlineButton } from '../types/creative';

const previewStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 6,
  padding: '10px 16px',
  borderRadius: 20,
  background: 'transparent',
  border: '1px solid color-mix(in srgb, var(--color-accent-primary) 40%, transparent)',
  color: 'var(--color-accent-primary)',
  fontSize: 14,
  fontWeight: 500,
  margin: '12px 12px 8px',
};

const addZoneStyle: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  padding: '14px 16px',
  borderRadius: 12,
  border: '2px dashed color-mix(in srgb, var(--color-accent-primary) 25%, transparent)',
  background: 'color-mix(in srgb, var(--color-accent-primary) 3%, transparent)',
  cursor: 'pointer',
  color: 'var(--color-accent-primary)',
  fontSize: 14,
  fontWeight: 500,
};

type ButtonBuilderProps = {
  buttons: InlineButton[];
  onChange: (buttons: InlineButton[]) => void;
  max?: number;
};

export function ButtonBuilder({ buttons, onChange, max = 5 }: ButtonBuilderProps) {
  const { t } = useTranslation();

  const addButton = () => {
    if (buttons.length >= max) return;
    onChange([...buttons, { text: '', url: '' }]);
  };

  const removeButton = (index: number) => {
    onChange(buttons.filter((_, i) => i !== index));
  };

  const updateButton = (index: number, field: keyof InlineButton, value: string) => {
    const updated = buttons.map((btn, i) => (i === index ? { ...btn, [field]: value } : btn));
    onChange(updated);
  };

  return (
    <div className="flex flex-col gap-2">
      <Text type="subheadline2" color="secondary">
        {t('creatives.form.buttons')}
      </Text>
      <AnimatePresence initial={false}>
        {buttons.map((btn, index) => (
          <motion.div
            // biome-ignore lint/suspicious/noArrayIndexKey: buttons have no stable ID
            key={index}
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2 }}
          >
            <div className="rounded-[12px] bg-bg-base border border-separator overflow-hidden">
              <div className="flex items-center justify-between px-3 pt-2">
                <Text type="caption1" color="secondary">
                  #{index + 1}
                </Text>
                <Tappable
                  onClick={() => removeButton(index)}
                  style={{
                    width: 28,
                    height: 28,
                    borderRadius: 14,
                    border: 'none',
                    background: 'color-mix(in srgb, var(--color-state-destructive) 10%, transparent)',
                    color: 'var(--color-state-destructive)',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                  aria-label={t('common.cancel')}
                >
                  <CrossIcon style={{ width: 14, height: 14 }} />
                </Tappable>
              </div>

              {/* Preview of the button */}
              <div style={previewStyle}>
                <GlobeIcon style={{ width: 14, height: 14 }} />
                {btn.text || t('creatives.form.buttonText')}
              </div>

              {/* Input fields */}
              <div className="flex flex-col gap-2 px-3 pb-3">
                <div>
                  <div className="mb-1">
                    <Text type="caption1" color="tertiary">
                      {t('creatives.form.buttonText')}
                    </Text>
                  </div>
                  <Input
                    value={btn.text}
                    onChange={(value) => updateButton(index, 'text', value)}
                    placeholder={t('creatives.form.buttonText')}
                  />
                </div>
                <div>
                  <div className="mb-1">
                    <Text type="caption1" color="tertiary">
                      {t('creatives.form.buttonUrl')}
                    </Text>
                  </div>
                  <Input
                    value={btn.url}
                    onChange={(value) => updateButton(index, 'url', value)}
                    placeholder="https://..."
                  />
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </AnimatePresence>
      {buttons.length < max && (
        <motion.div {...pressScale}>
          <Tappable onClick={addButton} style={addZoneStyle}>
            + {t('creatives.form.addButton')}
          </Tappable>
        </motion.div>
      )}
    </div>
  );
}
