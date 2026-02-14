import type { CSSProperties } from 'react';

export const postContent: CSSProperties = {
  background: 'var(--color-background-base)',
  overflow: 'hidden',
  width: '100%',
};

export const postHeader: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 8,
  padding: '10px 12px 4px',
};

export const headerAvatar: CSSProperties = {
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

export const headerInfo: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  flex: 1,
  minWidth: 0,
};

export const headerTitle: CSSProperties = {
  fontSize: 14,
  fontWeight: 600,
  color: 'var(--color-accent-primary)',
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
};

export const headerTime: CSSProperties = {
  fontSize: 11,
  color: 'var(--color-foreground-tertiary)',
  flexShrink: 0,
  marginLeft: 8,
};

export const postBody: CSSProperties = {
  padding: '4px 12px 2px',
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
  gap: 6,
  padding: '8px 16px',
  borderRadius: 6,
  background: 'color-mix(in srgb, var(--color-accent-primary) 12%, transparent)',
  border: 'none',
  color: 'var(--color-accent-primary)',
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

export const chatArea: CSSProperties = {
  background: 'var(--color-background-secondary)',
  padding: '0 0 16px',
  minHeight: 200,
  overflow: 'hidden',
};

export const chatTopBar: CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: 10,
  padding: '10px 12px',
  background: 'var(--color-background-base)',
  borderBottom: '1px solid var(--color-border-separator)',
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
  padding: '2px 12px 8px',
  fontSize: 12,
  color: 'var(--color-foreground-tertiary)',
};
