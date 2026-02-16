import { Input, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useTranslation } from 'react-i18next';
import { Tappable } from '@/shared/ui';
import { pressScale } from '@/shared/ui/animations';
import { CrossIcon } from '@/shared/ui/icons';
import type { InlineButton, TelegramKeyboardRow } from '../types/creative';
import { ensureButtonId, makeLocalId } from '../types/creative';

type ButtonBuilderProps = {
  buttons: TelegramKeyboardRow[];
  onChange: (buttons: TelegramKeyboardRow[]) => void;
  maxRows?: number;
  maxButtonsPerRow?: number;
};

function createEmptyButton(): InlineButton {
  return {
    id: makeLocalId('btn'),
    text: '',
  };
}

export function ButtonBuilder({ buttons, onChange, maxRows = 5, maxButtonsPerRow = 5 }: ButtonBuilderProps) {
  const { t } = useTranslation();

  const addRow = () => {
    if (buttons.length >= maxRows) return;
    onChange([...buttons, [createEmptyButton()]]);
  };

  const removeRow = (rowIndex: number) => {
    onChange(buttons.filter((_, index) => index !== rowIndex));
  };

  const addButton = (rowIndex: number) => {
    const next = buttons.map((row, index) => {
      if (index !== rowIndex || row.length >= maxButtonsPerRow) {
        return row;
      }
      return [...row, createEmptyButton()];
    });
    onChange(next);
  };

  const removeButton = (rowIndex: number, buttonIndex: number) => {
    const next = buttons
      .map((row, index) => {
        if (index !== rowIndex) return row;
        return row.filter((_, idx) => idx !== buttonIndex);
      })
      .filter((row) => row.length > 0);
    onChange(next);
  };

  const updateButton = (rowIndex: number, buttonIndex: number, patch: Partial<InlineButton>) => {
    const next = buttons.map((row, index) => {
      if (index !== rowIndex) return row;
      return row.map((button, idx) => {
        if (idx !== buttonIndex) return button;
        return ensureButtonId({ ...button, ...patch });
      });
    });
    onChange(next);
  };

  return (
    <div className="am-section">
      <Text type="subheadline2" color="secondary">
        {t('creatives.form.buttons')}
      </Text>

      <AnimatePresence initial={false}>
        {buttons.map((row, rowIndex) => (
          <motion.div
            // biome-ignore lint/suspicious/noArrayIndexKey: rows are local form state items.
            key={rowIndex}
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.18 }}
            className="rounded-card border border-card-border bg-card-surface p-3"
          >
            <div className="mb-2 flex items-center justify-between">
              <Text type="caption1" color="secondary">
                {t('creatives.form.rowLabel', { index: rowIndex + 1 })}
              </Text>
              <Tappable
                onClick={() => removeRow(rowIndex)}
                className="flex h-7 w-7 items-center justify-center rounded-full border-0 bg-soft-destructive text-destructive"
                aria-label={t('creatives.form.removeRow')}
              >
                <CrossIcon style={{ width: 14, height: 14 }} />
              </Tappable>
            </div>

            <div className="flex flex-col gap-2">
              {row.map((button, buttonIndex) => (
                <div key={button.id} className="rounded-row border border-separator bg-bg-base p-2">
                  <div className="mb-1 flex items-center justify-between">
                    <Text type="caption2" color="tertiary">
                      {t('creatives.form.buttonLabel', { index: buttonIndex + 1 })}
                    </Text>
                    <Tappable
                      onClick={() => removeButton(rowIndex, buttonIndex)}
                      className="flex h-6 w-6 items-center justify-center rounded-full border-0 bg-transparent text-destructive"
                      aria-label={t('creatives.form.removeButton')}
                    >
                      <CrossIcon style={{ width: 12, height: 12 }} />
                    </Tappable>
                  </div>
                  <div className="mb-2">
                    <Input
                      value={button.text}
                      onChange={(value) => updateButton(rowIndex, buttonIndex, { text: value })}
                      placeholder={t('creatives.form.buttonText')}
                    />
                  </div>
                  <Input
                    value={button.url ?? ''}
                    onChange={(value) => updateButton(rowIndex, buttonIndex, { url: value })}
                    placeholder="https://"
                  />
                </div>
              ))}
            </div>

            {row.length < maxButtonsPerRow && (
              <div className="mt-2">
                <Tappable
                  onClick={() => addButton(rowIndex)}
                  className="flex w-full items-center justify-center gap-1 rounded-row border border-separator bg-bg-base px-3 py-2 text-sm text-fg-secondary"
                >
                  <span aria-hidden="true">+</span>
                  {t('creatives.form.addButton')}
                </Tappable>
              </div>
            )}
          </motion.div>
        ))}
      </AnimatePresence>

      {buttons.length < maxRows && (
        <motion.div {...pressScale}>
          <Tappable
            onClick={addRow}
            className="flex w-full items-center justify-center gap-2 rounded-row border border-separator bg-bg-base px-4 py-3 text-sm text-accent"
          >
            <span aria-hidden="true">+</span>
            {t('creatives.form.addRow')}
          </Tappable>
        </motion.div>
      )}
    </div>
  );
}
