import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { beforeEach, describe, expect, it } from 'vitest';

let mockProfile: { id: number } | null = { id: 42 };
const mockFetchChannelTeam = vi.fn();

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

vi.mock('@/shared/hooks/use-auth', () => ({
  useAuth: () => ({
    profile: mockProfile,
    isAuthenticated: !!mockProfile,
    isLoading: false,
    invalidateProfile: vi.fn(),
  }),
}));

vi.mock('../../api/channels', () => ({
  fetchChannelTeam: (...args: unknown[]) => mockFetchChannelTeam(...args),
}));

import { useChannelRights } from '../useChannelRights';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
    },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('useChannelRights', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockProfile = { id: 42 };
  });

  describe('loading state', () => {
    it('starts in loading state', () => {
      mockFetchChannelTeam.mockReturnValue(new Promise(() => {}));

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.isOwner).toBe(false);
      expect(result.current.isMember).toBe(false);
    });

    it('does not fetch when profile is null', () => {
      mockProfile = null;
      mockFetchChannelTeam.mockResolvedValue({ members: [] });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      expect(result.current.isLoading).toBe(false);
      expect(mockFetchChannelTeam).not.toHaveBeenCalled();
    });
  });

  describe('owner detection', () => {
    it('returns isOwner=true when user is the owner', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [{ userId: 42, role: 'owner', rights: ['manage_deals', 'edit_channel'] }],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isOwner).toBe(true);
      expect(result.current.isMember).toBe(true);
    });

    it('returns isOwner=false when user is a manager', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [{ userId: 42, role: 'manager', rights: ['manage_deals'] }],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isOwner).toBe(false);
      expect(result.current.isMember).toBe(true);
    });
  });

  describe('membership detection', () => {
    it('returns isMember=true when user is in the team', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [
          { userId: 10, role: 'owner', rights: ['edit_channel'] },
          { userId: 42, role: 'manager', rights: ['manage_deals'] },
        ],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isMember).toBe(true);
    });

    it('returns isMember=false when user is not in the team', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [
          { userId: 10, role: 'owner', rights: ['edit_channel'] },
          { userId: 99, role: 'manager', rights: ['manage_deals'] },
        ],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isMember).toBe(false);
      expect(result.current.isOwner).toBe(false);
    });

    it('returns isMember=false when team is empty', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isMember).toBe(false);
    });
  });

  describe('hasRight', () => {
    it('returns true for a right the member has', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [{ userId: 42, role: 'manager', rights: ['manage_deals', 'view_stats'] }],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.hasRight('manage_deals')).toBe(true);
      expect(result.current.hasRight('view_stats')).toBe(true);
    });

    it('returns false for a right the member does not have', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [{ userId: 42, role: 'manager', rights: ['manage_deals'] }],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.hasRight('edit_channel')).toBe(false);
    });

    it('returns false when user is not a member', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [{ userId: 99, role: 'owner', rights: ['manage_deals', 'edit_channel'] }],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.hasRight('manage_deals')).toBe(false);
    });

    it('returns false when team is empty', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.hasRight('manage_deals')).toBe(false);
    });

    it('returns false when data has not loaded yet', () => {
      mockFetchChannelTeam.mockReturnValue(new Promise(() => {}));

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      expect(result.current.hasRight('manage_deals')).toBe(false);
    });

    it('returns false when profile is null', () => {
      mockProfile = null;
      mockFetchChannelTeam.mockResolvedValue({
        members: [{ userId: 42, role: 'owner', rights: ['manage_deals'] }],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      expect(result.current.hasRight('manage_deals')).toBe(false);
    });

    it('checks rights with empty rights array', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [{ userId: 42, role: 'manager', rights: [] }],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isMember).toBe(true);
      expect(result.current.hasRight('manage_deals')).toBe(false);
    });
  });

  describe('query key', () => {
    it('passes channelId to fetchChannelTeam', async () => {
      mockFetchChannelTeam.mockResolvedValue({ members: [] });

      renderHook(() => useChannelRights(777), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(mockFetchChannelTeam).toHaveBeenCalledWith(777);
      });
    });
  });

  describe('multiple members in team', () => {
    it('correctly identifies the current user among multiple members', async () => {
      mockFetchChannelTeam.mockResolvedValue({
        members: [
          { userId: 1, role: 'owner', rights: ['edit_channel', 'manage_deals'] },
          { userId: 42, role: 'manager', rights: ['view_stats'] },
          { userId: 99, role: 'manager', rights: ['manage_deals', 'view_stats'] },
        ],
      });

      const { result } = renderHook(() => useChannelRights(1), {
        wrapper: createWrapper(),
      });

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isOwner).toBe(false);
      expect(result.current.isMember).toBe(true);
      expect(result.current.hasRight('view_stats')).toBe(true);
      expect(result.current.hasRight('manage_deals')).toBe(false);
      expect(result.current.hasRight('edit_channel')).toBe(false);
    });
  });
});
