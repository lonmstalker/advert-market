type NegotiateSheetContentProps = {
  currentPriceNano: number;
  onSubmit: (priceNano: number, message?: string) => void;
  isPending: boolean;
};

let negotiateSheetProps: NegotiateSheetContentProps | null = null;

export function setNegotiateSheetProps(props: NegotiateSheetContentProps) {
  negotiateSheetProps = props;
}

export function getNegotiateSheetProps() {
  return negotiateSheetProps;
}

export type { NegotiateSheetContentProps };
