import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useEditorStore } from '../../store/editorStore';
import { computeRoomArea } from '../../engine/geometryValidation';
import { isRoom, isWall, type CanvasElement } from '../../engine/types';
import { getComplianceReport, type ComplianceStatus } from '../../api/compliance';

const CHIP_STYLES: Record<ComplianceStatus, string> = {
  OK: 'bg-[#f0faf6] text-[#065f46] border-accent-secondary',
  WARN: 'bg-[#fff7ed] text-[#92400e] border-warn',
  FAIL: 'bg-[#fff1f2] text-[#9f1239] border-danger',
  INFO: 'bg-canvas text-muted border-stroke',
};

const CHIP_ICON: Record<ComplianceStatus, string> = {
  OK: 'ti-circle-check',
  WARN: 'ti-alert-triangle',
  FAIL: 'ti-x',
  INFO: 'ti-info-circle',
};

interface PropsPanelProps {
  projectId?: string;
}

export default function PropsPanel({ projectId }: PropsPanelProps) {
  const canvasState = useEditorStore((s) => s.canvasState);
  const selectedIds = useEditorStore((s) => s.selectedIds);

  const selectedElement = useMemo(() => {
    const id = Array.from(selectedIds)[0];
    return canvasState.elements.find((el) => el.id === id) ?? null;
  }, [canvasState, selectedIds]);

  const { data: report, isLoading } = useQuery({
    queryKey: ['compliance-report', projectId],
    queryFn: () => getComplianceReport(projectId!),
    enabled: !!projectId,
  });

  return (
    <div className="w-[220px] flex-shrink-0 bg-surface border-l border-stroke overflow-y-auto p-4 hidden lg:block">
      <div className="font-sans text-[11px] font-medium text-muted tracking-widest uppercase pb-2 border-b border-stroke mb-4">
        Selection
      </div>
      <div className="mb-5">
        {selectedElement ? (
          <SelectedElementDetails element={selectedElement} />
        ) : (
          <p className="font-sans text-xs text-muted">Click a wall or room to see its properties.</p>
        )}
      </div>

      <div className="font-sans text-[11px] font-medium text-muted tracking-widest uppercase pb-2 border-b border-stroke mb-4">
        Layers
      </div>
      <div className="mb-5">
        {[
          { color: '#047857', label: 'Rooms' },
          { color: '#1A1A2E', label: 'Walls' },
          { color: '#C2692A', label: 'Dimensions' },
        ].map((layer) => (
          <div key={layer.label} className="flex items-center justify-between mb-2.5">
            <span className="flex items-center gap-1.5 font-mono text-[10px] text-muted">
              <span className="w-2 h-2 rounded-sm inline-block" style={{ background: layer.color }} />
              {layer.label}
            </span>
            <i className="ti ti-eye text-sm text-muted" />
          </div>
        ))}
      </div>

      <div className="font-sans text-[11px] font-medium text-muted tracking-widest uppercase pb-2 border-b border-stroke mb-4">
        Compliance
      </div>
      <div className="mb-4">
        {!projectId && <p className="font-sans text-xs text-muted">No project loaded.</p>}
        {projectId && isLoading && <p className="font-sans text-xs text-muted">Checking…</p>}
        {report?.checks.map((chip) => (
          <div
            key={chip.code}
            className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-md mb-1.5 font-mono text-[10px] border ${CHIP_STYLES[chip.status]}`}
            title={chip.detail}
          >
            <i className={`ti ${CHIP_ICON[chip.status]} text-sm flex-shrink-0`} />
            <span className="truncate">
              {chip.status === 'INFO' ? chip.detail : `${chip.label} ${chip.detail}`}
            </span>
          </div>
        ))}
      </div>
      <button
        disabled
        title="AI-assisted compliance fixes — coming soon"
        className="w-full h-8 border border-accent-primary/40 rounded-md bg-green-bg/40 text-accent-primary/50 font-sans text-xs flex items-center justify-center gap-1.5 cursor-not-allowed"
      >
        <i className="ti ti-robot" /> Fix with AI ↗
      </button>
    </div>
  );
}

function SelectedElementDetails({ element }: { element: CanvasElement }) {
  if (isWall(element)) {
    const length = Math.hypot(element.x2 - element.x1, element.y2 - element.y1);
    return (
      <div>
        <div className="font-sans text-xs font-medium text-ink mb-2.5">Wall</div>
        <Row label="Length" value={`${length.toFixed(2)} m`} />
        <Row label="Thickness" value={`${element.thickness.toFixed(2)} m`} />
        <Row label="Height" value={`${element.height.toFixed(2)} m`} />
      </div>
    );
  }
  if (isRoom(element)) {
    const area = computeRoomArea(element.points);
    return (
      <div>
        <div className="font-sans text-xs font-medium text-ink mb-2.5">{element.label || 'Room'}</div>
        <Row label="Area" value={`${area.toFixed(2)} m²`} />
        <Row label="Points" value={String(element.points.length)} />
      </div>
    );
  }
  return (
    <div>
      <div className="font-sans text-xs font-medium text-ink mb-2.5">{element.type}</div>
      <Row label="Width" value={`${element.width.toFixed(2)} m`} />
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between mb-2.5">
      <span className="font-mono text-[10px] text-muted tracking-wider">{label}</span>
      <span className="font-mono text-[11px] text-ink font-medium">{value}</span>
    </div>
  );
}
