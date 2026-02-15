import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render } from '@/test/test-utils';
import { Textarea } from '../textarea';

describe('Textarea', () => {
  it('forwards ref to the underlying <textarea>', () => {
    const ref = createRef<HTMLTextAreaElement>();

    render(<Textarea ref={ref} value="Hello" onChange={vi.fn()} />);

    expect(ref.current).toBeInstanceOf(HTMLTextAreaElement);
  });
});
