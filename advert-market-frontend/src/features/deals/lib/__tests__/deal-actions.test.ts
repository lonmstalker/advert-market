import { describe, expect, it } from 'vitest';
import type { DealRole, DealStatus } from '../../types/deal';
import type { DealAction } from '../deal-actions';
import { getDealActions } from '../deal-actions';

describe('getDealActions', () => {
  describe('OFFER_PENDING', () => {
    it('returns cancel for advertiser', () => {
      const actions = getDealActions('OFFER_PENDING', 'ADVERTISER');

      expect(actions.map((a) => a.type)).toEqual(['cancel']);
      expect(actions[0]).toMatchObject({
        targetStatus: 'CANCELLED',
        requiresConfirm: true,
      });
    });

    it('returns accept, counter_offer, reject for owner', () => {
      const actions = getDealActions('OFFER_PENDING', 'OWNER');

      expect(actions.map((a) => a.type)).toEqual(['accept', 'counter_offer', 'reject']);
      expect(actions.map((a) => a.targetStatus)).toEqual(['ACCEPTED', 'NEGOTIATING', 'CANCELLED']);
    });
  });

  describe('NEGOTIATING', () => {
    it('returns cancel for advertiser', () => {
      const actions = getDealActions('NEGOTIATING', 'ADVERTISER');
      expect(actions.map((a) => a.type)).toEqual(['cancel']);
    });

    it('returns accept, counter_offer, reject for owner', () => {
      const actions = getDealActions('NEGOTIATING', 'OWNER');
      expect(actions.map((a) => a.type)).toEqual(['accept', 'counter_offer', 'reject']);
    });
  });

  describe('ACCEPTED', () => {
    it('does not show pay, only allows cancellation while waiting auto-transition', () => {
      const advertiserActions = getDealActions('ACCEPTED', 'ADVERTISER');
      const ownerActions = getDealActions('ACCEPTED', 'OWNER');

      expect(advertiserActions.map((a) => a.type)).toEqual(['cancel']);
      expect(ownerActions.map((a) => a.type)).toEqual(['cancel']);
      expect(advertiserActions.some((a) => a.type === 'pay')).toBe(false);
      expect(ownerActions.some((a) => a.type === 'pay')).toBe(false);
    });
  });

  describe('AWAITING_PAYMENT', () => {
    it('returns pay and cancel for advertiser', () => {
      const actions = getDealActions('AWAITING_PAYMENT', 'ADVERTISER');

      expect(actions.map((a) => a.type)).toEqual(['pay', 'cancel']);
      expect(actions[0]?.targetStatus).toBeUndefined();
      expect(actions[1]).toMatchObject({ targetStatus: 'CANCELLED' });
    });

    it('returns empty array for owner', () => {
      expect(getDealActions('AWAITING_PAYMENT', 'OWNER')).toEqual([]);
    });
  });

  describe('FUNDED with creative flow blocked', () => {
    it('does not expose creative CTA and keeps cancellation only', () => {
      const advertiserActions = getDealActions('FUNDED', 'ADVERTISER');
      const ownerActions = getDealActions('FUNDED', 'OWNER');

      expect(advertiserActions.map((a) => a.type)).toEqual(['cancel']);
      expect(ownerActions.map((a) => a.type)).toEqual(['cancel']);
    });
  });

  describe('creative statuses with contract gate blocked', () => {
    it('returns no actions for CREATIVE_SUBMITTED', () => {
      expect(getDealActions('CREATIVE_SUBMITTED', 'ADVERTISER')).toEqual([]);
      expect(getDealActions('CREATIVE_SUBMITTED', 'OWNER')).toEqual([]);
    });

    it('returns no actions for CREATIVE_APPROVED', () => {
      expect(getDealActions('CREATIVE_APPROVED', 'ADVERTISER')).toEqual([]);
      expect(getDealActions('CREATIVE_APPROVED', 'OWNER')).toEqual([]);
    });
  });

  describe('SCHEDULED', () => {
    it('allows participant cancellation only', () => {
      expect(getDealActions('SCHEDULED', 'ADVERTISER').map((a) => a.type)).toEqual(['cancel']);
      expect(getDealActions('SCHEDULED', 'OWNER').map((a) => a.type)).toEqual(['cancel']);
    });
  });

  describe('statuses with no user actions', () => {
    const noActionStatuses: DealStatus[] = [
      'DRAFT',
      'PUBLISHED',
      'DELIVERY_VERIFYING',
      'COMPLETED_RELEASED',
      'DISPUTED',
      'CANCELLED',
      'EXPIRED',
      'REFUNDED',
      'PARTIALLY_REFUNDED',
    ];

    for (const status of noActionStatuses) {
      it(`returns empty array for ${status} regardless of role`, () => {
        expect(getDealActions(status, 'ADVERTISER')).toEqual([]);
        expect(getDealActions(status, 'OWNER')).toEqual([]);
      });
    }
  });

  describe('action properties', () => {
    it('cancel and reject require confirmation', () => {
      const ownerActions = getDealActions('OFFER_PENDING', 'OWNER');
      const advertiserActions = getDealActions('OFFER_PENDING', 'ADVERTISER');

      expect(ownerActions.find((a) => a.type === 'reject')?.requiresConfirm).toBe(true);
      expect(advertiserActions.find((a) => a.type === 'cancel')?.requiresConfirm).toBe(true);
    });

    it('every action has i18n key prefix', () => {
      const allActions: DealAction[] = [];
      const statusRolePairs: [DealStatus, DealRole][] = [
        ['OFFER_PENDING', 'ADVERTISER'],
        ['OFFER_PENDING', 'OWNER'],
        ['NEGOTIATING', 'ADVERTISER'],
        ['NEGOTIATING', 'OWNER'],
        ['ACCEPTED', 'ADVERTISER'],
        ['ACCEPTED', 'OWNER'],
        ['AWAITING_PAYMENT', 'ADVERTISER'],
        ['FUNDED', 'OWNER'],
      ];

      for (const [status, role] of statusRolePairs) {
        allActions.push(...getDealActions(status, role));
      }

      for (const action of allActions) {
        expect(action.i18nKey).toMatch(/^deals\.actions\./);
      }
    });
  });
});
