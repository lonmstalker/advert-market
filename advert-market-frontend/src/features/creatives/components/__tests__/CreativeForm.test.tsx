import { createRef } from 'react';
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
    fireEvent.focus(textarea);
    textarea.setSelectionRange(0, 5);
    fireEvent.select(textarea);
    fireEvent.mouseUp(textarea);

    expect(bold).toBeEnabled();

    fireEvent.click(bold);
    expect(toggleEntity).toHaveBeenCalledWith(TextEntityType.BOLD, { start: 0, end: 5 });
  });
});
