import { useMemo } from 'react';
import { useEditorStore } from '../../store/editorStore';
import { computeRoomArea } from '../../engine/geometryValidation';
import { isRoom, isWall } from '../../engine/types';

interface StatusBarProps {
  saveState: 'saved' | 'saving' | 'error';
  location?: string;
  scale?: string;
}

export default function StatusBar({ saveState, location = 'No location set', scale = '1:100' }: StatusBarProps) {
  const canvasState = useEditorStore((s) => s.canvasState);

  const stats = useMemo(() => {
    const walls = canvasState.elements.filter(isWall);
    const rooms = canvasState.elements.filter(isRoom);
    const totalArea = rooms.reduce((sum, r) => sum + computeRoomArea(r.points), 0);
    return { wallCount: walls.length, roomCount: rooms.length, totalArea };
  }, [canvasState]);

  const saveLabel = saveState === 'saving' ? 'Saving…' : saveState === 'error' ? 'Save failed' : 'Saved';
  const dotColor = saveState === 'error' ? 'bg-danger' : saveState === 'saving' ? 'bg-accent-gold' : 'bg-accent-secondary';

  return (
    <div className="h-[26px] bg-panel-dark flex items-center px-3 gap-4 overflow-x-auto flex-shrink-0">
      <span className="font-mono text-[10px] text-[#9FE1CB] flex items-center gap-1.5 whitespace-nowrap">
        <span className={`w-1.5 h-1.5 rounded-full ${dotColor}`} /> {saveLabel}
      </span>
      <span className="w-px h-3.5 bg-white/[0.15]" />
      <span className="font-mono text-[10px] text-[#9FE1CB] flex items-center gap-1.5 whitespace-nowrap">
        <i className="ti ti-map-pin text-xs" /> {location}
      </span>
      <span className="w-px h-3.5 bg-white/[0.15]" />
      <span className="font-mono text-[10px] text-[#9FE1CB] flex items-center gap-1.5 whitespace-nowrap">
        <i className="ti ti-ruler text-xs" /> {scale}
      </span>
      <span className="w-px h-3.5 bg-white/[0.15]" />
      <span className="font-mono text-[10px] text-[#9FE1CB] whitespace-nowrap">
        {stats.wallCount} walls · {stats.roomCount} rooms · {stats.totalArea.toFixed(1)}m²
      </span>
      <span className="font-mono text-[10px] text-[#9FE1CB] ml-auto whitespace-nowrap">
        Schema {canvasState.schemaVersion}
      </span>
    </div>
  );
}
