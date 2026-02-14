import { Button, Input, Text } from '@telegram-tools/ui-kit';
import type { CSSProperties } from 'react';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';
import { CrossIcon } from '@/shared/ui/icons';
import type { InlineButton } from '../types/creative';

const buttonRowStyle: CSSProperties = {
  display: 'flex',
  gap: 8,
  alignItems: 'flex-start',
  padding: 12,
  borderRadius: 12,
  background: 'var(--color-background-base)',
  border: '1px solid var(--color-border-separator)',
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
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <Text type="subheadline1" weight="medium">
        {t('creatives.form.buttons')}
      </Text>
      {buttons.map((btn, index) => (
        // biome-ignore lint/suspicious/noArrayIndexKey: buttons have no stable ID
        <div key={index} style={buttonRowStyle}>
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 4 }}>
            <Input
              value={btn.text}
              onChange={(value) => updateButton(index, 'text', value)}
              placeholder={t('creatives.form.buttonText')}
            />
            <Input value={btn.url} onChange={(value) => updateButton(index, 'url', value)} placeholder="https://..." />
          </div>
          <Tappable
            onClick={() => removeButton(index)}
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              border: 'none',
              background: 'var(--color-state-destructive)',
              color: 'var(--color-static-white)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginTop: 4,
            }}
            aria-label={t('common.cancel')}
          >
            <CrossIcon style={{ width: 16, height: 16 }} />
          </Tappable>
        </div>
      ))}
      {buttons.length < max && <Button text={t('creatives.form.addButton')} type="secondary" onClick={addButton} />}
    </div>
  );
}
