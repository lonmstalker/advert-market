import { Text } from '@telegram-tools/ui-kit';
import { ChannelAvatar } from '@/shared/ui';

type ProfileHeroProps = {
  displayName: string;
  username?: string;
  roleBadge?: string | null;
  memberSince?: string | null;
  avatarUrl?: string;
};

export function ProfileHero({ displayName, username, roleBadge, memberSince, avatarUrl }: ProfileHeroProps) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        padding: '24px 16px 16px',
        gap: 8,
      }}
    >
      <div
        style={{
          width: 80,
          height: 80,
          borderRadius: '50%',
          padding: 3,
          background:
            'linear-gradient(135deg, var(--color-accent-primary), color-mix(in srgb, var(--color-accent-primary) 60%, #a855f7))',
        }}
      >
        <div
          style={{
            width: '100%',
            height: '100%',
            borderRadius: '50%',
            overflow: 'hidden',
            border: '3px solid var(--color-background-base)',
          }}
        >
          {avatarUrl ? (
            <img src={avatarUrl} alt={displayName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
          ) : (
            <ChannelAvatar title={displayName || 'U'} size="lg" />
          )}
        </div>
      </div>

      <div style={{ textAlign: 'center' }}>
        <Text type="title2" weight="bold">
          {displayName}
        </Text>
        {username && (
          <div style={{ marginTop: 2 }}>
            <Text type="body" color="secondary">
              {username}
            </Text>
          </div>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', justifyContent: 'center' }}>
        {roleBadge && (
          <span
            style={{
              padding: '3px 10px',
              borderRadius: 12,
              background: 'color-mix(in srgb, var(--color-accent-primary) 12%, transparent)',
              color: 'var(--color-accent-primary)',
              fontSize: 12,
              fontWeight: 600,
            }}
          >
            {roleBadge}
          </span>
        )}
        {memberSince && (
          <Text type="caption1" color="tertiary">
            {memberSince}
          </Text>
        )}
      </div>
    </div>
  );
}
