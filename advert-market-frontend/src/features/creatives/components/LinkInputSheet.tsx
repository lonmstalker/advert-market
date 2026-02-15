import { Button, Input, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { slideFromBottom } from '@/shared/ui/animations';
import { LinkIcon } from '@/shared/ui/icons';

type LinkInputSheetProps = {
  open: boolean;
  onClose: () => void;
  onSubmit: (url: string) => void;
};

function isValidUrl(str: string): boolean {
  try {
    const url = new URL(str);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

const overlayStyle = {
  position: 'fixed' as const,
  inset: 0,
  background: 'var(--color-background-overlay)',
  zIndex: 100,
  display: 'flex',
  alignItems: 'flex-end',
};

const sheetStyle = {
  width: '100%',
  background: 'var(--color-background-base)',
  borderRadius: '16px 16px 0 0',
  padding: '20px 16px calc(20px + var(--am-safe-area-bottom))',
  display: 'flex',
  flexDirection: 'column' as const,
  gap: 16,
};

export function LinkInputSheet({ open, onClose, onSubmit }: LinkInputSheetProps) {
  const { t } = useTranslation();
  const [url, setUrl] = useState('');

  const isValid = isValidUrl(url);

  const handleSubmit = useCallback(() => {
    if (!isValid) return;
    onSubmit(url);
    setUrl('');
    onClose();
  }, [url, isValid, onSubmit, onClose]);

  const handleClose = useCallback(() => {
    setUrl('');
    onClose();
  }, [onClose]);

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
          style={overlayStyle}
          onClick={handleClose}
        >
          <motion.div {...slideFromBottom} style={sheetStyle} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <LinkIcon style={{ width: 20, height: 20, color: 'var(--color-accent-primary)' }} />
              <Text type="title3" weight="bold">
                {t('creatives.form.formatting.link')}
              </Text>
            </div>

            <div>
              <div style={{ marginBottom: 8 }}>
                <Text type="subheadline2" color="secondary">
                  URL
                </Text>
              </div>
              <Input value={url} onChange={setUrl} placeholder="https://example.com" />
            </div>

            {url && !isValid && (
              <span style={{ fontSize: 12, color: 'var(--color-state-destructive)' }}>
                {t('creatives.form.linkInvalid')}
              </span>
            )}

            <div style={{ display: 'flex', gap: 12 }}>
              <div style={{ flex: 1 }}>
                <Button text={t('common.cancel')} type="secondary" onClick={handleClose} />
              </div>
              <div style={{ flex: 1 }}>
                <Button text={t('common.apply')} type="primary" disabled={!isValid} onClick={handleSubmit} />
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
