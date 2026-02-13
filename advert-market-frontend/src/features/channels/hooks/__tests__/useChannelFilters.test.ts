import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';

let mockSearchParams = new URLSearchParams();
const mockSetSearchParams = vi.fn();

vi.mock('react-router', () => ({
  useSearchParams: () => [mockSearchParams, mockSetSearchParams],
}));

import { useChannelFilters } from '../useChannelFilters';

describe('useChannelFilters', () => {
  beforeEach(() => {
    mockSearchParams = new URLSearchParams();
    mockSetSearchParams.mockClear();
  });

  describe('initial state', () => {
    it('returns empty filters when no search params', () => {
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters).toEqual({
        q: undefined,
        category: undefined,
        categories: undefined,
        languages: undefined,
        minSubs: undefined,
        maxSubs: undefined,
        minPrice: undefined,
        maxPrice: undefined,
        sort: undefined,
      });
    });

    it('returns activeFilterCount=0 when no filters set', () => {
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(0);
    });
  });

  describe('filters parsing from search params', () => {
    it('parses q parameter', () => {
      mockSearchParams = new URLSearchParams('q=crypto');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.q).toBe('crypto');
    });

    it('parses category parameter', () => {
      mockSearchParams = new URLSearchParams('category=tech');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.category).toBe('tech');
    });

    it('parses categories as comma-separated array', () => {
      mockSearchParams = new URLSearchParams('categories=tech,crypto,finance');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.categories).toEqual(['tech', 'crypto', 'finance']);
    });

    it('parses languages as comma-separated array', () => {
      mockSearchParams = new URLSearchParams('languages=en,ru');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.languages).toEqual(['en', 'ru']);
    });

    it('parses numeric minSubs and maxSubs', () => {
      mockSearchParams = new URLSearchParams('minSubs=1000&maxSubs=50000');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.minSubs).toBe(1000);
      expect(result.current.filters.maxSubs).toBe(50000);
    });

    it('parses numeric minPrice and maxPrice', () => {
      mockSearchParams = new URLSearchParams('minPrice=100&maxPrice=5000');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.minPrice).toBe(100);
      expect(result.current.filters.maxPrice).toBe(5000);
    });

    it('parses valid sort value', () => {
      mockSearchParams = new URLSearchParams('sort=subscribers');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.sort).toBe('subscribers');
    });

    it('ignores invalid sort value', () => {
      mockSearchParams = new URLSearchParams('sort=invalid_sort');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.sort).toBeUndefined();
    });

    it('returns undefined for empty categories string', () => {
      mockSearchParams = new URLSearchParams('categories=');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.categories).toBeUndefined();
    });

    it('returns undefined for empty q string', () => {
      mockSearchParams = new URLSearchParams('q=');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.filters.q).toBeUndefined();
    });

    it('accepts all valid sort values', () => {
      for (const sortValue of ['relevance', 'subscribers', 'price_asc', 'price_desc', 'er']) {
        mockSearchParams = new URLSearchParams(`sort=${sortValue}`);
        const { result } = renderHook(() => useChannelFilters());
        expect(result.current.filters.sort).toBe(sortValue);
      }
    });
  });

  describe('setFilters', () => {
    it('calls setSearchParams with replace option', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({ q: 'test' });
      });

      expect(mockSetSearchParams).toHaveBeenCalledWith(expect.any(Function), { replace: true });
    });

    it('sets q parameter correctly', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({ q: 'crypto' });
      });

      const updaterFn = mockSetSearchParams.mock.calls[0][0];
      const params = updaterFn(new URLSearchParams());

      expect(params.get('q')).toBe('crypto');
    });

    it('sets numeric parameters as strings', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({ minSubs: 1000, maxSubs: 50000 });
      });

      const updaterFn = mockSetSearchParams.mock.calls[0][0];
      const params = updaterFn(new URLSearchParams());

      expect(params.get('minSubs')).toBe('1000');
      expect(params.get('maxSubs')).toBe('50000');
    });

    it('serializes categories array as comma-separated string', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({ categories: ['tech', 'crypto'] });
      });

      const updaterFn = mockSetSearchParams.mock.calls[0][0];
      const params = updaterFn(new URLSearchParams());

      expect(params.get('categories')).toBe('tech,crypto');
    });

    it('serializes languages array as comma-separated string', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({ languages: ['en', 'ru', 'de'] });
      });

      const updaterFn = mockSetSearchParams.mock.calls[0][0];
      const params = updaterFn(new URLSearchParams());

      expect(params.get('languages')).toBe('en,ru,de');
    });

    it('removes params with undefined values', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({ q: undefined, category: undefined });
      });

      const updaterFn = mockSetSearchParams.mock.calls[0][0];
      const existingParams = new URLSearchParams('q=old&category=oldcat');
      const params = updaterFn(existingParams);

      expect(params.has('q')).toBe(false);
      expect(params.has('category')).toBe(false);
    });

    it('removes empty arrays', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({ categories: [] });
      });

      const updaterFn = mockSetSearchParams.mock.calls[0][0];
      const existingParams = new URLSearchParams('categories=tech,crypto');
      const params = updaterFn(existingParams);

      expect(params.has('categories')).toBe(false);
    });

    it('sets sort parameter', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({ sort: 'price_asc' });
      });

      const updaterFn = mockSetSearchParams.mock.calls[0][0];
      const params = updaterFn(new URLSearchParams());

      expect(params.get('sort')).toBe('price_asc');
    });

    it('sets multiple filters at once', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.setFilters({
          q: 'test',
          category: 'tech',
          minSubs: 500,
          sort: 'subscribers',
        });
      });

      const updaterFn = mockSetSearchParams.mock.calls[0][0];
      const params = updaterFn(new URLSearchParams());

      expect(params.get('q')).toBe('test');
      expect(params.get('category')).toBe('tech');
      expect(params.get('minSubs')).toBe('500');
      expect(params.get('sort')).toBe('subscribers');
    });
  });

  describe('resetFilters', () => {
    it('calls setSearchParams with empty params and replace option', () => {
      const { result } = renderHook(() => useChannelFilters());

      act(() => {
        result.current.resetFilters();
      });

      expect(mockSetSearchParams).toHaveBeenCalledWith({}, { replace: true });
    });
  });

  describe('activeFilterCount', () => {
    it('counts category filter', () => {
      mockSearchParams = new URLSearchParams('category=tech');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(1);
    });

    it('counts categories array filter', () => {
      mockSearchParams = new URLSearchParams('categories=tech,crypto');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(1);
    });

    it('counts languages filter', () => {
      mockSearchParams = new URLSearchParams('languages=en,ru');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(1);
    });

    it('counts subscriber range as one filter', () => {
      mockSearchParams = new URLSearchParams('minSubs=1000&maxSubs=50000');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(1);
    });

    it('counts only minSubs as subscriber filter', () => {
      mockSearchParams = new URLSearchParams('minSubs=1000');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(1);
    });

    it('counts price range as one filter', () => {
      mockSearchParams = new URLSearchParams('minPrice=100&maxPrice=5000');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(1);
    });

    it('counts non-relevance sort as filter', () => {
      mockSearchParams = new URLSearchParams('sort=subscribers');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(1);
    });

    it('does not count relevance sort as filter', () => {
      mockSearchParams = new URLSearchParams('sort=relevance');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(0);
    });

    it('does not count q as active filter', () => {
      mockSearchParams = new URLSearchParams('q=search');
      const { result } = renderHook(() => useChannelFilters());

      expect(result.current.activeFilterCount).toBe(0);
    });

    it('counts all distinct active filters', () => {
      mockSearchParams = new URLSearchParams(
        'category=tech&languages=en,ru&minSubs=1000&minPrice=100&sort=subscribers',
      );
      const { result } = renderHook(() => useChannelFilters());

      // category=1, languages=1, subs(minSubs)=1, price(minPrice)=1, sort=1
      expect(result.current.activeFilterCount).toBe(5);
    });

    it('counts maximum possible filters', () => {
      mockSearchParams = new URLSearchParams('category=tech&languages=en&minSubs=100&maxPrice=9999&sort=price_desc');
      const { result } = renderHook(() => useChannelFilters());

      // category=1, languages=1, subs(minSubs)=1, price(maxPrice)=1, sort=1
      expect(result.current.activeFilterCount).toBe(5);
    });
  });
});
