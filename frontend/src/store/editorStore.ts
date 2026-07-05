import { create } from 'zustand';
import { emptyCanvasState, type CanvasElement, type CanvasState, type Tool } from '../engine/types';

interface HistoryEntry {
  state: CanvasState;
}

interface EditorStore {
  canvasState: CanvasState;
  selectedIds: Set<string>;
  activeTool: Tool;
  wallThickness: number;
  wallHeight: number;

  undoStack: HistoryEntry[];
  redoStack: HistoryEntry[];

  setTool: (tool: Tool) => void;
  setSelection: (ids: string[]) => void;
  toggleSelection: (id: string) => void;
  clearSelection: () => void;

  addElement: (element: CanvasElement) => void;
  updateElement: (id: string, patch: Partial<CanvasElement>) => void;
  removeElements: (ids: string[]) => void;

  loadState: (state: CanvasState) => void;
  applyGeneratedLayout: (state: CanvasState) => void;
  applyRemoteState: (state: CanvasState) => void;
  resetState: () => void;

  undo: () => void;
  redo: () => void;
  canUndo: () => boolean;
  canRedo: () => boolean;
}

const HISTORY_LIMIT = 100;

function cloneState(state: CanvasState): CanvasState {
  return JSON.parse(JSON.stringify(state));
}

export const useEditorStore = create<EditorStore>((set, get) => ({
  canvasState: emptyCanvasState(),
  selectedIds: new Set(),
  activeTool: 'SELECT',
  wallThickness: 0.2,
  wallHeight: 3.0,

  undoStack: [],
  redoStack: [],

  setTool: (tool) => set({ activeTool: tool, selectedIds: new Set() }),

  setSelection: (ids) => set({ selectedIds: new Set(ids) }),
  toggleSelection: (id) =>
    set((s) => {
      const next = new Set(s.selectedIds);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return { selectedIds: next };
    }),
  clearSelection: () => set({ selectedIds: new Set() }),

  addElement: (element) => {
    const current = get().canvasState;
    pushHistory(set, get, current);
    set({
      canvasState: { ...current, elements: [...current.elements, element] },
      redoStack: [],
    });
  },

  updateElement: (id, patch) => {
    const current = get().canvasState;
    pushHistory(set, get, current);
    set({
      canvasState: {
        ...current,
        elements: current.elements.map((el) => (el.id === id ? ({ ...el, ...patch } as CanvasElement) : el)),
      },
      redoStack: [],
    });
  },

  removeElements: (ids) => {
    const current = get().canvasState;
    pushHistory(set, get, current);
    const idSet = new Set(ids);
    set({
      canvasState: {
        ...current,
        // Also drop any door/window elements hosted on a removed wall to avoid orphans.
        elements: current.elements.filter((el) => {
          if (idSet.has(el.id)) return false;
          if (el.type === 'DOOR' || el.type === 'WINDOW') {
            return !idSet.has((el as any).hostWallId);
          }
          return true;
        }),
      },
      selectedIds: new Set(),
      redoStack: [],
    });
  },

  loadState: (state) => set({ canvasState: state, undoStack: [], redoStack: [], selectedIds: new Set() }),

  // Used when replacing the drawing with an AI-generated layout: unlike loadState
  // (used for the initial snapshot load, where there's no prior in-session state
  // worth preserving), this pushes the current state onto the undo stack first so
  // the replacement can be undone.
  applyGeneratedLayout: (state) => {
    const current = get().canvasState;
    pushHistory(set, get, current);
    set({ canvasState: state, redoStack: [], selectedIds: new Set() });
  },

  // Applied when a collaborator's live edit arrives over WebSocket. Unlike
  // applyGeneratedLayout, this clears undo/redo entirely rather than pushing
  // history: a remote overwrite can contain unrelated changes, so the local
  // undo stack (built against the pre-overwrite timeline) can no longer be
  // trusted to produce a sane result if replayed. This is the sharp edge of
  // the last-write-wins design documented in CollabController — a real CRDT/
  // operational-transform approach wouldn't need to do this.
  applyRemoteState: (state) => set({ canvasState: state, undoStack: [], redoStack: [], selectedIds: new Set() }),

  resetState: () => set({ canvasState: emptyCanvasState(), undoStack: [], redoStack: [], selectedIds: new Set() }),

  undo: () => {
    const { undoStack, canvasState } = get();
    if (undoStack.length === 0) return;
    const prev = undoStack[undoStack.length - 1];
    set((s) => ({
      canvasState: prev.state,
      undoStack: s.undoStack.slice(0, -1),
      redoStack: [...s.redoStack, { state: cloneState(canvasState) }],
      selectedIds: new Set(),
    }));
  },

  redo: () => {
    const { redoStack, canvasState } = get();
    if (redoStack.length === 0) return;
    const next = redoStack[redoStack.length - 1];
    set((s) => ({
      canvasState: next.state,
      redoStack: s.redoStack.slice(0, -1),
      undoStack: [...s.undoStack, { state: cloneState(canvasState) }],
      selectedIds: new Set(),
    }));
  },

  canUndo: () => get().undoStack.length > 0,
  canRedo: () => get().redoStack.length > 0,
}));

function pushHistory(
  set: (partial: Partial<EditorStore>) => void,
  get: () => EditorStore,
  currentState: CanvasState
) {
  const { undoStack } = get();
  const nextStack = [...undoStack, { state: cloneState(currentState) }];
  if (nextStack.length > HISTORY_LIMIT) nextStack.shift();
  set({ undoStack: nextStack });
}
