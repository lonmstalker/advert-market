import { describe, expect, it, vi } from 'vitest';
import type { DealEvent, DealStatus } from '../../types/deal';
import { DEAL_STATUSES } from '../../types/deal';
import type { StatusColor } from '../deal-status';
import { buildTimelineSteps, getPollingInterval, getStatusConfig, statusBgVar, statusColorVar } from '../deal-status';

function mockT(key: string): string {
  return key;
}

function createEvent(overrides: Partial<DealEvent> & Pick<DealEvent, 'status' | 'createdAt'>): DealEvent {
  return {
    id: crypto.randomUUID(),
    type: 'STATUS_CHANGE',
    actorRole: 'SYSTEM',
    message: null,
    ...overrides,
  };
}

describe('getStatusConfig', () => {
  it('returns config for every known status', () => {
    for (const status of DEAL_STATUSES) {
      const config = getStatusConfig(status);
      expect(config).toBeDefined();
      expect(config.color).toBeTruthy();
      expect(config.i18nKey).toMatch(/^deals\.status\./);
    }
  });

  it('maps DRAFT to accent color', () => {
    expect(getStatusConfig('DRAFT').color).toBe('accent');
  });

  it('maps AWAITING_PAYMENT to warning color', () => {
    expect(getStatusConfig('AWAITING_PAYMENT').color).toBe('warning');
  });

  it('maps COMPLETED_RELEASED to success color', () => {
    expect(getStatusConfig('COMPLETED_RELEASED').color).toBe('success');
  });

  it('maps DISPUTED to destructive color', () => {
    expect(getStatusConfig('DISPUTED').color).toBe('destructive');
  });

  it('maps CANCELLED to secondary color', () => {
    expect(getStatusConfig('CANCELLED').color).toBe('secondary');
  });

  it('maps EXPIRED to secondary color', () => {
    expect(getStatusConfig('EXPIRED').color).toBe('secondary');
  });

  it('maps REFUNDED to secondary color', () => {
    expect(getStatusConfig('REFUNDED').color).toBe('secondary');
  });

  it('maps PARTIALLY_REFUNDED to secondary color', () => {
    expect(getStatusConfig('PARTIALLY_REFUNDED').color).toBe('secondary');
  });
});

describe('buildTimelineSteps', () => {
  it('returns happy-path steps with completed, active, and pending states', () => {
    const events: DealEvent[] = [
      createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' }),
      createEvent({ status: 'OFFER_PENDING', createdAt: '2026-01-01T01:00:00Z' }),
    ];

    const steps = buildTimelineSteps(events, 'NEGOTIATING', mockT);

    const completed = steps.filter((s) => s.state === 'completed');
    const active = steps.filter((s) => s.state === 'active');
    const pending = steps.filter((s) => s.state === 'pending');

    expect(completed).toHaveLength(2);
    expect(active).toHaveLength(1);
    expect(active[0].status).toBe('NEGOTIATING');
    expect(pending.length).toBeGreaterThan(0);
  });

  it('sets timestamps on completed steps from events', () => {
    const events: DealEvent[] = [
      createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' }),
      createEvent({ status: 'OFFER_PENDING', createdAt: '2026-01-02T00:00:00Z' }),
    ];

    const steps = buildTimelineSteps(events, 'NEGOTIATING', mockT);

    expect(steps[0].timestamp).toBe('2026-01-01T00:00:00Z');
    expect(steps[1].timestamp).toBe('2026-01-02T00:00:00Z');
  });

  it('does not set timestamp on active step', () => {
    const events: DealEvent[] = [createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' })];

    const steps = buildTimelineSteps(events, 'OFFER_PENDING', mockT);
    const active = steps.find((s) => s.state === 'active');

    expect(active?.timestamp).toBeUndefined();
  });

  it('sets description on active step via t() function', () => {
    const events: DealEvent[] = [createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' })];

    const steps = buildTimelineSteps(events, 'OFFER_PENDING', mockT);
    const active = steps.find((s) => s.state === 'active');

    expect(active?.description).toBe('deals.status.offerPendingDesc');
  });

  it('does not set description on pending steps', () => {
    const events: DealEvent[] = [];
    const steps = buildTimelineSteps(events, 'DRAFT', mockT);
    const pending = steps.filter((s) => s.state === 'pending');

    for (const step of pending) {
      expect(step.description).toBeUndefined();
    }
  });

  it('uses first event timestamp when duplicate statuses exist', () => {
    const events: DealEvent[] = [
      createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' }),
      createEvent({ status: 'DRAFT', createdAt: '2026-01-01T12:00:00Z' }),
    ];

    const steps = buildTimelineSteps(events, 'OFFER_PENDING', mockT);
    const draft = steps.find((s) => s.status === 'DRAFT');

    expect(draft?.timestamp).toBe('2026-01-01T00:00:00Z');
  });

  describe('terminal statuses (non-completed)', () => {
    it('shows completed events plus terminal status as active for CANCELLED', () => {
      const events: DealEvent[] = [
        createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' }),
        createEvent({ status: 'OFFER_PENDING', createdAt: '2026-01-02T00:00:00Z' }),
      ];

      const steps = buildTimelineSteps(events, 'CANCELLED', mockT);

      expect(steps).toHaveLength(3);
      expect(steps[0]).toMatchObject({ status: 'DRAFT', state: 'completed' });
      expect(steps[1]).toMatchObject({ status: 'OFFER_PENDING', state: 'completed' });
      expect(steps[2]).toMatchObject({ status: 'CANCELLED', state: 'active' });
    });

    it('includes description on terminal active step', () => {
      const events: DealEvent[] = [createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' })];

      const steps = buildTimelineSteps(events, 'DISPUTED', mockT);
      const terminal = steps.find((s) => s.status === 'DISPUTED');

      expect(terminal?.description).toBe('deals.status.disputedDesc');
    });

    it('does not include pending steps for terminal statuses', () => {
      const events: DealEvent[] = [createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' })];

      const steps = buildTimelineSteps(events, 'EXPIRED', mockT);
      const pending = steps.filter((s) => s.state === 'pending');

      expect(pending).toHaveLength(0);
    });
  });

  describe('COMPLETED_RELEASED', () => {
    it('marks all steps as completed with no pending steps', () => {
      const events: DealEvent[] = [
        createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' }),
        createEvent({ status: 'OFFER_PENDING', createdAt: '2026-01-02T00:00:00Z' }),
        createEvent({ status: 'NEGOTIATING', createdAt: '2026-01-03T00:00:00Z' }),
        createEvent({ status: 'ACCEPTED', createdAt: '2026-01-04T00:00:00Z' }),
        createEvent({ status: 'AWAITING_PAYMENT', createdAt: '2026-01-05T00:00:00Z' }),
        createEvent({ status: 'FUNDED', createdAt: '2026-01-06T00:00:00Z' }),
        createEvent({ status: 'CREATIVE_SUBMITTED', createdAt: '2026-01-07T00:00:00Z' }),
        createEvent({ status: 'CREATIVE_APPROVED', createdAt: '2026-01-08T00:00:00Z' }),
        createEvent({ status: 'SCHEDULED', createdAt: '2026-01-09T00:00:00Z' }),
        createEvent({ status: 'PUBLISHED', createdAt: '2026-01-10T00:00:00Z' }),
        createEvent({ status: 'DELIVERY_VERIFYING', createdAt: '2026-01-11T00:00:00Z' }),
        createEvent({ status: 'COMPLETED_RELEASED', createdAt: '2026-01-12T00:00:00Z' }),
      ];

      const steps = buildTimelineSteps(events, 'COMPLETED_RELEASED', mockT);
      const pending = steps.filter((s) => s.state === 'pending');
      const active = steps.filter((s) => s.state === 'active');
      const completed = steps.filter((s) => s.state === 'completed');

      expect(pending).toHaveLength(0);
      expect(active).toHaveLength(0);
      expect(completed).toHaveLength(12);
    });

    it('sets timestamp on the COMPLETED_RELEASED step', () => {
      const events: DealEvent[] = [createEvent({ status: 'COMPLETED_RELEASED', createdAt: '2026-01-12T00:00:00Z' })];

      const steps = buildTimelineSteps(events, 'COMPLETED_RELEASED', mockT);
      const completedStep = steps.find((s) => s.status === 'COMPLETED_RELEASED');

      expect(completedStep?.timestamp).toBe('2026-01-12T00:00:00Z');
    });

    it('does not set description on COMPLETED_RELEASED step', () => {
      const events: DealEvent[] = [createEvent({ status: 'COMPLETED_RELEASED', createdAt: '2026-01-12T00:00:00Z' })];

      const steps = buildTimelineSteps(events, 'COMPLETED_RELEASED', mockT);
      const completedStep = steps.find((s) => s.status === 'COMPLETED_RELEASED');

      expect(completedStep?.description).toBeUndefined();
    });
  });

  it('returns labels from the t() function', () => {
    const t = vi.fn((key: string) => `translated:${key}`);
    const events: DealEvent[] = [createEvent({ status: 'DRAFT', createdAt: '2026-01-01T00:00:00Z' })];

    const steps = buildTimelineSteps(events, 'OFFER_PENDING', t);

    expect(steps[0].label).toBe('translated:deals.status.draft');
    expect(steps[1].label).toBe('translated:deals.status.offerPending');
    expect(t).toHaveBeenCalled();
  });
});

describe('getPollingInterval', () => {
  it('returns 10000ms for AWAITING_PAYMENT', () => {
    expect(getPollingInterval('AWAITING_PAYMENT')).toBe(10_000);
  });

  it('returns 10000ms for DELIVERY_VERIFYING', () => {
    expect(getPollingInterval('DELIVERY_VERIFYING')).toBe(10_000);
  });

  it('returns 30000ms for PUBLISHED', () => {
    expect(getPollingInterval('PUBLISHED')).toBe(30_000);
  });

  it('returns false for statuses that should not poll', () => {
    const nonPollingStatuses: DealStatus[] = [
      'DRAFT',
      'OFFER_PENDING',
      'NEGOTIATING',
      'ACCEPTED',
      'FUNDED',
      'CREATIVE_SUBMITTED',
      'CREATIVE_APPROVED',
      'SCHEDULED',
      'COMPLETED_RELEASED',
      'DISPUTED',
      'CANCELLED',
      'EXPIRED',
      'REFUNDED',
      'PARTIALLY_REFUNDED',
    ];

    for (const status of nonPollingStatuses) {
      expect(getPollingInterval(status)).toBe(false);
    }
  });
});

describe('statusColorVar', () => {
  it('returns CSS variable for accent', () => {
    expect(statusColorVar('accent')).toBe('var(--color-accent-primary)');
  });

  it('returns CSS variable for warning', () => {
    expect(statusColorVar('warning')).toBe('var(--color-state-warning)');
  });

  it('returns CSS variable for success', () => {
    expect(statusColorVar('success')).toBe('var(--color-state-success)');
  });

  it('returns CSS variable for destructive', () => {
    expect(statusColorVar('destructive')).toBe('var(--color-state-destructive)');
  });

  it('returns CSS variable for secondary', () => {
    expect(statusColorVar('secondary')).toBe('var(--color-foreground-secondary)');
  });
});

describe('statusBgVar', () => {
  it('returns soft background variable for each color', () => {
    const colors: StatusColor[] = ['accent', 'warning', 'success', 'destructive', 'secondary'];

    for (const color of colors) {
      const result = statusBgVar(color);
      expect(result).toMatch(/^var\(--am-soft-/);
    }
  });

  it('returns correct background for accent', () => {
    expect(statusBgVar('accent')).toBe('var(--am-soft-accent-bg)');
  });

  it('returns correct background for destructive', () => {
    expect(statusBgVar('destructive')).toBe('var(--am-soft-destructive-bg)');
  });
});
