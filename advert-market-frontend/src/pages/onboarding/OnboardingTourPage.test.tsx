import { act } from '@testing-library/react';
import { Route, Routes } from 'react-router';
import { useOnboardingStore } from '@/features/onboarding';
import { renderWithProviders, screen, waitFor } from '@/test/test-utils';
import OnboardingTourPage from './OnboardingTourPage';

vi.mock('@telegram-apps/sdk-react', () => ({
  retrieveRawInitData: vi.fn(() => 'mock-init-data'),
}));

function completeTask(index: number) {
  act(() => {
    useOnboardingStore.getState().completeTourTask(index);
  });
}

describe('OnboardingTourPage', () => {
  beforeEach(() => {
    act(() => {
      useOnboardingStore.getState().reset();
      useOnboardingStore.getState().toggleInterest('advertiser');
    });
    sessionStorage.setItem('access_token', 'test-token');
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  function renderPage() {
    return renderWithProviders(
      <Routes>
        <Route path="/onboarding/tour" element={<OnboardingTourPage />} />
        <Route path="/onboarding/interest" element={<div>interest-page</div>} />
        <Route path="/catalog" element={<div>catalog-page</div>} />
        <Route path="/profile/channels/new" element={<div>owner-entry-page</div>} />
      </Routes>,
      { initialEntries: ['/onboarding/tour'] },
    );
  }

  it('renders first slide with interactive catalog mockup', () => {
    renderPage();
    expect(screen.getByText('Find Channels')).toBeInTheDocument();
    expect(screen.getByText('Crypto News Daily')).toBeInTheDocument();
  });

  it('shows Next button on first slide', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Next' })).toBeInTheDocument();
  });

  it('Next button is enabled even when task is not completed (soft-gated)', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled();
  });

  it('shows recommended task status when task is not completed', () => {
    renderPage();
    expect(screen.getByText('Recommended: complete the action in this slide')).toBeInTheDocument();
  });

  it('shows completed task status after completing slide task', async () => {
    renderPage();
    completeTask(0);
    await waitFor(() => {
      expect(screen.getByText('Done: you can continue or explore more')).toBeInTheDocument();
    });
  });

  it('advances to second slide on Next click without task completion', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => {
      expect(screen.getByText('Secure Deals')).toBeInTheDocument();
    });
  });

  it('shows role-aware finish button on last slide', async () => {
    const { user } = renderPage();
    completeTask(0);
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => expect(screen.getByText('Secure Deals')).toBeInTheDocument());
    completeTask(1);
    await user.click(screen.getByRole('button', { name: 'Next' }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Open catalog' })).toBeInTheDocument();
    });
  });

  it('shows Skip link in tour header', () => {
    renderPage();
    expect(screen.getByRole('button', { name: 'Skip tutorial' })).toBeInTheDocument();
  });

  it('opens skip confirmation dialog', async () => {
    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Skip tutorial' }));
    expect(screen.getByText('Skip tutorial?')).toBeInTheDocument();
  });

  it('redirects owner to owner-first action after completion', async () => {
    act(() => {
      useOnboardingStore.getState().reset();
      useOnboardingStore.getState().toggleInterest('owner');
      useOnboardingStore.getState().setActiveSlide(2);
    });

    const { user } = renderPage();
    await user.click(screen.getByRole('button', { name: 'Add channel' }));

    await waitFor(() => {
      expect(screen.getByText('owner-entry-page')).toBeInTheDocument();
    });
  });

  it('redirects to /onboarding/interest when interests are empty', () => {
    act(() => {
      useOnboardingStore.getState().reset();
    });
    renderPage();
    expect(screen.getByText('interest-page')).toBeInTheDocument();
  });

  it('renders pagination dots with ARIA tablist role', () => {
    renderPage();
    expect(screen.getByRole('tablist')).toBeInTheDocument();
    expect(screen.getAllByRole('tab')).toHaveLength(3);
  });
});
