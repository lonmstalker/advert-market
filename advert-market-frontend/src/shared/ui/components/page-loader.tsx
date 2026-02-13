import { Spinner } from '@telegram-tools/ui-kit';

export function PageLoader() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '60vh' }}>
      <Spinner size="32px" color="accent" />
    </div>
  );
}
