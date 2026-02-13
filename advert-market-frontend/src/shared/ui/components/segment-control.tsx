type Tab<T extends string> = {
  value: T;
  label: string;
};

type SegmentControlProps<T extends string> = {
  tabs: Tab<T>[];
  active: T;
  onChange: (value: T) => void;
};

export function SegmentControl<T extends string>({ tabs, active, onChange }: SegmentControlProps<T>) {
  return (
    <div
      style={{
        display: 'flex',
        gap: 0,
        borderRadius: 10,
        padding: 2,
        background: 'var(--color-background-base)',
        border: '1px solid var(--color-border-separator)',
      }}
    >
      {tabs.map((tab) => {
        const isActive = active === tab.value;
        return (
          <button
            key={tab.value}
            type="button"
            onClick={() => onChange(tab.value)}
            style={{
              flex: 1,
              padding: '8px 16px',
              borderRadius: 8,
              border: 'none',
              cursor: 'pointer',
              background: isActive ? 'var(--color-accent-primary)' : 'transparent',
              color: isActive ? 'var(--color-static-white)' : 'var(--color-foreground-primary)',
              fontSize: 14,
              fontWeight: isActive ? 600 : 400,
              fontFamily: 'inherit',
              WebkitTapHighlightColor: 'transparent',
              transition: 'background 0.2s, color 0.2s',
            }}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
