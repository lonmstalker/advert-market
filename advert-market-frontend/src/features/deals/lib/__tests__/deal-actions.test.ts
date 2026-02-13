import { describe, expect, it } from 'vitest';
import type { DealRole, DealStatus } from '../../types/deal';
import type { DealAction } from '../deal-actions';
import { getDealActions } from '../deal-actions';

describe('getDealActions', () => {
  describe('OFFER_PENDING', () => {
    it('returns cancel action for advertiser', () => {
      const actions = getDealActions('OFFER_PENDING', 'ADVERTISER');

      expect(actions).toHaveLength(1);
      expect(actions[0]).toEqual<DealAction>({
        type: 'cancel',
        i18nKey: 'deals.actions.cancel',
        variant: 'destructive',
        requiresConfirm: true,
      });
    });

    it('returns accept, counter_offer, reject for owner', () => {
      const actions = getDealActions('OFFER_PENDING', 'OWNER');

      expect(actions).toHaveLength(3);
      expect(actions.map((a) => a.type)).toEqual(['accept', 'counter_offer', 'reject']);
    });

    it('returns correct variants for owner actions', () => {
      const actions = getDealActions('OFFER_PENDING', 'OWNER');

      expect(actions[0].variant).toBe('primary');
      expect(actions[1].variant).toBe('secondary');
      expect(actions[2].variant).toBe('destructive');
    });
  });

  describe('NEGOTIATING', () => {
    it('returns reply and cancel for advertiser', () => {
      const actions = getDealActions('NEGOTIATING', 'ADVERTISER');

      expect(actions).toHaveLength(2);
      expect(actions.map((a) => a.type)).toEqual(['reply', 'cancel']);
    });

    it('returns reply and reject for owner', () => {
      const actions = getDealActions('NEGOTIATING', 'OWNER');

      expect(actions).toHaveLength(2);
      expect(actions.map((a) => a.type)).toEqual(['reply', 'reject']);
    });
  });

  describe('AWAITING_PAYMENT', () => {
    it('returns pay action for advertiser', () => {
      const actions = getDealActions('AWAITING_PAYMENT', 'ADVERTISER');

      expect(actions).toHaveLength(1);
      expect(actions[0]).toMatchObject({
        type: 'pay',
        variant: 'primary',
        requiresConfirm: false,
      });
    });

    it('returns empty array for owner', () => {
      const actions = getDealActions('AWAITING_PAYMENT', 'OWNER');

      expect(actions).toEqual([]);
    });
  });

  describe('CREATIVE_SUBMITTED', () => {
    it('returns approve and request_revision for advertiser', () => {
      const actions = getDealActions('CREATIVE_SUBMITTED', 'ADVERTISER');

      expect(actions).toHaveLength(2);
      expect(actions.map((a) => a.type)).toEqual(['approve_creative', 'request_revision']);
    });

    it('returns empty array for owner', () => {
      const actions = getDealActions('CREATIVE_SUBMITTED', 'OWNER');

      expect(actions).toEqual([]);
    });
  });

  describe('CREATIVE_APPROVED', () => {
    it('returns publish and schedule for owner', () => {
      const actions = getDealActions('CREATIVE_APPROVED', 'OWNER');

      expect(actions).toHaveLength(2);
      expect(actions.map((a) => a.type)).toEqual(['publish', 'schedule']);
    });

    it('returns empty array for advertiser', () => {
      const actions = getDealActions('CREATIVE_APPROVED', 'ADVERTISER');

      expect(actions).toEqual([]);
    });
  });

  describe('statuses with no actions defined', () => {
    const noActionStatuses: DealStatus[] = [
      'DRAFT',
      'ACCEPTED',
      'FUNDED',
      'SCHEDULED',
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
    it('reject action requires confirmation', () => {
      const actions = getDealActions('OFFER_PENDING', 'OWNER');
      const reject = actions.find((a) => a.type === 'reject');

      expect(reject?.requiresConfirm).toBe(true);
    });

    it('accept action does not require confirmation', () => {
      const actions = getDealActions('OFFER_PENDING', 'OWNER');
      const accept = actions.find((a) => a.type === 'accept');

      expect(accept?.requiresConfirm).toBe(false);
    });

    it('cancel action requires confirmation', () => {
      const actions = getDealActions('OFFER_PENDING', 'ADVERTISER');
      const cancel = actions.find((a) => a.type === 'cancel');

      expect(cancel?.requiresConfirm).toBe(true);
    });

    it('every action has a valid i18nKey', () => {
      const allActions: DealAction[] = [];
      const statusRolePairs: [DealStatus, DealRole][] = [
        ['OFFER_PENDING', 'ADVERTISER'],
        ['OFFER_PENDING', 'OWNER'],
        ['NEGOTIATING', 'ADVERTISER'],
        ['NEGOTIATING', 'OWNER'],
        ['AWAITING_PAYMENT', 'ADVERTISER'],
        ['CREATIVE_SUBMITTED', 'ADVERTISER'],
        ['CREATIVE_APPROVED', 'OWNER'],
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
