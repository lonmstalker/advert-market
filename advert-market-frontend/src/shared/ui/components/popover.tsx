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
    <span className="inline-flex items-center">
      <span ref={triggerRef} className="am-popover-trigger">
        <button type="button" className="am-popover-trigger__button" onClick={toggle}>
          <span className="am-popover-trigger__content">{children}</span>
        </button>
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
                className="am-popover-backdrop"
              />
              <motion.div
                ref={popoverRef}
                initial={{ opacity: 0, scale: 0.92, y: 4 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.92, y: 4 }}
                transition={{ duration: 0.15 }}
                className="am-popover-bubble"
                style={{
                  top: coords ? coords.top : -9999,
                  left: coords ? coords.left : -9999,
                }}
              >
                {content}
                <span className="am-popover-arrow" />
              </motion.div>
            </>
          )}
        </AnimatePresence>,
        document.body,
      )}
    </span>
  );
}
