import type { CanvasElement, CanvasState } from './types';
import { isOpening, isRoom, isWall } from './types';

export interface ValidationIssue {
  elementId: string;
  message: string;
}

/**
 * Validates a CanvasState before it is persisted or exported. This mirrors the
 * structural checks performed server-side in GeometryValidator.java, plus a few
 * client-only checks (dangling openings, degenerate rooms) that are cheap to run
 * on every edit but unnecessary to duplicate server-side.
 */
export function validateCanvasState(state: CanvasState): ValidationIssue[] {
  const issues: ValidationIssue[] = [];
  const wallIds = new Set(state.elements.filter(isWall).map((w) => w.id));

  for (const element of state.elements) {
    if (isWall(element)) {
      const length = Math.hypot(element.x2 - element.x1, element.y2 - element.y1);
      if (length < 0.05) {
        issues.push({ elementId: element.id, message: 'Wall is too short (below 5cm)' });
      }
      if (element.thickness <= 0 || element.thickness > 1) {
        issues.push({ elementId: element.id, message: 'Wall thickness must be between 0 and 1m' });
      }
    }
    if (isRoom(element)) {
      if (element.points.length < 3) {
        issues.push({ elementId: element.id, message: 'Room needs at least 3 points' });
      }
    }
    if (isOpening(element)) {
      if (!wallIds.has(element.hostWallId)) {
        issues.push({ elementId: element.id, message: `${element.type} references a missing host wall` });
      }
      if (element.width <= 0) {
        issues.push({ elementId: element.id, message: `${element.type} width must be positive` });
      }
    }
  }

  return issues;
}

export function isValid(state: CanvasState): boolean {
  return validateCanvasState(state).length === 0;
}

export function computeRoomArea(points: { x: number; y: number }[]): number {
  // Shoelace formula
  let area = 0;
  for (let i = 0; i < points.length; i++) {
    const j = (i + 1) % points.length;
    area += points[i].x * points[j].y;
    area -= points[j].x * points[i].y;
  }
  return Math.abs(area) / 2;
}

export function computeWallLength(element: CanvasElement): number {
  if (!isWall(element)) return 0;
  return Math.hypot(element.x2 - element.x1, element.y2 - element.y1);
}
