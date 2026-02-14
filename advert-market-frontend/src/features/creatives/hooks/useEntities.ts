import { useCallback, useState } from 'react';
import type { TextEntity, TextEntityType } from '../types/creative';

type Selection = {
  start: number;
  end: number;
};

export function useEntities(initialEntities: TextEntity[] = []) {
  const [entities, setEntities] = useState<TextEntity[]>(initialEntities);

  const addEntity = useCallback(
    (type: TextEntityType, selection: Selection, extra?: { url?: string; language?: string }) => {
      if (selection.start >= selection.end) return;

      setEntities((prev) => [
        ...prev,
        {
          type,
          offset: selection.start,
          length: selection.end - selection.start,
          ...(extra?.url ? { url: extra.url } : {}),
          ...(extra?.language ? { language: extra.language } : {}),
        },
      ]);
    },
    [],
  );

  const removeEntity = useCallback((index: number) => {
    setEntities((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const toggleEntity = useCallback(
    (type: TextEntityType, selection: Selection, extra?: { url?: string }) => {
      if (selection.start >= selection.end) return;

      const existingIndex = entities.findIndex(
        (e) => e.type === type && e.offset === selection.start && e.length === selection.end - selection.start,
      );

      if (existingIndex >= 0) {
        removeEntity(existingIndex);
      } else {
        addEntity(type, selection, extra);
      }
    },
    [entities, addEntity, removeEntity],
  );

  const isActive = useCallback(
    (type: TextEntityType, cursorPos: number): boolean => {
      return entities.some((e) => e.type === type && e.offset <= cursorPos && e.offset + e.length > cursorPos);
    },
    [entities],
  );

  const clearEntities = useCallback(() => {
    setEntities([]);
  }, []);

  const replaceEntities = useCallback((newEntities: TextEntity[]) => {
    setEntities(newEntities);
  }, []);

  return {
    entities,
    addEntity,
    removeEntity,
    toggleEntity,
    isActive,
    clearEntities,
    replaceEntities,
  };
}
