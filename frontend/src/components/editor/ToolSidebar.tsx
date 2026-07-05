import { useEditorStore } from '../../store/editorStore';
import type { Tool } from '../../engine/types';
import clsx from 'clsx';

const TOOLS: { id: Tool; label: string; icon: string }[] = [
  { id: 'SELECT', label: 'Select', icon: 'ti-pointer' },
  { id: 'WALL', label: 'Wall', icon: 'ti-layout-sidebar' },
  { id: 'DOOR', label: 'Door', icon: 'ti-door' },
  { id: 'WINDOW', label: 'Window', icon: 'ti-border-all' },
  { id: 'ROOM', label: 'Room', icon: 'ti-layout-2' },
];

// Present in the reference mockup but not wired to real behavior yet — shown
// dimmed with a "coming soon" tooltip rather than silently omitted, so the
// toolbar matches the design without pretending these tools work.
const COMING_SOON = [
  { label: 'Stairs', icon: 'ti-stairs' },
  { label: 'Dimension', icon: 'ti-ruler-measure' },
  { label: 'Text', icon: 'ti-text-size' },
  { label: 'Hatch', icon: 'ti-texture' },
];

interface ToolSidebarProps {
  onZoomIn?: () => void;
  onZoomOut?: () => void;
}

function ToolButton({
  icon,
  label,
  active,
  disabled,
  title,
  onClick,
}: {
  icon: string;
  label: string;
  active?: boolean;
  disabled?: boolean;
  title: string;
  onClick?: () => void;
}) {
  return (
    <button
      onClick={onClick}
      title={title}
      disabled={disabled}
      className={clsx(
        'w-11 h-11 rounded-md flex flex-col items-center justify-center gap-0.5 transition-colors duration-150',
        disabled
          ? 'text-stroke cursor-not-allowed'
          : active
          ? 'bg-green-bg text-accent-primary'
          : 'text-muted hover:bg-canvas hover:text-ink'
      )}
    >
      <i className={`ti ${icon} text-base leading-none`} />
      <span className="font-mono text-[8px] leading-none tracking-tight">{label}</span>
    </button>
  );
}

export default function ToolSidebar({ onZoomIn, onZoomOut }: ToolSidebarProps) {
  const { activeTool, setTool } = useEditorStore();

  return (
    <div className="w-14 flex-shrink-0 bg-surface border-r border-stroke flex flex-col items-center py-3 gap-1 overflow-y-auto">
      {TOOLS.map((tool) => (
        <ToolButton
          key={tool.id}
          icon={tool.icon}
          label={tool.label}
          active={activeTool === tool.id}
          title={tool.label}
          onClick={() => setTool(tool.id)}
        />
      ))}

      <div className="w-6 h-px bg-stroke my-1.5" />

      {COMING_SOON.map((tool) => (
        <ToolButton key={tool.label} icon={tool.icon} label={tool.label} disabled title={`${tool.label} — coming soon`} />
      ))}

      <div className="w-6 h-px bg-stroke my-1.5" />

      <ToolButton icon="ti-zoom-in" label="Zoom in" title="Zoom in" onClick={onZoomIn} />
      <ToolButton icon="ti-zoom-out" label="Zoom out" title="Zoom out" onClick={onZoomOut} />
      <ToolButton
        icon="ti-hand-grab"
        label="Pan"
        title="Pan"
        active={activeTool === 'PAN'}
        onClick={() => setTool('PAN')}
      />
    </div>
  );
}
