import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import { useEditorStore } from '../../store/editorStore';
import {
  drawElements,
  drawGrid,
  hitTestElement,
  screenToWorld,
  worldToScreen,
  type ViewTransform,
} from '../../engine/CADEngine';
import { findNearestWall, snapAngle, snapPoint } from '../../engine/snapEngine';
import type { OpeningElement, Point, RoomElement, WallElement } from '../../engine/types';

const DEFAULT_VIEW: ViewTransform = { scale: 60, offsetX: 0, offsetY: 0 };

function newId(prefix: string): string {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
}

export interface CadCanvasHandle {
  zoomIn: () => void;
  zoomOut: () => void;
}

interface CadCanvasProps {
  remoteCursors?: Record<string, { userId: string; fullName: string; color: string; x: number; y: number }>;
  onCursorMove?: (x: number, y: number) => void;
}

const CadCanvas = forwardRef<CadCanvasHandle, CadCanvasProps>(function CadCanvas({ remoteCursors, onCursorMove }, ref) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [view, setView] = useState<ViewTransform>(DEFAULT_VIEW);
  const [size, setSize] = useState({ width: 800, height: 600 });
  const [drawStart, setDrawStart] = useState<Point | null>(null);
  const [cursorWorld, setCursorWorld] = useState<Point | null>(null);
  const [roomPoints, setRoomPoints] = useState<Point[]>([]);
  const [isPanning, setIsPanning] = useState(false);
  const panOrigin = useRef<{ screen: Point; view: ViewTransform } | null>(null);

  const {
    canvasState,
    activeTool,
    selectedIds,
    wallThickness,
    wallHeight,
    addElement,
    setSelection,
    clearSelection,
    removeElements,
    undo,
    redo,
  } = useEditorStore();

  useImperativeHandle(ref, () => ({
    zoomIn: () => setView((v) => ({ ...v, scale: Math.min(400, v.scale * 1.2) })),
    zoomOut: () => setView((v) => ({ ...v, scale: Math.max(10, v.scale / 1.2) })),
  }));

  // Resize observer keeps the canvas crisp on container resize.
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const observer = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (entry) {
        setSize({ width: entry.contentRect.width, height: entry.contentRect.height });
      }
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  // Keyboard shortcuts: undo/redo/delete/escape
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      const meta = e.metaKey || e.ctrlKey;
      if (meta && e.key.toLowerCase() === 'z' && !e.shiftKey) {
        e.preventDefault();
        undo();
      } else if (meta && (e.key.toLowerCase() === 'y' || (e.key.toLowerCase() === 'z' && e.shiftKey))) {
        e.preventDefault();
        redo();
      } else if (e.key === 'Delete' || e.key === 'Backspace') {
        if (selectedIds.size > 0) {
          e.preventDefault();
          removeElements(Array.from(selectedIds));
        }
      } else if (e.key === 'Escape') {
        setDrawStart(null);
        setRoomPoints([]);
        clearSelection();
      }
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [undo, redo, removeElements, selectedIds, clearSelection]);

  // Main render loop
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const dpr = window.devicePixelRatio || 1;
    canvas.width = size.width * dpr;
    canvas.height = size.height * dpr;
    canvas.style.width = `${size.width}px`;
    canvas.style.height = `${size.height}px`;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    drawGrid(ctx, view, size.width, size.height, canvasState.meta.gridSize);
    drawElements(ctx, canvasState, view, size.width, size.height, selectedIds);

    // In-progress wall preview
    if (activeTool === 'WALL' && drawStart && cursorWorld) {
      const endPoint = snapAngle(drawStart, cursorWorld);
      const p1 = worldToScreen(drawStart, view, size.width, size.height);
      const p2 = worldToScreen(endPoint, view, size.width, size.height);
      ctx.save();
      ctx.strokeStyle = 'rgba(4,120,87,0.7)';
      ctx.lineWidth = Math.max(2, wallThickness * view.scale);
      ctx.setLineDash([6, 4]);
      ctx.beginPath();
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
      ctx.stroke();
      ctx.setLineDash([]);
      // length label
      const length = Math.hypot(endPoint.x - drawStart.x, endPoint.y - drawStart.y);
      ctx.fillStyle = '#1A1A2E';
      ctx.font = '500 12px "JetBrains Mono", monospace';
      ctx.fillText(`${length.toFixed(2)}m`, (p1.x + p2.x) / 2 + 8, (p1.y + p2.y) / 2 - 8);
      ctx.restore();
    }

    // In-progress room preview
    if (activeTool === 'ROOM' && roomPoints.length > 0) {
      ctx.save();
      ctx.strokeStyle = 'rgba(16,185,129,0.8)';
      ctx.lineWidth = 2;
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      const first = worldToScreen(roomPoints[0], view, size.width, size.height);
      ctx.moveTo(first.x, first.y);
      for (const pt of roomPoints.slice(1)) {
        const p = worldToScreen(pt, view, size.width, size.height);
        ctx.lineTo(p.x, p.y);
      }
      if (cursorWorld) {
        const p = worldToScreen(snapPoint(cursorWorld, canvasState.elements, canvasState.meta.gridSize), view, size.width, size.height);
        ctx.lineTo(p.x, p.y);
      }
      ctx.stroke();
      ctx.setLineDash([]);
      ctx.restore();
    }

    // Door/window placement ghost, snapped to nearest wall
    if ((activeTool === 'DOOR' || activeTool === 'WINDOW') && cursorWorld) {
      const nearest = findNearestWall(cursorWorld, canvasState.elements);
      if (nearest) {
        const wall = nearest.wall;
        const wallLength = Math.hypot(wall.x2 - wall.x1, wall.y2 - wall.y1);
        const dirX = (wall.x2 - wall.x1) / wallLength;
        const dirY = (wall.y2 - wall.y1) / wallLength;
        const openingWidth = activeTool === 'DOOR' ? 0.9 : 1.2;
        const clampedCenter = Math.max(openingWidth / 2, Math.min(wallLength - openingWidth / 2, nearest.distanceAlongWall));
        const centerX = wall.x1 + dirX * clampedCenter;
        const centerY = wall.y1 + dirY * clampedCenter;
        const start = worldToScreen({ x: centerX - dirX * openingWidth / 2, y: centerY - dirY * openingWidth / 2 }, view, size.width, size.height);
        const end = worldToScreen({ x: centerX + dirX * openingWidth / 2, y: centerY + dirY * openingWidth / 2 }, view, size.width, size.height);
        ctx.save();
        ctx.strokeStyle = activeTool === 'DOOR' ? '#C2692A' : '#185FA5';
        ctx.lineWidth = Math.max(4, wall.thickness * view.scale + 3);
        ctx.globalAlpha = 0.6;
        ctx.beginPath();
        ctx.moveTo(start.x, start.y);
        ctx.lineTo(end.x, end.y);
        ctx.stroke();
        ctx.restore();
      }
    }

    // Remote collaborators' cursors
    if (remoteCursors) {
      for (const cursor of Object.values(remoteCursors)) {
        const p = worldToScreen({ x: cursor.x, y: cursor.y }, view, size.width, size.height);
        ctx.save();
        ctx.fillStyle = cursor.color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, 5, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 1.5;
        ctx.stroke();
        ctx.font = '500 11px Inter, sans-serif';
        const label = cursor.fullName.split(' ')[0] || cursor.fullName;
        const textWidth = ctx.measureText(label).width;
        ctx.fillStyle = cursor.color;
        ctx.fillRect(p.x + 8, p.y - 9, textWidth + 8, 16);
        ctx.fillStyle = '#ffffff';
        ctx.fillText(label, p.x + 12, p.y + 3);
        ctx.restore();
      }
    }
  }, [canvasState, view, size, selectedIds, activeTool, drawStart, cursorWorld, roomPoints, wallThickness, remoteCursors]);

  const toWorld = useCallback(
    (clientX: number, clientY: number): Point => {
      const canvas = canvasRef.current;
      if (!canvas) return { x: 0, y: 0 };
      const rect = canvas.getBoundingClientRect();
      const screenPt = { x: clientX - rect.left, y: clientY - rect.top };
      return screenToWorld(screenPt, view, size.width, size.height);
    },
    [view, size]
  );

  function handlePointerDown(e: React.PointerEvent<HTMLCanvasElement>) {
    if (e.button === 1 || activeTool === 'PAN') {
      setIsPanning(true);
      panOrigin.current = { screen: { x: e.clientX, y: e.clientY }, view };
      return;
    }

    const world = toWorld(e.clientX, e.clientY);

    if (activeTool === 'SELECT') {
      const hit = hitTestElement(world, canvasState);
      if (hit) {
        setSelection([hit.id]);
      } else {
        clearSelection();
      }
      return;
    }

    if (activeTool === 'WALL') {
      const snapped = snapPoint(world, canvasState.elements, canvasState.meta.gridSize);
      if (!drawStart) {
        setDrawStart(snapped);
      } else {
        const endPoint = snapAngle(drawStart, snapped);
        const length = Math.hypot(endPoint.x - drawStart.x, endPoint.y - drawStart.y);
        if (length >= 0.05) {
          const wall: WallElement = {
            id: newId('wall'),
            type: 'WALL',
            x1: drawStart.x,
            y1: drawStart.y,
            x2: endPoint.x,
            y2: endPoint.y,
            thickness: wallThickness,
            height: wallHeight,
          };
          addElement(wall);
        }
        // Chain from the last endpoint so consecutive walls join naturally.
        setDrawStart(endPoint);
      }
      return;
    }

    if (activeTool === 'ROOM') {
      const snapped = snapPoint(world, canvasState.elements, canvasState.meta.gridSize);
      // Closing the loop: click near the first point to finish the room.
      if (roomPoints.length >= 3) {
        const first = roomPoints[0];
        const dist = Math.hypot(snapped.x - first.x, snapped.y - first.y);
        if (dist < 0.3) {
          const room: RoomElement = {
            id: newId('room'),
            type: 'ROOM',
            points: roomPoints,
            label: 'Room',
            floorColor: '#E1F5EE',
          };
          addElement(room);
          setRoomPoints([]);
          return;
        }
      }
      setRoomPoints((pts) => [...pts, snapped]);
      return;
    }

    if (activeTool === 'DOOR' || activeTool === 'WINDOW') {
      const nearest = findNearestWall(world, canvasState.elements);
      if (!nearest) return;
      const wall = nearest.wall;
      const wallLength = Math.hypot(wall.x2 - wall.x1, wall.y2 - wall.y1);
      const openingWidth = activeTool === 'DOOR' ? 0.9 : 1.2;
      const clampedCenter = Math.max(openingWidth / 2, Math.min(wallLength - openingWidth / 2, nearest.distanceAlongWall));
      const opening: OpeningElement = {
        id: newId(activeTool.toLowerCase()),
        type: activeTool,
        hostWallId: wall.id,
        x: clampedCenter,
        y: 0,
        width: openingWidth,
        swing: 'RIGHT',
      };
      addElement(opening);
    }
  }

  function handlePointerMove(e: React.PointerEvent<HTMLCanvasElement>) {
    if (isPanning && panOrigin.current) {
      const dx = (e.clientX - panOrigin.current.screen.x) / panOrigin.current.view.scale;
      const dy = (e.clientY - panOrigin.current.screen.y) / panOrigin.current.view.scale;
      setView({ ...panOrigin.current.view, offsetX: panOrigin.current.view.offsetX - dx, offsetY: panOrigin.current.view.offsetY - dy });
      return;
    }
    const world = toWorld(e.clientX, e.clientY);
    setCursorWorld(world);
    onCursorMove?.(world.x, world.y);
  }

  function handlePointerUp() {
    setIsPanning(false);
    panOrigin.current = null;
  }

  function handleWheel(e: React.WheelEvent<HTMLCanvasElement>) {
    e.preventDefault();
    const zoomFactor = e.deltaY > 0 ? 0.9 : 1.1;
    setView((v) => ({ ...v, scale: Math.min(400, Math.max(10, v.scale * zoomFactor)) }));
  }

  function handleDoubleClick() {
    if (activeTool === 'WALL') setDrawStart(null);
  }

  return (
    <div ref={containerRef} className="relative w-full h-full bg-canvas overflow-hidden touch-none">
      <canvas
        ref={canvasRef}
        className="block cursor-crosshair"
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerLeave={handlePointerUp}
        onWheel={handleWheel}
        onDoubleClick={handleDoubleClick}
        data-testid="cad-canvas"
      />
      <div className="absolute bottom-2 left-2 font-mono text-xs text-muted bg-surface/80 rounded-sm px-2 py-1">
        {cursorWorld ? `x: ${cursorWorld.x.toFixed(2)}m  y: ${cursorWorld.y.toFixed(2)}m` : ''}
        {'  |  '}zoom: {Math.round((view.scale / DEFAULT_VIEW.scale) * 100)}%
      </div>
    </div>
  );
});

export default CadCanvas;
