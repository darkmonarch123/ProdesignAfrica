import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listRuleSets } from '../../api/compliance';
import { updateProject, type Project } from '../../api/projects';

interface PlotSettingsPopoverProps {
  project: Project;
  onClose: () => void;
}

export default function PlotSettingsPopover({ project, onClose }: PlotSettingsPopoverProps) {
  const queryClient = useQueryClient();
  const [width, setWidth] = useState(project.plotWidthMeters?.toString() ?? '');
  const [depth, setDepth] = useState(project.plotDepthMeters?.toString() ?? '');
  const [ruleCode, setRuleCode] = useState(project.complianceRuleCode ?? '');

  const { data: ruleSets } = useQuery({ queryKey: ['rulesets'], queryFn: listRuleSets });

  const mutation = useMutation({
    mutationFn: () =>
      updateProject(project.id, {
        name: project.name,
        location: project.location ?? undefined,
        description: project.description ?? undefined,
        complianceRuleCode: ruleCode || null,
        plotWidthMeters: width ? parseFloat(width) : null,
        plotDepthMeters: depth ? parseFloat(depth) : null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', project.id] });
      queryClient.invalidateQueries({ queryKey: ['compliance-report', project.id] });
      onClose();
    },
  });

  // Close on Escape
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="absolute right-3 sm:right-4 top-14 z-30 w-72 bg-surface border border-stroke rounded-lg shadow-lg p-4">
      <div className="font-sans text-sm font-medium text-ink mb-3">Plot &amp; compliance settings</div>

      <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1">
        Plot width (m, road-facing)
      </label>
      <input
        type="number"
        min="0"
        step="0.1"
        value={width}
        onChange={(e) => setWidth(e.target.value)}
        placeholder="e.g. 15"
        className="w-full h-9 mb-3 px-2.5 border border-stroke rounded-md font-mono text-xs bg-canvas outline-none focus:border-accent-primary transition-colors duration-150"
      />

      <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1">Plot depth (m)</label>
      <input
        type="number"
        min="0"
        step="0.1"
        value={depth}
        onChange={(e) => setDepth(e.target.value)}
        placeholder="e.g. 30"
        className="w-full h-9 mb-3 px-2.5 border border-stroke rounded-md font-mono text-xs bg-canvas outline-none focus:border-accent-primary transition-colors duration-150"
      />

      <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1">
        Compliance ruleset
      </label>
      <select
        value={ruleCode}
        onChange={(e) => setRuleCode(e.target.value)}
        className="w-full h-9 mb-4 px-2.5 border border-stroke rounded-md font-mono text-xs bg-canvas outline-none focus:border-accent-primary transition-colors duration-150"
      >
        <option value="">None</option>
        {ruleSets?.map((r) => (
          <option key={r.code} value={r.code}>
            {r.code} — {r.region}
          </option>
        ))}
      </select>

      <p className="font-sans text-[11px] text-muted mb-3 leading-relaxed">
        Your drawing is treated as plot-relative, with the road-facing edge at y = 0. Draw your building starting
        near the top of the canvas for accurate setback checks.
      </p>

      <div className="flex gap-2">
        <button
          onClick={onClose}
          className="flex-1 h-9 border border-stroke rounded-md font-sans text-xs text-ink hover:bg-canvas transition-colors duration-150"
        >
          Cancel
        </button>
        <button
          onClick={() => mutation.mutate()}
          disabled={mutation.isPending}
          className="flex-1 h-9 bg-accent-primary text-white rounded-md font-sans text-xs font-medium hover:bg-accent-secondary transition-colors duration-200 disabled:opacity-60"
        >
          {mutation.isPending ? 'Saving…' : 'Save'}
        </button>
      </div>
    </div>
  );
}
