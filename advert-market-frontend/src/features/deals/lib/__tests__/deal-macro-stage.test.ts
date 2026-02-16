import { describe, expect, it } from 'vitest';
import { DEAL_STATUSES, type DealStatus } from '../../types/deal';
import { getMacroStage, getTimelineConfig, type MiniTimelineStep } from '../deal-macro-stage';

describe('getMacroStage', () => {
  it('covers every known DealStatus', () => {
    for (const status of DEAL_STATUSES) {
      const stage = getMacroStage(status);
      expect(stage).toBeTruthy();
    }
  });

  const negotiationStatuses: DealStatus[] = ['DRAFT', 'OFFER_PENDING', 'NEGOTIATING', 'ACCEPTED'];
  for (const status of negotiationStatuses) {
    it(`maps ${status} to negotiation`, () => {
      expect(getMacroStage(status)).toBe('negotiation');
    });
  }

  const paymentStatuses: DealStatus[] = ['AWAITING_PAYMENT', 'FUNDED', 'CREATIVE_SUBMITTED', 'CREATIVE_APPROVED'];
  for (const status of paymentStatuses) {
    it(`maps ${status} to payment`, () => {
      expect(getMacroStage(status)).toBe('payment');
    });
  }

  const publicationStatuses: DealStatus[] = ['SCHEDULED', 'PUBLISHED', 'DELIVERY_VERIFYING'];
  for (const status of publicationStatuses) {
    it(`maps ${status} to publication`, () => {
      expect(getMacroStage(status)).toBe('publication');
    });
  }

  const terminalStatuses: DealStatus[] = [
    'COMPLETED_RELEASED',
    'DISPUTED',
    'CANCELLED',
    'EXPIRED',
    'REFUNDED',
    'PARTIALLY_REFUNDED',
  ];
  for (const status of terminalStatuses) {
    it(`maps ${status} to terminal`, () => {
      expect(getMacroStage(status)).toBe('terminal');
    });
  }
});

describe('getTimelineConfig', () => {
  it('returns exactly 3 steps', () => {
    const config = getTimelineConfig('DRAFT');
    expect(config).toHaveLength(3);
  });

  it('returns negotiation, payment, publication labels in order', () => {
    const config = getTimelineConfig('DRAFT');
    expect(config.map((s) => s.stage)).toEqual(['negotiation', 'payment', 'publication']);
  });

  it('returns i18n keys for each step', () => {
    const config = getTimelineConfig('DRAFT');
    expect(config[0].i18nKey).toBe('deals.macro.negotiation');
    expect(config[1].i18nKey).toBe('deals.macro.payment');
    expect(config[2].i18nKey).toBe('deals.macro.publication');
  });

  describe('negotiation stage statuses', () => {
    const statuses: DealStatus[] = ['DRAFT', 'OFFER_PENDING', 'NEGOTIATING', 'ACCEPTED'];

    for (const status of statuses) {
      it(`${status}: negotiation=active, payment=pending, publication=pending`, () => {
        const config = getTimelineConfig(status);
        expect(states(config)).toEqual(['active', 'pending', 'pending']);
      });
    }
  });

  describe('payment stage statuses', () => {
    const statuses: DealStatus[] = ['AWAITING_PAYMENT', 'FUNDED', 'CREATIVE_SUBMITTED', 'CREATIVE_APPROVED'];

    for (const status of statuses) {
      it(`${status}: negotiation=completed, payment=active, publication=pending`, () => {
        const config = getTimelineConfig(status);
        expect(states(config)).toEqual(['completed', 'active', 'pending']);
      });
    }
  });

  describe('publication stage statuses', () => {
    const statuses: DealStatus[] = ['SCHEDULED', 'PUBLISHED', 'DELIVERY_VERIFYING'];

    for (const status of statuses) {
      it(`${status}: negotiation=completed, payment=completed, publication=active`, () => {
        const config = getTimelineConfig(status);
        expect(states(config)).toEqual(['completed', 'completed', 'active']);
      });
    }
  });

  describe('terminal statuses', () => {
    it('COMPLETED_RELEASED: all 3 steps completed', () => {
      const config = getTimelineConfig('COMPLETED_RELEASED');
      expect(states(config)).toEqual(['completed', 'completed', 'completed']);
    });

    const abnormalTerminals: DealStatus[] = ['DISPUTED', 'CANCELLED', 'EXPIRED', 'REFUNDED', 'PARTIALLY_REFUNDED'];
    for (const status of abnormalTerminals) {
      it(`${status}: all 3 steps completed (deal is over)`, () => {
        const config = getTimelineConfig(status);
        expect(states(config)).toEqual(['completed', 'completed', 'completed']);
      });
    }
  });
});

function states(config: MiniTimelineStep[]): string[] {
  return config.map((s) => s.state);
}
