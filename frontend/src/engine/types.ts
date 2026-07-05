export type Tool = 'SELECT' | 'WALL' | 'ROOM' | 'DOOR' | 'WINDOW' | 'PAN';

export type ElementType = 'WALL' | 'ROOM' | 'DOOR' | 'WINDOW';

export interface Point {
  x: number;
  y: number;
}

export interface WallElement {
  id: string;
  type: 'WALL';
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  thickness: number; // meters
  height: number; // meters
}

export interface RoomElement {
  id: string;
  type: 'ROOM';
  points: Point[];
  label: string;
  floorColor: string;
}

export interface OpeningElement {
  id: string;
  type: 'DOOR' | 'WINDOW';
  hostWallId: string;
  /** distance in meters along the host wall from (x1,y1) to the opening's center */
  x: number;
  y: number;
  width: number; // meters
  swing?: 'LEFT' | 'RIGHT';
}

export type CanvasElement = WallElement | RoomElement | OpeningElement;

export interface CanvasState {
  schemaVersion: string;
  elements: CanvasElement[];
  meta: {
    unit: 'm';
    gridSize: number;
  };
}

export const SCHEMA_VERSION = '1.1';

export function emptyCanvasState(): CanvasState {
  return {
    schemaVersion: SCHEMA_VERSION,
    elements: [],
    meta: { unit: 'm', gridSize: 0.5 },
  };
}

export function isWall(el: CanvasElement): el is WallElement {
  return el.type === 'WALL';
}

export function isRoom(el: CanvasElement): el is RoomElement {
  return el.type === 'ROOM';
}

export function isOpening(el: CanvasElement): el is OpeningElement {
  return el.type === 'DOOR' || el.type === 'WINDOW';
}
