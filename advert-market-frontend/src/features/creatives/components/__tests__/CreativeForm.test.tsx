import { act, createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/test-utils';
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

  it('keeps only one button row expanded at a time', async () => {
    const textareaRef = createRef<HTMLTextAreaElement>();
    const { user } = renderWithProviders(
      <CreativeForm
        title="Test"
        onTitleChange={vi.fn()}
        text="Hello world"
        onTextChange={vi.fn()}
        media={[]}
        onMediaChange={vi.fn()}
        buttons={[
          [{ id: 'btn-1', text: 'Open', url: 'https://example.com/open' }],
          [{ id: 'btn-2', text: 'Read', url: 'https://example.com/read' }],
        ]}
        onButtonsChange={vi.fn()}
        toggleEntity={vi.fn()}
        isActive={() => false}
        disableWebPagePreview={false}
        onDisableWebPagePreviewChange={vi.fn()}
        textareaRef={textareaRef}
      />,
    );

    const firstToggle = screen.getByTestId('creative-button-row-toggle-0');
    const secondToggle = screen.getByTestId('creative-button-row-toggle-1');

    expect(firstToggle).toHaveAttribute('aria-expanded', 'true');
    expect(secondToggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByDisplayValue('Open')).toBeInTheDocument();
    expect(screen.queryByDisplayValue('Read')).not.toBeInTheDocument();

    await user.click(secondToggle);

    expect(firstToggle).toHaveAttribute('aria-expanded', 'false');
    expect(secondToggle).toHaveAttribute('aria-expanded', 'true');
    await waitFor(() => {
      expect(screen.queryByDisplayValue('Open')).not.toBeInTheDocument();
      expect(screen.getByDisplayValue('Read')).toBeInTheDocument();
    });
  });
});
