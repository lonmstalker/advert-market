import { renderWithProviders, screen } from '@/test/test-utils';
import { BottomTabs } from './bottom-tabs';

describe('BottomTabs', () => {
  it('renders 4 navigation links with correct paths', () => {
    renderWithProviders(<BottomTabs />);
    expect(screen.getByRole('link', { name: /catalog/i })).toHaveAttribute('href', '/catalog');
    expect(screen.getByRole('link', { name: /deals/i })).toHaveAttribute('href', '/deals');
    expect(screen.getByRole('link', { name: /wallet/i })).toHaveAttribute('href', '/wallet');
    expect(screen.getByRole('link', { name: /profile/i })).toHaveAttribute('href', '/profile');
  });

  it('renders i18n labels', () => {
    renderWithProviders(<BottomTabs />);
    expect(screen.getByText('Catalog')).toBeInTheDocument();
    expect(screen.getByText('Deals')).toBeInTheDocument();
    expect(screen.getByText('Wallet')).toBeInTheDocument();
    expect(screen.getByText('Profile')).toBeInTheDocument();
  });

  it('renders exactly 4 links', () => {
    renderWithProviders(<BottomTabs />);
    expect(screen.getAllByRole('link')).toHaveLength(4);
  });

  it('marks active tab based on current route', () => {
    renderWithProviders(<BottomTabs />, { initialEntries: ['/catalog'] });
    expect(screen.getByRole('link', { name: /catalog/i })).toHaveAttribute('aria-current', 'page');
  });
});
