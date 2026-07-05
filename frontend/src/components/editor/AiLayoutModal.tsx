import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { generateAiLayout } from '../../api/aiLayout';
import { parseCanvasStateJson } from '../../engine/schemaMigration';
import { useEditorStore } from '../../store/editorStore';

interface AiLayoutModalProps {
  projectId: string;
  hasExistingDrawing: boolean;
  onClose: () => void;
}

export default function AiLayoutModal({ projectId, hasExistingDrawing, onClose }: AiLayoutModalProps) {
  const [buildingType, setBuildingType] = useState('3-bedroom bungalow');
  const [bedrooms, setBedrooms] = useState('3');
  const [brief, setBrief] = useState('');
  const [confirmOverwrite, setConfirmOverwrite] = useState(!hasExistingDrawing);
  const [fallbackNote, setFallbackNote] = useState<string | null>(null);
  const applyGeneratedLayout = useEditorStore((s) => s.applyGeneratedLayout);

  const mutation = useMutation({
    mutationFn: () =>
      generateAiLayout(projectId, {
        buildingType,
        bedrooms: bedrooms ? parseInt(bedrooms, 10) : undefined,
        brief,
      }),
    onSuccess: (res) => {
      applyGeneratedLayout(parseCanvasStateJson(res.canvasStateJson));
      if (res.usedFreeFallback && res.note) {
        // Don't silently close — the person should see that this wasn't
        // generated from their brief's wording before they trust the result.
        setFallbackNote(res.note);
      } else {
        onClose();
      }
    },
  });

  const errorMessage = (mutation.error as any)?.response?.data?.message || (mutation.error as any)?.message;

  if (fallbackNote) {
    return (
      <div className="fixed inset-0 bg-ink/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
        <div
          className="bg-surface rounded-xl border border-stroke w-full max-w-md p-6 panel-enter"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-center gap-2 mb-2">
            <i className="ti ti-info-circle text-accent-gold text-lg" />
            <h2 className="font-serif text-lg text-ink">Layout applied — using the built-in generator</h2>
          </div>
          <p className="font-sans text-sm text-ink leading-relaxed mb-5">{fallbackNote}</p>
          <button
            onClick={onClose}
            className="w-full h-10 bg-accent-primary text-white rounded-md font-sans text-sm font-medium hover:bg-accent-secondary transition-colors duration-200"
          >
            Got it
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-ink/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div
        className="bg-surface rounded-xl border border-stroke w-full max-w-md p-6 panel-enter"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center gap-2 mb-1">
          <i className="ti ti-robot text-accent-primary text-lg" />
          <h2 className="font-serif text-xl text-ink">AI room planner</h2>
        </div>
        <p className="font-sans text-xs text-muted mb-5">
          Describe your brief and get a starter layout you can refine in the editor. If no AI key is configured on
          this server, this falls back to a free built-in generator using your bedroom count and plot size (it
          won't read the wording of your brief in that case — you'll be told clearly if that happens).
        </p>

        {!hasExistingDrawing ? null : (
          <div className="mb-4 p-3 border border-accent-gold/40 bg-accent-gold/[0.06] rounded-md">
            <label className="flex items-start gap-2 font-sans text-xs text-ink cursor-pointer">
              <input
                type="checkbox"
                checked={confirmOverwrite}
                onChange={(e) => setConfirmOverwrite(e.target.checked)}
                className="mt-0.5"
              />
              This project already has a drawing — generating a new layout will replace it. (You can still undo
              afterward.)
            </label>
          </div>
        )}

        <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">
          Building type
        </label>
        <input
          value={buildingType}
          onChange={(e) => setBuildingType(e.target.value)}
          placeholder="e.g. 3-bedroom bungalow"
          className="w-full h-9 mb-3 px-3 border border-stroke rounded-md font-sans text-sm bg-canvas outline-none focus:border-accent-primary transition-colors duration-150"
        />

        <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">Bedrooms</label>
        <input
          type="number"
          min="0"
          value={bedrooms}
          onChange={(e) => setBedrooms(e.target.value)}
          className="w-full h-9 mb-3 px-3 border border-stroke rounded-md font-sans text-sm bg-canvas outline-none focus:border-accent-primary transition-colors duration-150"
        />

        <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">
          Brief for the client
        </label>
        <textarea
          value={brief}
          onChange={(e) => setBrief(e.target.value)}
          placeholder="e.g. Family of 5, open-plan living and dining, en-suite master bedroom, separate kitchen with pantry access."
          rows={4}
          className="w-full mb-4 px-3 py-2 border border-stroke rounded-md font-sans text-sm bg-canvas outline-none focus:border-accent-primary transition-colors duration-150 resize-none"
        />

        {errorMessage && <div className="mb-3 text-xs text-danger">{errorMessage}</div>}

        <div className="flex gap-2">
          <button
            onClick={onClose}
            className="flex-1 h-10 border border-stroke rounded-md font-sans text-sm text-ink hover:bg-canvas transition-colors duration-150"
          >
            Cancel
          </button>
          <button
            onClick={() => mutation.mutate()}
            disabled={!brief.trim() || !confirmOverwrite || mutation.isPending}
            className="flex-1 h-10 bg-accent-primary text-white rounded-md font-sans text-sm font-medium hover:bg-accent-secondary transition-colors duration-200 disabled:opacity-50"
          >
            {mutation.isPending ? 'Generating…' : 'Generate layout'}
          </button>
        </div>
        <p className="font-sans text-[10px] text-muted mt-3 text-center">Limited to 5 generations per hour.</p>
      </div>
    </div>
  );
}
