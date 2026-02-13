import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { PageLoader } from '../page-loader';

describe('PageLoader', () => {
  it('renders a spinner container', () => {
    const { container } = render(<PageLoader />);
    const wrapper = container.firstChild as HTMLElement;
    expect(wrapper.style.display).toBe('flex');
    expect(wrapper.style.justifyContent).toBe('center');
    expect(wrapper.style.height).toBe('60vh');
  });
});
