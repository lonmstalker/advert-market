import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { TextareaField } from '../textarea-field';

describe('TextareaField', () => {
  it('renders textarea with value', () => {
    render(<TextareaField value="hello" onChange={vi.fn()} />);
    expect(screen.getByRole('textbox')).toHaveValue('hello');
  });

  it('calls onChange on input', async () => {
    const onChange = vi.fn();
    render(<TextareaField value="" onChange={onChange} />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), 'a');
    expect(onChange).toHaveBeenCalledWith('a');
  });

  it('shows char count when showCharCount is true', () => {
    render(<TextareaField value="hi" onChange={vi.fn()} maxLength={100} />);
    expect(screen.getByText('2/100')).toBeInTheDocument();
  });

  it('hides char count when showCharCount is false', () => {
    render(<TextareaField value="hi" onChange={vi.fn()} showCharCount={false} />);
    expect(screen.queryByText('2/2000')).not.toBeInTheDocument();
  });

  it('renders label when provided', () => {
    render(<TextareaField value="" onChange={vi.fn()} label="Message" />);
    expect(screen.getByText('Message')).toBeInTheDocument();
  });

  it('sets maxLength attribute', () => {
    render(<TextareaField value="" onChange={vi.fn()} maxLength={500} />);
    expect(screen.getByRole('textbox')).toHaveAttribute('maxLength', '500');
  });
});
