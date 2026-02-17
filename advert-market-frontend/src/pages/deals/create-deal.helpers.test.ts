import { describe, expect, it } from 'vitest';
import { buildPostTypeOptions, resolveCreateDealPricing } from './create-deal.helpers';

describe('create-deal helpers', () => {
  it('builds post type options from post-types catalog and marks rule-backed types', () => {
    const options = buildPostTypeOptions(
      [
        { type: 'NATIVE', labels: { en: 'Native', ru: 'Нативная' } },
        { type: 'STORY', labels: { en: 'Story', ru: 'Сторис' } },
      ],
      [
        {
          id: 10,
          channelId: 1,
          name: 'Native package',
          postTypes: ['NATIVE'],
          priceNano: 2_000_000_000,
          isActive: true,
          sortOrder: 0,
        },
      ],
      'en',
    );

    expect(options).toEqual([
      {
        type: 'NATIVE',
        label: 'Native',
        pricingRuleId: 10,
        priceNano: 2_000_000_000,
      },
      {
        type: 'STORY',
        label: 'Story',
        pricingRuleId: null,
        priceNano: null,
      },
    ]);
  });

  it('uses pricing rule amount when selected post type has a channel rule', () => {
    const pricing = resolveCreateDealPricing({
      selectedPostType: 'NATIVE',
      options: [
        {
          type: 'NATIVE',
          label: 'Native',
          pricingRuleId: 10,
          priceNano: 2_000_000_000,
        },
      ],
      customAmountTon: '',
    });

    expect(pricing).toEqual({
      amountNano: 2_000_000_000,
      pricingRuleId: 10,
    });
  });

  it('uses custom amount when selected type has no rule', () => {
    const pricing = resolveCreateDealPricing({
      selectedPostType: 'STORY',
      options: [
        {
          type: 'STORY',
          label: 'Story',
          pricingRuleId: null,
          priceNano: null,
        },
      ],
      customAmountTon: '3.5',
    });

    expect(pricing).toEqual({
      amountNano: 3_500_000_000,
      pricingRuleId: undefined,
    });
  });

  it('throws on invalid custom amount for post type without rule', () => {
    expect(() =>
      resolveCreateDealPricing({
        selectedPostType: 'STORY',
        options: [
          {
            type: 'STORY',
            label: 'Story',
            pricingRuleId: null,
            priceNano: null,
          },
        ],
        customAmountTon: '',
      }),
    ).toThrowError('Custom amount is required when post type has no pricing rule');
  });
});
