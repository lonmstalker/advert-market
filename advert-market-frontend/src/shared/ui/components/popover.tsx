import { Button } from '@telegram-tools/ui-kit';
import { AnimatePresence, motion } from 'motion/react';
import type { ReactNode } from 'react';
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

type PopoverProps = {
  content: ReactNode;
  children: ReactNode;
};

export function Popover({ content, children }: PopoverProps) {
  const [open, setOpen] = useState(false);
  const [coords, setCoords] = useState<{ top: number; left: number } | null>(null);
  const triggerRef = useRef<HTMLSpanElement>(null);
  const popoverRef = useRef<HTMLDivElement>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const close = useCallback(() => {
    setOpen(false);
    if (timerRef.current) clearTimeout(timerRef.current);
  }, []);

  const toggle = useCallback(() => {
    setOpen((prev) => {
      if (!prev) {
        timerRef.current = setTimeout(() => setOpen(false), 4000);
      } else if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
      return !prev;
    });
  }, []);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  useLayoutEffect(() => {
    if (!open || !triggerRef.current) {
      setCoords(null);
      return;
    }

    function updatePosition() {
      const trigger = triggerRef.current;
      if (!trigger) return;
      const rect = trigger.getBoundingClientRect();
      const popover = popoverRef.current;
      const popoverWidth = popover ? popover.offsetWidth : 0;

      let left = rect.left + rect.width / 2 - popoverWidth / 2;
      const margin = 8;
      if (left < margin) left = margin;
      if (popoverWidth > 0 && left + popoverWidth > window.innerWidth - margin) {
        left = window.innerWidth - margin - popoverWidth;
      }

      setCoords({
        top: rect.top - 8,
        left,
      });
    }

    updatePosition();
    // Recalculate after popover renders (first pass has popoverWidth=0)
    requestAnimationFrame(updatePosition);
  }, [open]);

  return (
    <span style={{ display: 'inline-flex', alignItems: 'center' }}>
      <span ref={triggerRef} className="am-popover-trigger">
        <Button
          type="secondary"
          className="am-popover-trigger__button"
          icon={<span className="am-popover-trigger__content">{children}</span>}
          onClick={toggle}
        />
      </span>
      {createPortal(
        <AnimatePresence>
          {open && (
            <>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.15 }}
                onClick={close}
                style={{
                  position: 'fixed',
                  inset: 0,
                  zIndex: 9998,
                }}
              />
              <motion.div
                ref={popoverRef}
                initial={{ opacity: 0, scale: 0.92, y: 4 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.92, y: 4 }}
                transition={{ duration: 0.15 }}
                style={{
                  position: 'fixed',
                  top: coords ? coords.top : -9999,
                  left: coords ? coords.left : -9999,
                  transform: 'translateY(-100%)',
                  zIndex: 9999,
                  background: 'var(--color-background-base)',
                  border: '1px solid var(--color-border-separator)',
                  borderRadius: 10,
                  padding: '8px 12px',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
                  whiteSpace: 'nowrap',
                  pointerEvents: 'auto',
                }}
              >
                {content}
                {/* Arrow */}
                <span
                  style={{
                    position: 'absolute',
                    bottom: -5,
                    left: '50%',
                    transform: 'translateX(-50%) rotate(45deg)',
                    width: 10,
                    height: 10,
                    background: 'var(--color-background-base)',
                    borderRight: '1px solid var(--color-border-separator)',
                    borderBottom: '1px solid var(--color-border-separator)',
                  }}
                />
              </motion.div>
            </>
          )}
        </AnimatePresence>,
        document.body,
      )}
    </span>
  );
}
