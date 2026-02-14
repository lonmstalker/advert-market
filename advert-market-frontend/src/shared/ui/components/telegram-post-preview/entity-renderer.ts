import { createElement, type ReactNode } from 'react';
import type { TextEntity } from '@/shared/types/text-entity';
import { TextEntityType } from '@/shared/types/text-entity';

type EntityTag = {
  type: TextEntityType;
  url?: string;
  language?: string;
};

export function renderEntities(text: string, entities: TextEntity[]): ReactNode[] {
  if (text.length === 0 && entities.length === 0) {
    return [];
  }

  if (entities.length === 0) {
    return renderPlainText(text);
  }

  // Build a list of boundary points where entity sets change
  const sorted = [...entities].sort((a, b) => a.offset - b.offset || b.length - a.length);

  // Collect all unique boundary offsets
  const boundaries = new Set<number>();
  boundaries.add(0);
  boundaries.add(text.length);
  for (const e of sorted) {
    boundaries.add(e.offset);
    boundaries.add(e.offset + e.length);
  }

  const sortedBoundaries = [...boundaries].sort((a, b) => a - b);

  const result: ReactNode[] = [];
  let keyCounter = 0;

  for (let i = 0; i < sortedBoundaries.length - 1; i++) {
    const start = sortedBoundaries.at(i);
    const end = sortedBoundaries.at(i + 1);
    if (start == null || end == null) continue;

    if (start >= end) continue;

    // Extract substring using UTF-16 offsets (JS strings are UTF-16 natively)
    const segment = text.slice(start, end);

    // Find all entities that cover this segment
    const activeEntities: EntityTag[] = [];
    for (const e of sorted) {
      if (e.offset <= start && e.offset + e.length >= end) {
        activeEntities.push({ type: e.type, url: e.url, language: e.language });
      }
    }

    if (activeEntities.length === 0) {
      result.push(...renderPlainText(segment, keyCounter));
      keyCounter += segment.split('\n').length;
    } else {
      const innerNodes = renderPlainText(segment);
      const wrapped = wrapWithEntities(innerNodes, activeEntities, keyCounter);
      result.push(wrapped);
      keyCounter++;
    }
  }

  return result;
}

function renderPlainText(text: string, startKey = 0): ReactNode[] {
  const parts = text.split('\n');
  const nodes: ReactNode[] = [];

  for (let i = 0; i < parts.length; i++) {
    const part = parts[i] ?? '';
    if (i > 0) {
      nodes.push(createElement('br', { key: `br-${startKey + i}` }));
    }
    if (part.length > 0) {
      nodes.push(part);
    }
  }

  return nodes;
}

function wrapWithEntities(children: ReactNode[], entities: EntityTag[], key: number): ReactNode {
  let result: ReactNode = createElement('span', { key: `seg-${key}` }, ...children);

  // Apply entities from innermost to outermost
  // Lower number = applied first = innermost; higher = applied last = outermost
  const sortedEntities = [...entities].sort((a, b) => {
    const priority: Record<string, number> = {
      [TextEntityType.CODE]: 0,
      [TextEntityType.PRE]: 0,
      [TextEntityType.STRIKETHROUGH]: 1,
      [TextEntityType.UNDERLINE]: 2,
      [TextEntityType.ITALIC]: 3,
      [TextEntityType.BOLD]: 4,
      [TextEntityType.TEXT_LINK]: 5,
      [TextEntityType.SPOILER]: 6,
    };
    return (priority[a.type] ?? 99) - (priority[b.type] ?? 99);
  });

  for (const entity of sortedEntities) {
    result = wrapSingle(result, entity, key);
  }

  return result;
}

function wrapSingle(child: ReactNode, entity: EntityTag, key: number): ReactNode {
  switch (entity.type) {
    case TextEntityType.BOLD:
      return createElement('strong', { key: `bold-${key}` }, child);
    case TextEntityType.ITALIC:
      return createElement('em', { key: `italic-${key}` }, child);
    case TextEntityType.UNDERLINE:
      return createElement('u', { key: `underline-${key}` }, child);
    case TextEntityType.STRIKETHROUGH:
      return createElement('del', { key: `strike-${key}` }, child);
    case TextEntityType.CODE:
      return createElement('code', { key: `code-${key}` }, child);
    case TextEntityType.PRE:
      return createElement(
        'pre',
        { key: `pre-${key}` },
        createElement('code', { ...(entity.language ? { className: `language-${entity.language}` } : {}) }, child),
      );
    case TextEntityType.TEXT_LINK:
      return createElement(
        'a',
        {
          key: `link-${key}`,
          href: entity.url,
          target: '_blank',
          rel: 'noopener noreferrer',
        },
        child,
      );
    case TextEntityType.SPOILER:
      return createElement('span', { key: `spoiler-${key}`, 'data-spoiler': 'true' }, child);
    default:
      return child;
  }
}
