import type { CSSProperties } from 'react';

export const postBubble: CSSProperties = {
  background: 'var(--color-background-base)',
  borderRadius: 12,
  border: '1px solid var(--color-border-separator)',
  overflow: 'hidden',
  maxWidth: 400,
  width: '100%',
};

export const postHeader: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 8,
  padding: '10px 12px 4px',
};

export const headerAvatar: CSSProperties = {
  width: 32,
  height: 32,
  borderRadius: '50%',
  background: 'var(--color-accent-primary)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: 'var(--color-static-white)',
  fontSize: 14,
  fontWeight: 600,
  flexShrink: 0,
};

export const headerInfo: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  minWidth: 0,
};

export const headerTitle: CSSProperties = {
  fontSize: 14,
  fontWeight: 600,
  color: 'var(--color-foreground-primary)',
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
};

export const headerTime: CSSProperties = {
  fontSize: 11,
  color: 'var(--color-foreground-tertiary)',
};

export const postBody: CSSProperties = {
  padding: '4px 12px 8px',
  fontSize: 15,
  lineHeight: 1.4,
  color: 'var(--color-foreground-primary)',
  wordBreak: 'break-word',
};

export const mediaContainer: CSSProperties = {
  width: '100%',
};

export const mediaPlaceholder: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: 8,
  padding: '24px 12px',
  background: 'var(--color-background-section)',
  color: 'var(--color-foreground-secondary)',
  fontSize: 14,
};

export const mediaImage: CSSProperties = {
  width: '100%',
  display: 'block',
  objectFit: 'cover',
  maxHeight: 300,
};

export const buttonsContainer: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 4,
  padding: '4px 8px 8px',
};

export const inlineButton: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: '8px 12px',
  borderRadius: 8,
  background: 'var(--color-accent-primary)',
  color: 'var(--color-static-white)',
  fontSize: 14,
  fontWeight: 500,
  textDecoration: 'none',
  cursor: 'pointer',
};

export const spoilerHidden: CSSProperties = {
  background: 'var(--color-foreground-primary)',
  color: 'transparent',
  borderRadius: 4,
  cursor: 'pointer',
  transition: 'all 0.3s ease',
  userSelect: 'none',
};

export const spoilerRevealed: CSSProperties = {
  background: 'transparent',
  color: 'inherit',
  borderRadius: 4,
  transition: 'all 0.3s ease',
};

export const codeInline: CSSProperties = {
  fontFamily: 'monospace',
  fontSize: '0.9em',
  background: 'var(--color-background-section)',
  padding: '2px 4px',
  borderRadius: 4,
};

export const preBlock: CSSProperties = {
  fontFamily: 'monospace',
  fontSize: '0.85em',
  background: 'var(--color-background-section)',
  padding: '8px 12px',
  borderRadius: 8,
  margin: '4px 0',
  overflowX: 'auto',
};

export const linkStyle: CSSProperties = {
  color: 'var(--color-link)',
  textDecoration: 'none',
};

export const chatBackground: CSSProperties = {
  background: 'var(--color-background-section)',
  backgroundImage:
    'radial-gradient(circle at 20% 30%, color-mix(in srgb, var(--color-accent-primary) 6%, transparent) 0%, transparent 50%), radial-gradient(circle at 80% 70%, color-mix(in srgb, var(--color-accent-primary) 4%, transparent) 0%, transparent 50%)',
  borderRadius: 16,
  padding: '12px 12px 16px',
  overflow: 'hidden',
};

export const chatTopBar: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 10,
  padding: '8px 12px',
  background: 'var(--color-background-base)',
  borderRadius: '12px 12px 0 0',
  marginBottom: 12,
  border: '1px solid var(--color-border-separator)',
  borderBottom: 'none',
};

export const chatTopBarAvatar: CSSProperties = {
  width: 28,
  height: 28,
  borderRadius: '50%',
  background: 'var(--color-accent-primary)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  color: 'var(--color-static-white)',
  fontSize: 12,
  fontWeight: 600,
  flexShrink: 0,
};

export const chatTopBarTitle: CSSProperties = {
  fontSize: 14,
  fontWeight: 600,
  color: 'var(--color-foreground-primary)',
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
};

export const chatTopBarSubtitle: CSSProperties = {
  fontSize: 11,
  color: 'var(--color-foreground-tertiary)',
};

export const viewCounter: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 4,
  justifyContent: 'flex-end',
  padding: '2px 12px 6px',
  fontSize: 12,
  color: 'var(--color-foreground-tertiary)',
};

export const bubbleTail: CSSProperties = {
  position: 'relative',
};
