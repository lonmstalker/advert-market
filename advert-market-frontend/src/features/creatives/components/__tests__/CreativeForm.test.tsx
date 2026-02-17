import { act, createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, renderWithProviders, screen } from '@/test/test-utils';
import { TextEntityType } from '../../types/creative';
import { CreativeForm } from '../CreativeForm';

describe('CreativeForm', () => {
  it('enables formatting after selecting text and toggles entity on click', () => {
    const textareaRef = createRef<HTMLTextAreaElement>();
    const toggleEntity = vi.fn();

    renderWithProviders(
      <CreativeForm
        title="Test"
        onTitleChange={vi.fn()}
        text="Hello world"
        onTextChange={vi.fn()}
        media={[]}
        onMediaChange={vi.fn()}
        buttons={[]}
        onButtonsChange={vi.fn()}
        toggleEntity={toggleEntity}
        isActive={() => false}
        disableWebPagePreview={false}
        onDisableWebPagePreviewChange={vi.fn()}
        textareaRef={textareaRef}
      />,
    );

    const bold = screen.getByRole('button', { name: 'Bold' });
    expect(bold).toBeDisabled();

    const textarea = screen.getByPlaceholderText('Enter ad post text...') as HTMLTextAreaElement;
    act(() => {
      fireEvent.focus(textarea);
      textarea.setSelectionRange(0, 5);
      fireEvent.select(textarea);
      fireEvent.mouseUp(textarea);
    });

    expect(bold).toBeEnabled();

    act(() => {
      fireEvent.click(bold);
    });
    expect(toggleEntity).toHaveBeenCalledWith(TextEntityType.BOLD, { start: 0, end: 5 });
  });

  it('shows inline URL error for invalid button links', () => {
    const textareaRef = createRef<HTMLTextAreaElement>();

    renderWithProviders(
      <CreativeForm
        title="Test"
        onTitleChange={vi.fn()}
        text="Hello world"
        onTextChange={vi.fn()}
        media={[]}
        onMediaChange={vi.fn()}
        buttons={[[{ id: 'btn-1', text: 'Open', url: 'not-url' }]]}
        onButtonsChange={vi.fn()}
        toggleEntity={vi.fn()}
        isActive={() => false}
        disableWebPagePreview={false}
        onDisableWebPagePreviewChange={vi.fn()}
        textareaRef={textareaRef}
      />,
    );

    expect(screen.getByText('Enter a valid URL starting with http:// or https://')).toBeInTheDocument();
  });
});
