import { emptyCanvasState, SCHEMA_VERSION, type CanvasState } from './types';

/**
 * Migrates a raw parsed canvas-state object (of any prior schemaVersion) up to the
 * current SCHEMA_VERSION. Each migration step is a pure function from one version
 * to the next, chained in order, so adding a new version means adding one function
 * and one entry in MIGRATIONS rather than a monolithic branch.
 */
type Migration = (raw: any) => any;

const MIGRATIONS: Record<string, Migration> = {
  // 1.0 -> 1.1: `meta` block was introduced; backfill defaults for older snapshots.
  '1.0': (raw) => ({
    ...raw,
    schemaVersion: '1.1',
    meta: raw.meta ?? { unit: 'm', gridSize: 0.5 },
  }),
};

export function migrateCanvasState(raw: unknown): CanvasState {
  if (raw == null || typeof raw !== 'object') {
    return emptyCanvasState();
  }

  let current: any = raw;
  let guard = 0;
  while (current.schemaVersion !== SCHEMA_VERSION && guard < 10) {
    const migration = MIGRATIONS[current.schemaVersion];
    if (!migration) break;
    current = migration(current);
    guard += 1;
  }

  if (current.schemaVersion !== SCHEMA_VERSION || !Array.isArray(current.elements)) {
    // Unknown or unmigratable version — return a safe empty state rather than
    // crashing the editor on a corrupt or future-versioned payload.
    return emptyCanvasState();
  }

  return current as CanvasState;
}

export function parseCanvasStateJson(json: string): CanvasState {
  try {
    const raw = JSON.parse(json);
    return migrateCanvasState(raw);
  } catch {
    return emptyCanvasState();
  }
}
