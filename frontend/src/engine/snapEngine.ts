import type { CanvasElement, Point, WallElement } from './types';
import { isWall } from './types';

const SNAP_RADIUS_WORLD = 0.35; // meters — snap to nearby wall endpoints
const ANGLE_SNAP_DEGREES = 15; // wall angle snapping while drawing

export function snapToGrid(point: Point, gridSize: number): Point {
  return {
    x: Math.round(point.x / gridSize) * gridSize,
    y: Math.round(point.y / gridSize) * gridSize,
  };
}

/**
 * Snaps a candidate point to nearby wall endpoints first (so walls join cleanly),
 * falling back to grid snapping if nothing is close enough.
 */
export function snapPoint(point: Point, elements: CanvasElement[], gridSize: number): Point {
  const walls = elements.filter(isWall) as WallElement[];
  let best: Point | null = null;
  let bestDist = SNAP_RADIUS_WORLD;

  for (const wall of walls) {
    for (const endpoint of [
      { x: wall.x1, y: wall.y1 },
      { x: wall.x2, y: wall.y2 },
    ]) {
      const d = distance(point, endpoint);
      if (d < bestDist) {
        bestDist = d;
        best = endpoint;
      }
    }
  }

  return best ?? snapToGrid(point, gridSize);
}

/** Snaps the angle of a wall being drawn to the nearest 15-degree increment, holding length. */
export function snapAngle(origin: Point, target: Point): Point {
  const dx = target.x - origin.x;
  const dy = target.y - origin.y;
  const length = Math.hypot(dx, dy);
  if (length < 1e-6) return target;

  const angleRad = Math.atan2(dy, dx);
  const angleDeg = (angleRad * 180) / Math.PI;
  const snappedDeg = Math.round(angleDeg / ANGLE_SNAP_DEGREES) * ANGLE_SNAP_DEGREES;
  const snappedRad = (snappedDeg * Math.PI) / 180;

  return {
    x: origin.x + Math.cos(snappedRad) * length,
    y: origin.y + Math.sin(snappedRad) * length,
  };
}

export function distance(a: Point, b: Point): number {
  return Math.hypot(b.x - a.x, b.y - a.y);
}

/** Finds the nearest wall to a point within a tolerance, used for placing doors/windows. */
export function findNearestWall(
  point: Point,
  elements: CanvasElement[],
  toleranceMeters = 0.5
): { wall: WallElement; distanceAlongWall: number; perpendicularDistance: number } | null {
  const walls = elements.filter(isWall) as WallElement[];
  let best: { wall: WallElement; distanceAlongWall: number; perpendicularDistance: number } | null = null;
  let bestPerp = toleranceMeters;

  for (const wall of walls) {
    const { distanceAlong, perpendicular } = projectOntoSegment(point, wall);
    if (distanceAlong < 0 || distanceAlong > wallLength(wall)) continue;
    if (perpendicular < bestPerp) {
      bestPerp = perpendicular;
      best = { wall, distanceAlongWall: distanceAlong, perpendicularDistance: perpendicular };
    }
  }

  return best;
}

function wallLength(wall: WallElement): number {
  return distance({ x: wall.x1, y: wall.y1 }, { x: wall.x2, y: wall.y2 });
}

function projectOntoSegment(
  point: Point,
  wall: WallElement
): { distanceAlong: number; perpendicular: number } {
  const dx = wall.x2 - wall.x1;
  const dy = wall.y2 - wall.y1;
  const lenSq = dx * dx + dy * dy;
  if (lenSq < 1e-9) {
    return { distanceAlong: 0, perpendicular: distance(point, { x: wall.x1, y: wall.y1 }) };
  }
  const t = ((point.x - wall.x1) * dx + (point.y - wall.y1) * dy) / lenSq;
  const projX = wall.x1 + t * dx;
  const projY = wall.y1 + t * dy;
  const perpendicular = distance(point, { x: projX, y: projY });
  const distanceAlong = t * Math.sqrt(lenSq);
  return { distanceAlong, perpendicular };
}
