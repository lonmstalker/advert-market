import { motion } from 'motion/react';
import type { CSSProperties, ReactNode } from 'react';

type DeviceFrameProps = {
  children: ReactNode;
};

const frameOuter: CSSProperties = {
  position: 'relative',
  display: 'flex',
  justifyContent: 'center',
  padding: '24px 0',
};

const glowBlob: CSSProperties = {
  position: 'absolute',
  top: '10%',
  left: '50%',
  transform: 'translateX(-50%)',
  width: '80%',
  height: '60%',
  borderRadius: '50%',
  background:
    'radial-gradient(ellipse, color-mix(in srgb, var(--color-accent-primary) 15%, transparent), transparent 70%)',
  filter: 'blur(40px)',
  pointerEvents: 'none',
};

const frameContainer: CSSProperties = {
  position: 'relative',
  width: '100%',
  maxWidth: 360,
  borderRadius: 28,
  overflow: 'hidden',
  background: 'var(--color-background-base)',
  boxShadow: '0 4px 20px rgba(0,0,0,0.08), 0 1px 4px rgba(0,0,0,0.04)',
};

const gradientBorder: CSSProperties = {
  position: 'absolute',
  inset: 0,
  borderRadius: 28,
  padding: 2,
  background:
    'linear-gradient(135deg, var(--color-accent-primary), color-mix(in srgb, var(--color-accent-primary) 50%, var(--color-link)), var(--color-accent-primary))',
  WebkitMask: 'linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)',
  WebkitMaskComposite: 'xor',
  maskComposite: 'exclude',
  pointerEvents: 'none',
};

const statusBar: CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: '6px 20px',
  fontSize: 12,
  fontWeight: 600,
  color: 'var(--color-foreground-primary)',
  background: 'var(--color-background-base)',
};

const statusBarIcons: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 4,
};

function formatTime(): string {
  const now = new Date();
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`;
}

function SignalIcon() {
  return (
    <svg width="16" height="12" viewBox="0 0 16 12" fill="none" aria-hidden="true">
      <rect x="0" y="8" width="3" height="4" rx="1" fill="currentColor" opacity="0.9" />
      <rect x="4.5" y="5" width="3" height="7" rx="1" fill="currentColor" opacity="0.9" />
      <rect x="9" y="2" width="3" height="10" rx="1" fill="currentColor" opacity="0.9" />
      <rect x="13.5" y="0" width="3" height="12" rx="1" fill="currentColor" opacity="0.3" />
    </svg>
  );
}

function WifiIcon() {
  return (
    <svg width="14" height="12" viewBox="0 0 14 12" fill="none" aria-hidden="true">
      <path d="M7 10.5a1.25 1.25 0 100 2.5 1.25 1.25 0 000-2.5z" fill="currentColor" opacity="0.9" />
      <path
        d="M3.5 8.5C4.5 7.5 5.7 7 7 7s2.5.5 3.5 1.5"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        opacity="0.9"
      />
      <path
        d="M1 5.5C2.8 3.7 4.8 3 7 3s4.2.7 6 2.5"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
        opacity="0.5"
      />
    </svg>
  );
}

function BatteryIcon() {
  return (
    <svg width="22" height="12" viewBox="0 0 22 12" fill="none" aria-hidden="true">
      <rect x="0.5" y="0.5" width="19" height="11" rx="2" stroke="currentColor" opacity="0.4" />
      <rect x="2" y="2" width="14" height="8" rx="1" fill="currentColor" opacity="0.9" />
      <rect x="20.5" y="3.5" width="1.5" height="5" rx="0.75" fill="currentColor" opacity="0.3" />
    </svg>
  );
}

export function DeviceFrame({ children }: DeviceFrameProps) {
  return (
    <div style={frameOuter}>
      <motion.div
        animate={{ x: [0, 12, 0], y: [0, -6, 0] }}
        transition={{ duration: 8, repeat: Number.POSITIVE_INFINITY, ease: 'easeInOut' }}
        style={glowBlob}
      />
      <div style={frameContainer}>
        <div style={gradientBorder} />
        <div style={statusBar}>
          <span>{formatTime()}</span>
          <div style={statusBarIcons}>
            <SignalIcon />
            <WifiIcon />
            <BatteryIcon />
          </div>
        </div>
        {children}
      </div>
    </div>
  );
}
