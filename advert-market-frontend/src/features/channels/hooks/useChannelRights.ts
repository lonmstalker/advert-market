import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/features/auth/hooks/use-auth';
import { channelKeys } from '@/shared/api/query-keys';
import { fetchChannelTeam } from '../api/channels';

export function useChannelRights(channelId: number) {
  const { profile } = useAuth();

  const { data: team, isLoading } = useQuery({
    queryKey: channelKeys.team(channelId),
    queryFn: () => fetchChannelTeam(channelId),
    enabled: !!profile?.id,
  });

  const member = team?.members.find((m) => m.userId === profile?.id);

  return {
    isOwner: member?.role === 'owner',
    isMember: !!member,
    hasRight: (right: string) => member?.rights.includes(right) ?? false,
    isLoading,
  };
}
