import { Text } from '@telegram-tools/ui-kit';
import { formatFiat } from '@/shared/lib/fiat-format';
import { formatTon } from '@/shared/lib/ton-format';

type FormattedPriceSize = 'sm' | 'md' | 'lg';

type FormattedPriceProps = {
  nanoTon: number;
  showFiat?: boolean;
  size?: FormattedPriceSize;
};

const sizeConfig: Record<FormattedPriceSize, { tonType: string; fiatType: string; fiatColor: string }> = {
  sm: { tonType: 'caption1', fiatType: 'caption1', fiatColor: 'tertiary' },
  md: { tonType: 'callout', fiatType: 'caption1', fiatColor: 'tertiary' },
  lg: { tonType: 'title1', fiatType: 'caption1', fiatColor: 'secondary' },
};

export function FormattedPrice({ nanoTon, showFiat = true, size = 'md' }: FormattedPriceProps) {
  const config = sizeConfig[size];

  return (
    <>
      <Text type={config.tonType as 'callout'} weight={size === 'lg' ? 'bold' : 'bold'}>
        <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatTon(nanoTon)}</span>
      </Text>
      {showFiat && (
        <Text type={config.fiatType as 'caption1'} color={config.fiatColor as 'tertiary'}>
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>{formatFiat(nanoTon)}</span>
        </Text>
      )}
    </>
  );
}
