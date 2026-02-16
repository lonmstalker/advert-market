import { Button, Input, Text } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useHaptic } from '@/shared/hooks/use-haptic';
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

export function LinkInputSheet({ open, onClose, onSubmit }: LinkInputSheetProps) {
  const { t } = useTranslation();
  const haptic = useHaptic();
  const [url, setUrl] = useState('');

  const isValid = isValidUrl(url);

  const handleSubmit = useCallback(() => {
    if (!isValid) return;
    haptic.impactOccurred('medium');
    onSubmit(url);
    setUrl('');
    onClose();
  }, [url, isValid, haptic, onSubmit, onClose]);

  const handleClose = useCallback(() => {
    haptic.impactOccurred('light');
    setUrl('');
    onClose();
  }, [haptic, onClose]);

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
          className="fixed inset-0 z-[100] flex items-end bg-[var(--color-background-overlay)]"
          onClick={handleClose}
        >
          <motion.div
            {...slideFromBottom}
            className="w-full bg-bg-base rounded-t-[16px] px-4 pt-5 pb-[calc(20px+var(--am-safe-area-bottom))] flex flex-col gap-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-2">
              <LinkIcon className="size-5 text-accent" />
              <Text type="title3" weight="bold">
                {t('creatives.form.formatting.link')}
              </Text>
            </div>

            <div>
              <div className="mb-2">
                <Text type="subheadline2" color="secondary">
                  URL
                </Text>
              </div>
              <Input value={url} onChange={setUrl} placeholder="https://example.com" />
            </div>

            {url && !isValid && (
              <Text type="caption1" color="danger">
                {t('creatives.form.linkInvalid')}
              </Text>
            )}

            <div className="flex gap-3">
              <div className="flex-1">
                <Button text={t('common.cancel')} type="secondary" onClick={handleClose} />
              </div>
              <div className="flex-1">
                <Button text={t('common.apply')} type="primary" disabled={!isValid} onClick={handleSubmit} />
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
