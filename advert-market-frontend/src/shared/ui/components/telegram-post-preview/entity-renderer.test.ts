import { createElement } from 'react';
import { describe, expect, it } from 'vitest';
import type { TextEntity } from '@/shared/types/text-entity';
import { TextEntityType } from '@/shared/types/text-entity';
import { render, screen } from '@/test/test-utils';
import { renderEntities } from './entity-renderer';

function renderToContainer(text: string, entities: TextEntity[]) {
  const nodes = renderEntities(text, entities);
  return render(createElement('div', { 'data-testid': 'container' }, ...nodes));
}

describe('entity-renderer', () => {
  describe('plain text without entities', () => {
    it('should render plain text as-is', () => {
      renderToContainer('Hello, world!', []);
      expect(screen.getByTestId('container')).toHaveTextContent('Hello, world!');
    });

    it('should render empty string', () => {
      renderToContainer('', []);
      expect(screen.getByTestId('container')).toHaveTextContent('');
    });

    it('should render newlines as br elements', () => {
      renderToContainer('Line 1\nLine 2', []);
      const container = screen.getByTestId('container');
      expect(container.querySelector('br')).toBeTruthy();
      expect(container).toHaveTextContent('Line 1Line 2');
    });
  });

  describe('single entities', () => {
    it('should render bold entity as <strong>', () => {
      renderToContainer('Hello bold world', [{ type: TextEntityType.BOLD, offset: 6, length: 4 }]);
      const strong = screen.getByTestId('container').querySelector('strong');
      expect(strong).toBeTruthy();
      expect(strong!.textContent).toBe('bold');
    });

    it('should render italic entity as <em>', () => {
      renderToContainer('Hello italic world', [{ type: TextEntityType.ITALIC, offset: 6, length: 6 }]);
      const em = screen.getByTestId('container').querySelector('em');
      expect(em).toBeTruthy();
      expect(em!.textContent).toBe('italic');
    });

    it('should render underline entity as <u>', () => {
      renderToContainer('Hello underline world', [{ type: TextEntityType.UNDERLINE, offset: 6, length: 9 }]);
      const u = screen.getByTestId('container').querySelector('u');
      expect(u).toBeTruthy();
      expect(u!.textContent).toBe('underline');
    });

    it('should render strikethrough entity as <del>', () => {
      renderToContainer('Hello deleted world', [{ type: TextEntityType.STRIKETHROUGH, offset: 6, length: 7 }]);
      const del = screen.getByTestId('container').querySelector('del');
      expect(del).toBeTruthy();
      expect(del!.textContent).toBe('deleted');
    });

    it('should render code entity as <code>', () => {
      renderToContainer('Use const x = 1 here', [{ type: TextEntityType.CODE, offset: 4, length: 11 }]);
      const code = screen.getByTestId('container').querySelector('code');
      expect(code).toBeTruthy();
      expect(code!.textContent).toBe('const x = 1');
    });

    it('should render pre entity as <pre><code>', () => {
      renderToContainer('Code block:\nfunction() {}', [{ type: TextEntityType.PRE, offset: 12, length: 13 }]);
      const pre = screen.getByTestId('container').querySelector('pre');
      expect(pre).toBeTruthy();
      const code = pre!.querySelector('code');
      expect(code).toBeTruthy();
      expect(code!.textContent).toBe('function() {}');
    });

    it('should render text_link entity as <a>', () => {
      renderToContainer('Click here please', [
        { type: TextEntityType.TEXT_LINK, offset: 6, length: 4, url: 'https://example.com' },
      ]);
      const link = screen.getByTestId('container').querySelector('a');
      expect(link).toBeTruthy();
      expect(link!.textContent).toBe('here');
      expect(link!.getAttribute('href')).toBe('https://example.com');
      expect(link!.getAttribute('target')).toBe('_blank');
      expect(link!.getAttribute('rel')).toBe('noopener noreferrer');
    });

    it('should render spoiler entity with data-spoiler attribute', () => {
      renderToContainer('This is hidden text', [{ type: TextEntityType.SPOILER, offset: 8, length: 6 }]);
      const spoiler = screen.getByTestId('container').querySelector('[data-spoiler]');
      expect(spoiler).toBeTruthy();
      expect(spoiler!.textContent).toBe('hidden');
    });
  });

  describe('nested entities', () => {
    it('should render bold + italic on same range', () => {
      renderToContainer('Hello world', [
        { type: TextEntityType.BOLD, offset: 6, length: 5 },
        { type: TextEntityType.ITALIC, offset: 6, length: 5 },
      ]);
      const strong = screen.getByTestId('container').querySelector('strong');
      expect(strong).toBeTruthy();
      const em = strong!.querySelector('em');
      expect(em).toBeTruthy();
      expect(em!.textContent).toBe('world');
    });

    it('should handle partially overlapping entities', () => {
      // "Hello bold italic world"
      // bold: offset=6, length=11 => "bold italic"
      // italic: offset=11, length=12 => "italic world"
      renderToContainer('Hello bold italic world', [
        { type: TextEntityType.BOLD, offset: 6, length: 11 },
        { type: TextEntityType.ITALIC, offset: 11, length: 12 },
      ]);
      const container = screen.getByTestId('container');
      expect(container).toHaveTextContent('Hello bold italic world');
      // "bold " should be bold only, "italic" should be bold+italic, " world" should be italic only
      const strong = container.querySelector('strong');
      expect(strong).toBeTruthy();
    });
  });

  describe('multiple entities in one text', () => {
    it('should render multiple separate entities', () => {
      renderToContainer('Hello bold and italic text', [
        { type: TextEntityType.BOLD, offset: 6, length: 4 },
        { type: TextEntityType.ITALIC, offset: 15, length: 6 },
      ]);
      const container = screen.getByTestId('container');
      const strong = container.querySelector('strong');
      const em = container.querySelector('em');
      expect(strong!.textContent).toBe('bold');
      expect(em!.textContent).toBe('italic');
    });
  });

  describe('UTF-16 offset correctness', () => {
    it('should handle emoji (surrogate pairs) correctly', () => {
      // Each emoji is 2 UTF-16 code units
      const text = 'Hi \u{1F600} bold';
      // \u{1F600} is at offset 3, takes 2 units. "bold" starts at offset 6
      renderToContainer(text, [{ type: TextEntityType.BOLD, offset: 6, length: 4 }]);
      const strong = screen.getByTestId('container').querySelector('strong');
      expect(strong).toBeTruthy();
      expect(strong!.textContent).toBe('bold');
    });
  });

  describe('entity at string boundaries', () => {
    it('should handle entity at the start', () => {
      renderToContainer('bold text', [{ type: TextEntityType.BOLD, offset: 0, length: 4 }]);
      const strong = screen.getByTestId('container').querySelector('strong');
      expect(strong!.textContent).toBe('bold');
    });

    it('should handle entity at the end', () => {
      renderToContainer('text bold', [{ type: TextEntityType.BOLD, offset: 5, length: 4 }]);
      const strong = screen.getByTestId('container').querySelector('strong');
      expect(strong!.textContent).toBe('bold');
    });

    it('should handle entity covering full text', () => {
      renderToContainer('bold', [{ type: TextEntityType.BOLD, offset: 0, length: 4 }]);
      const strong = screen.getByTestId('container').querySelector('strong');
      expect(strong!.textContent).toBe('bold');
    });
  });
});
