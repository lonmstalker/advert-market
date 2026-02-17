import { Image, Text } from '@telegram-tools/ui-kit';
import { AppSurfaceCard, ChannelAvatar } from '@/shared/ui';

type ProfileHeroProps = {
  displayName: string;
  username?: string;
  roleBadge?: string | null;
  memberSince?: string | null;
  avatarUrl?: string;
};

export function ProfileHero({ displayName, username, roleBadge, memberSince, avatarUrl }: ProfileHeroProps) {
  return (
    <AppSurfaceCard className="am-profile-hero">
      <div className="flex flex-col items-center pt-4 px-4 pb-3 gap-2">
        <div className="am-profile-avatar-ring p-[3px] bg-accent">
          <div className="w-full h-full rounded-full overflow-hidden border-[3px] border-bg-base flex-center">
            {avatarUrl ? (
              <Image
                src={avatarUrl}
                alt={displayName}
                width="100%"
                height="100%"
                borderRadius="50%"
                objectFit="cover"
              />
            ) : (
              <ChannelAvatar title={displayName || 'U'} size="xl" />
            )}
          </div>
        </div>

        <div className="text-center">
          <Text type="title2" weight="bold">
            {displayName}
          </Text>
          {username && (
            <div className="mt-1">
              <Text type="body" color="secondary">
                {username}
              </Text>
            </div>
          )}
        </div>

        <div className="flex items-center gap-2 flex-wrap justify-center">
          {roleBadge && <span className="am-role-badge">{roleBadge}</span>}
          {memberSince && (
            <Text type="caption1" color="tertiary">
              {memberSince}
            </Text>
          )}
        </div>
      </div>
    </AppSurfaceCard>
  );
}
