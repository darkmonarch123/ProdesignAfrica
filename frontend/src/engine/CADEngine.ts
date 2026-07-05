import type { CanvasElement, CanvasState, OpeningElement, Point, RoomElement, WallElement } from './types';
import { isOpening, isRoom, isWall } from './types';

export interface ViewTransform {
  /** pixels per meter */
  scale: number;
  /** world-space offset (meters) shown at the canvas origin */
  offsetX: number;
  offsetY: number;
}

export function worldToScreen(point: Point, view: ViewTransform, canvasWidth: number, canvasHeight: number): Point {
  return {
    x: canvasWidth / 2 + (point.x - view.offsetX) * view.scale,
    y: canvasHeight / 2 + (point.y - view.offsetY) * view.scale,
  };
}

export function screenToWorld(point: Point, view: ViewTransform, canvasWidth: number, canvasHeight: number): Point {
  return {
    x: (point.x - canvasWidth / 2) / view.scale + view.offsetX,
    y: (point.y - canvasHeight / 2) / view.scale + view.offsetY,
  };
}

const COLORS = {
  grid: '#C9C4B8',
  gridMajor: '#B5AF9F',
  wall: '#1A1A2E',
  wallSelected: '#047857',
  room: '#E1F5EE',
  roomStroke: '#10b981',
  door: '#C2692A',
  window: '#185FA5',
  ghost: 'rgba(4,120,87,0.4)',
};

export function drawGrid(ctx: CanvasRenderingContext2D, view: ViewTransform, width: number, height: number, gridSize: number) {
  ctx.save();
  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = '#F7F5F0';
  ctx.fillRect(0, 0, width, height);

  const topLeft = screenToWorld({ x: 0, y: 0 }, view, width, height);
  const bottomRight = screenToWorld({ x: width, y: height }, view, width, height);

  const startX = Math.floor(topLeft.x / gridSize) * gridSize;
  const endX = Math.ceil(bottomRight.x / gridSize) * gridSize;
  const startY = Math.floor(topLeft.y / gridSize) * gridSize;
  const endY = Math.ceil(bottomRight.y / gridSize) * gridSize;

  ctx.lineWidth = 1;
  for (let x = startX; x <= endX; x += gridSize) {
    const isMajor = Math.round(x / gridSize) % 5 === 0;
    ctx.strokeStyle = isMajor ? COLORS.gridMajor : COLORS.grid;
    ctx.globalAlpha = isMajor ? 0.6 : 0.3;
    const p1 = worldToScreen({ x, y: startY }, view, width, height);
    const p2 = worldToScreen({ x, y: endY }, view, width, height);
    ctx.beginPath();
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(p2.x, p2.y);
    ctx.stroke();
  }
  for (let y = startY; y <= endY; y += gridSize) {
    const isMajor = Math.round(y / gridSize) % 5 === 0;
    ctx.strokeStyle = isMajor ? COLORS.gridMajor : COLORS.grid;
    ctx.globalAlpha = isMajor ? 0.6 : 0.3;
    const p1 = worldToScreen({ x: startX, y }, view, width, height);
    const p2 = worldToScreen({ x: endX, y }, view, width, height);
    ctx.beginPath();
    ctx.moveTo(p1.x, p1.y);
    ctx.lineTo(p2.x, p2.y);
    ctx.stroke();
  }
  ctx.globalAlpha = 1;
  ctx.restore();
}

export function drawElements(
  ctx: CanvasRenderingContext2D,
  state: CanvasState,
  view: ViewTransform,
  width: number,
  height: number,
  selectedIds: Set<string>
) {
  // Rooms first (so walls/openings render on top of floor fills)
  for (const el of state.elements) {
    if (isRoom(el)) drawRoom(ctx, el, view, width, height, selectedIds.has(el.id));
  }
  for (const el of state.elements) {
    if (isWall(el)) drawWall(ctx, el, view, width, height, selectedIds.has(el.id));
  }
  for (const el of state.elements) {
    if (isOpening(el)) drawOpening(ctx, el, state.elements, view, width, height, selectedIds.has(el.id));
  }
}

function drawWall(
  ctx: CanvasRenderingContext2D,
  wall: WallElement,
  view: ViewTransform,
  width: number,
  height: number,
  selected: boolean
) {
  const p1 = worldToScreen({ x: wall.x1, y: wall.y1 }, view, width, height);
  const p2 = worldToScreen({ x: wall.x2, y: wall.y2 }, view, width, height);
  ctx.save();
  ctx.strokeStyle = selected ? COLORS.wallSelected : COLORS.wall;
  ctx.lineWidth = Math.max(2, wall.thickness * view.scale);
  ctx.lineCap = 'square';
  ctx.beginPath();
  ctx.moveTo(p1.x, p1.y);
  ctx.lineTo(p2.x, p2.y);
  ctx.stroke();
  ctx.restore();
}

function drawRoom(
  ctx: CanvasRenderingContext2D,
  room: RoomElement,
  view: ViewTransform,
  width: number,
  height: number,
  selected: boolean
) {
  if (room.points.length < 3) return;
  ctx.save();
  ctx.beginPath();
  const first = worldToScreen(room.points[0], view, width, height);
  ctx.moveTo(first.x, first.y);
  for (const pt of room.points.slice(1)) {
    const p = worldToScreen(pt, view, width, height);
    ctx.lineTo(p.x, p.y);
  }
  ctx.closePath();
  ctx.fillStyle = room.floorColor || COLORS.room;
  ctx.globalAlpha = 0.7;
  ctx.fill();
  ctx.globalAlpha = 1;
  ctx.strokeStyle = selected ? COLORS.wallSelected : COLORS.roomStroke;
  ctx.lineWidth = selected ? 2 : 1;
  ctx.stroke();

  if (room.label) {
    const cx = room.points.reduce((s, p) => s + p.x, 0) / room.points.length;
    const cy = room.points.reduce((s, p) => s + p.y, 0) / room.points.length;
    const center = worldToScreen({ x: cx, y: cy }, view, width, height);
    ctx.fillStyle = COLORS.wall;
    ctx.font = '500 13px Inter, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(room.label, center.x, center.y);
  }
  ctx.restore();
}

function drawOpening(
  ctx: CanvasRenderingContext2D,
  opening: OpeningElement,
  elements: CanvasElement[],
  view: ViewTransform,
  width: number,
  height: number,
  selected: boolean
) {
  const wall = elements.find((e) => e.id === opening.hostWallId && isWall(e)) as WallElement | undefined;
  if (!wall) return;

  const wallLength = Math.hypot(wall.x2 - wall.x1, wall.y2 - wall.y1);
  if (wallLength < 1e-6) return;
  const dirX = (wall.x2 - wall.x1) / wallLength;
  const dirY = (wall.y2 - wall.y1) / wallLength;

  const centerX = wall.x1 + dirX * opening.x;
  const centerY = wall.y1 + dirY * opening.y;
  const halfWidth = opening.width / 2;

  const startWorld = { x: centerX - dirX * halfWidth, y: centerY - dirY * halfWidth };
  const endWorld = { x: centerX + dirX * halfWidth, y: centerY + dirY * halfWidth };
  const p1 = worldToScreen(startWorld, view, width, height);
  const p2 = worldToScreen(endWorld, view, width, height);

  ctx.save();
  ctx.strokeStyle = selected ? COLORS.wallSelected : opening.type === 'DOOR' ? COLORS.door : COLORS.window;
  ctx.lineWidth = Math.max(3, wall.thickness * view.scale + 2);
  // Draw opening as a gap-highlight over the wall (a colored segment)
  ctx.beginPath();
  ctx.moveTo(p1.x, p1.y);
  ctx.lineTo(p2.x, p2.y);
  ctx.stroke();

  if (opening.type === 'DOOR') {
    // door swing arc
    ctx.strokeStyle = COLORS.door;
    ctx.lineWidth = 1;
    ctx.setLineDash([3, 3]);
    const radius = opening.width * view.scale;
    const perpX = -dirY;
    const perpY = dirX;
    const hinge = p1;
    const arcStart = Math.atan2(dirY, dirX);
    ctx.beginPath();
    ctx.arc(hinge.x, hinge.y, radius, arcStart, arcStart + Math.PI / 2);
    ctx.stroke();
    ctx.setLineDash([]);
    void perpX;
    void perpY;
  }
  ctx.restore();
}

export function hitTestElement(point: Point, state: CanvasState, tolerance = 0.2): CanvasElement | null {
  // Test openings and walls (thin elements) before rooms (large fill areas)
  for (const el of state.elements) {
    if (isWall(el)) {
      const d = distanceToSegment(point, { x: el.x1, y: el.y1 }, { x: el.x2, y: el.y2 });
      if (d < tolerance + el.thickness / 2) return el;
    }
  }
  for (const el of state.elements) {
    if (isRoom(el) && pointInPolygon(point, el.points)) return el;
  }
  return null;
}

function distanceToSegment(p: Point, a: Point, b: Point): number {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  const lenSq = dx * dx + dy * dy;
  if (lenSq < 1e-9) return Math.hypot(p.x - a.x, p.y - a.y);
  let t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / lenSq;
  t = Math.max(0, Math.min(1, t));
  const projX = a.x + t * dx;
  const projY = a.y + t * dy;
  return Math.hypot(p.x - projX, p.y - projY);
}

function pointInPolygon(point: Point, polygon: Point[]): boolean {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const xi = polygon[i].x, yi = polygon[i].y;
    const xj = polygon[j].x, yj = polygon[j].y;
    const intersect =
      yi > point.y !== yj > point.y &&
      point.x < ((xj - xi) * (point.y - yi)) / (yj - yi) + xi;
    if (intersect) inside = !inside;
  }
  return inside;
}
