import type { PostType, PricingRule } from '@/features/channels';
import { parseTonToNano } from '@/shared/lib/ton-format';

export type DealPostTypeOption = {
  type: string;
  label: string;
  pricingRuleId: number | null;
  priceNano: number | null;
};

export function buildPostTypeOptions(
  postTypes: PostType[],
  pricingRules: PricingRule[],
  language: string,
): DealPostTypeOption[] {
  const normalizedLang = language.toLowerCase().startsWith('ru') ? 'ru' : 'en';
  const sourceTypes =
    postTypes.length > 0
      ? postTypes.map((postType) => ({
          type: postType.type,
          label: postType.labels[normalizedLang] ?? postType.labels.en ?? postType.type,
        }))
      : [...new Set(pricingRules.flatMap((rule) => rule.postTypes))].map((type) => ({ type, label: type }));

  return sourceTypes.map((postType) => {
    const matchedRule = pricingRules.find((rule) => rule.postTypes.includes(postType.type));
    return {
      type: postType.type,
      label: postType.label,
      pricingRuleId: matchedRule?.id ?? null,
      priceNano: matchedRule?.priceNano ?? null,
    };
  });
}

export function resolveCreateDealPricing(input: {
  selectedPostType: string | null;
  options: DealPostTypeOption[];
  customAmountTon: string;
}): { amountNano: number; pricingRuleId?: number } {
  if (!input.selectedPostType) {
    throw new Error('Post type is required');
  }

  const selectedOption = input.options.find((option) => option.type === input.selectedPostType);
  if (!selectedOption) {
    throw new Error('Selected post type is unknown');
  }

  if (selectedOption.pricingRuleId != null && selectedOption.priceNano != null) {
    return {
      amountNano: selectedOption.priceNano,
      pricingRuleId: selectedOption.pricingRuleId,
    };
  }

  const customAmount = input.customAmountTon.trim();
  if (!customAmount) {
    throw new Error('Custom amount is required when post type has no pricing rule');
  }

  return {
    amountNano: Number(parseTonToNano(customAmount)),
  };
}
