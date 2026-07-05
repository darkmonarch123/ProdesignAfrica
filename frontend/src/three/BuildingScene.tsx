import { useMemo } from 'react';
import * as THREE from 'three';
import type { CanvasState, OpeningElement, WallElement } from '../engine/types';
import { isOpening, isRoom, isWall } from '../engine/types';
import { doorMaterial, floorMaterial, groundMaterial, wallMaterial, windowMaterial } from './materials';

interface BuildingSceneProps {
  canvasState: CanvasState;
}

/**
 * Converts a 2D wall segment into a 3D box mesh, cutting out any door/window
 * openings hosted on that wall as separate colored inserts positioned at the
 * correct height (doors sit on the floor, windows are centered vertically).
 */
export default function BuildingScene({ canvasState }: BuildingSceneProps) {
  const walls = useMemo(() => canvasState.elements.filter(isWall) as WallElement[], [canvasState]);
  const rooms = useMemo(() => canvasState.elements.filter(isRoom), [canvasState]);
  const openings = useMemo(() => canvasState.elements.filter(isOpening) as OpeningElement[], [canvasState]);

  // Normalize world coordinates (meters, XY plane) into three.js coordinates (X, Z ground plane, Y up).
  const toVec = (x: number, y: number) => new THREE.Vector3(x, 0, y);

  return (
    <group>
      {/* Ground plane */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.01, 0]} receiveShadow>
        <planeGeometry args={[200, 200]} />
        <primitive object={groundMaterial} attach="material" />
      </mesh>

      {/* Room floor slabs */}
      {rooms.map((room) => {
        if (!isRoom(room) || room.points.length < 3) return null;
        const shape = new THREE.Shape();
        room.points.forEach((p, i) => {
          if (i === 0) shape.moveTo(p.x, p.y);
          else shape.lineTo(p.x, p.y);
        });
        shape.closePath();
        return (
          <mesh key={room.id} rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.01, 0]} receiveShadow>
            <shapeGeometry args={[shape]} />
            <primitive object={floorMaterial} attach="material" />
          </mesh>
        );
      })}

      {/* Extruded walls with opening cutouts approximated via segmented boxes */}
      {walls.map((wall) => (
        <WallMesh key={wall.id} wall={wall} openings={openings.filter((o) => o.hostWallId === wall.id)} toVec={toVec} />
      ))}
    </group>
  );
}

function WallMesh({
  wall,
  openings,
  toVec,
}: {
  wall: WallElement;
  openings: OpeningElement[];
  toVec: (x: number, y: number) => THREE.Vector3;
}) {
  const start = toVec(wall.x1, wall.y1);
  const end = toVec(wall.x2, wall.y2);
  const length = start.distanceTo(end);
  const angle = Math.atan2(end.z - start.z, end.x - start.x);
  const mid = start.clone().lerp(end, 0.5);
  const height = wall.height || 3;

  // Sort openings along the wall so we can build wall segments between them.
  const sorted = [...openings].sort((a, b) => a.x - b.x);

  const segments: { start: number; end: number }[] = [];
  let cursor = 0;
  for (const opening of sorted) {
    const halfW = opening.width / 2;
    const openStart = Math.max(0, opening.x - halfW);
    const openEnd = Math.min(length, opening.x + halfW);
    if (openStart > cursor) segments.push({ start: cursor, end: openStart });
    cursor = Math.max(cursor, openEnd);
  }
  if (cursor < length) segments.push({ start: cursor, end: length });
  if (segments.length === 0) segments.push({ start: 0, end: length });

  return (
    <group position={mid} rotation={[0, -angle, 0]}>
      {segments.map((seg, i) => {
        const segLength = seg.end - seg.start;
        if (segLength <= 0.01) return null;
        const segCenter = seg.start + segLength / 2 - length / 2;
        return (
          <mesh key={i} position={[segCenter, height / 2, 0]} castShadow receiveShadow>
            <boxGeometry args={[segLength, height, wall.thickness]} />
            <primitive object={wallMaterial} attach="material" />
          </mesh>
        );
      })}

      {/* Openings: doors go floor-to-2.1m, windows sit at sill height */}
      {sorted.map((opening) => {
        const segCenter = opening.x - length / 2;
        const isDoor = opening.type === 'DOOR';
        const openingHeight = isDoor ? 2.1 : 1.2;
        const sill = isDoor ? 0 : 0.9;
        return (
          <mesh key={opening.id} position={[segCenter, sill + openingHeight / 2, 0]}>
            <boxGeometry args={[opening.width, openingHeight, wall.thickness * 0.6]} />
            <primitive object={isDoor ? doorMaterial : windowMaterial} attach="material" />
          </mesh>
        );
      })}
    </group>
  );
}
