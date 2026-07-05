import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import CadCanvas, { type CadCanvasHandle } from '../components/editor/CadCanvas';
import ToolSidebar from '../components/editor/ToolSidebar';
import PropsPanel from '../components/editor/PropsPanel';
import StatusBar from '../components/editor/StatusBar';
import PlotSettingsPopover from '../components/editor/PlotSettingsPopover';
import AiLayoutModal from '../components/editor/AiLayoutModal';
import PresenceStack from '../components/editor/PresenceStack';
import ExportModal from '../components/editor/ExportModal';
import { useCollabSocket } from '../hooks/useCollabSocket';
import ThreeDView from '../three/ThreeDView';
import { useEditorStore } from '../store/editorStore';
import { getLatestSnapshot, saveSnapshot } from '../api/snapshots';
import { parseCanvasStateJson } from '../engine/schemaMigration';
import { validateCanvasState } from '../engine/geometryValidation';
import { getProject } from '../api/projects';

type ViewMode = '2D' | '3D';

export default function EditorPage() {
  const { id: projectId } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [viewMode, setViewMode] = useState<ViewMode>('2D');
  const [saveState, setSaveState] = useState<'saved' | 'saving' | 'error'>('saved');
  const [showPlotSettings, setShowPlotSettings] = useState(false);
  const [showAiModal, setShowAiModal] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);
  const canvasRef = useRef<CadCanvasHandle>(null);
  const canvasState = useEditorStore((s) => s.canvasState);
  const loadState = useEditorStore((s) => s.loadState);
  const applyRemoteState = useEditorStore((s) => s.applyRemoteState);
  const skipNextBroadcastRef = useRef(false);

  const { presence, remoteCursors, connected, sendCursor, sendState, onRemoteState } = useCollabSocket(projectId);

  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => getProject(projectId!),
    enabled: !!projectId,
  });

  const { data: snapshot } = useQuery({
    queryKey: ['snapshot', projectId],
    queryFn: () => getLatestSnapshot(projectId!),
    enabled: !!projectId,
  });

  useEffect(() => {
    if (snapshot?.canvasStateJson) {
      loadState(parseCanvasStateJson(snapshot.canvasStateJson));
    }
  }, [snapshot, loadState]);

  // Apply a collaborator's live edit as soon as it arrives, and mark the next
  // outgoing-broadcast cycle to be skipped so we don't immediately echo their
  // own state back at them.
  useEffect(() => {
    onRemoteState((msg) => {
      skipNextBroadcastRef.current = true;
      applyRemoteState(parseCanvasStateJson(msg.canvasStateJson));
    });
  }, [onRemoteState, applyRemoteState]);

  // Live-broadcast the local canvas state to collaborators, throttled to
  // avoid flooding the socket on every keystroke. This is a live preview
  // channel, not the source of truth — see CollabController's Javadoc for
  // the last-write-wins limitation.
  useEffect(() => {
    if (!projectId) return;
    if (skipNextBroadcastRef.current) {
      skipNextBroadcastRef.current = false;
      return;
    }
    const timer = setTimeout(() => {
      sendState(canvasState.schemaVersion, JSON.stringify(canvasState));
    }, 400);
    return () => clearTimeout(timer);
  }, [canvasState, projectId, sendState]);

  // Debounced autosave whenever the canvas state changes. On success, also
  // refresh the compliance report so checks reflect the latest geometry.
  useEffect(() => {
    if (!projectId) return;
    const issues = validateCanvasState(canvasState);
    if (issues.length > 0) return; // don't autosave invalid intermediate states
    setSaveState('saving');
    const timer = setTimeout(() => {
      saveSnapshot(projectId, canvasState, true)
        .then(() => {
          setSaveState('saved');
          queryClient.invalidateQueries({ queryKey: ['compliance-report', projectId] });
        })
        .catch(() => setSaveState('error'));
    }, 1500);
    return () => clearTimeout(timer);
  }, [canvasState, projectId, queryClient]);

  return (
    <div className="relative flex flex-col h-screen bg-canvas">
      {/* Dark topbar */}
      <div className="bg-panel-dark h-12 flex items-center px-3 sm:px-4 gap-2 sm:gap-3 flex-shrink-0 overflow-x-auto">
        <span className="font-serif text-sm text-white flex-shrink-0">
          Pro<span className="text-accent-secondary">design</span>
        </span>
        <span className="font-mono text-[11px] text-[#9FE1CB] border-l border-white/20 pl-3 truncate max-w-[160px] sm:max-w-xs">
          {project?.name ?? 'Untitled project'}
        </span>
        <div className="ml-auto flex items-center gap-1.5 sm:gap-2 flex-shrink-0">
          <button
            onClick={() => setViewMode('2D')}
            className={`h-[30px] px-3 rounded-md font-sans text-[11px] flex items-center gap-1.5 border transition-colors duration-150 ${
              viewMode === '2D'
                ? 'bg-accent-primary border-accent-primary text-white'
                : 'border-white/20 text-white hover:bg-white/10'
            }`}
          >
            <i className="ti ti-box text-sm" /> 2D
          </button>
          <button
            onClick={() => setViewMode('3D')}
            className={`h-[30px] px-3 rounded-md font-sans text-[11px] flex items-center gap-1.5 border transition-colors duration-150 ${
              viewMode === '3D'
                ? 'bg-accent-primary border-accent-primary text-white'
                : 'border-white/20 text-white hover:bg-white/10'
            }`}
          >
            <i className="ti ti-cube text-sm" /> 3D view
          </button>
          <button
            onClick={() => setShowAiModal(true)}
            title="AI room planner"
            className="h-[30px] px-3 rounded-md font-sans text-[11px] flex items-center gap-1.5 border border-white/20 text-white hover:bg-white/10 transition-colors duration-150"
          >
            <i className="ti ti-robot text-sm" /> <span className="hidden sm:inline">AI Layout</span>
          </button>
          <button
            onClick={() => setShowPlotSettings((v) => !v)}
            title="Plot & compliance settings"
            className="h-[30px] px-3 rounded-md font-sans text-[11px] flex items-center gap-1.5 border border-white/20 text-white hover:bg-white/10 transition-colors duration-150"
          >
            <i className="ti ti-shield-check text-sm" /> <span className="hidden sm:inline">Compliance</span>
          </button>
          <button
            disabled
            title="Share — coming soon"
            className="hidden sm:flex h-[30px] px-3 rounded-md font-sans text-[11px] items-center gap-1.5 border border-white/10 text-white/40 cursor-not-allowed"
          >
            <i className="ti ti-share text-sm" /> Share
          </button>
          <button
            onClick={() => setShowExportModal(true)}
            title="Export BOQ"
            className="hidden sm:flex h-[30px] px-3 rounded-md font-sans text-[11px] items-center gap-1.5 border border-white/20 text-white hover:bg-white/10 transition-colors duration-150"
          >
            <i className="ti ti-file-export text-sm" /> Export
          </button>
          <PresenceStack users={presence} connected={connected} />
          <span className="hidden md:flex h-[30px] px-3 rounded-md font-sans text-[11px] items-center gap-1.5 bg-accent-primary text-white">
            <i className="ti ti-device-floppy text-sm" />
            {saveState === 'saving' ? 'Saving…' : saveState === 'error' ? 'Save failed' : 'Saved'}
          </span>
        </div>
      </div>

      {showPlotSettings && project && (
        <PlotSettingsPopover project={project} onClose={() => setShowPlotSettings(false)} />
      )}

      {showAiModal && projectId && (
        <AiLayoutModal
          projectId={projectId}
          hasExistingDrawing={canvasState.elements.length > 0}
          onClose={() => setShowAiModal(false)}
        />
      )}

      {showExportModal && projectId && project && (
        <ExportModal projectId={projectId} projectName={project.name} onClose={() => setShowExportModal(false)} />
      )}

      <main className="flex-1 flex overflow-hidden">
        {viewMode === '2D' && <ToolSidebar onZoomIn={() => canvasRef.current?.zoomIn()} onZoomOut={() => canvasRef.current?.zoomOut()} />}
        <div className="flex-1 relative overflow-hidden">
          {viewMode === '2D' ? (
            <CadCanvas ref={canvasRef} remoteCursors={remoteCursors} onCursorMove={sendCursor} />
          ) : (
            <ThreeDView />
          )}
        </div>
        {viewMode === '2D' && <PropsPanel projectId={projectId} />}
      </main>

      <StatusBar saveState={saveState} location={project?.location ?? undefined} />
    </div>
  );
}
