import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { listTemplates, useTemplate, type TemplateSummary } from '../api/templates';

const BEDROOM_FILTERS = [null, 1, 2, 3, 4, 5, 6] as const;

export default function TemplatesPage() {
  const navigate = useNavigate();
  const [bedrooms, setBedrooms] = useState<number | null>(null);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<TemplateSummary | null>(null);
  const [projectName, setProjectName] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['templates', bedrooms, page],
    queryFn: () => listTemplates(bedrooms, page, 24),
  });

  const useMutationResult = useMutation({
    mutationFn: () =>
      useTemplate(
        selected!.id,
        projectName || selected!.name,
        selected!.suggestedPlotWidthMeters,
        selected!.suggestedPlotDepthMeters
      ),
    onSuccess: (project) => navigate(`/editor/${project.id}`),
  });

  return (
    <div className="min-h-screen bg-canvas">
      <div className="bg-surface border-b border-stroke px-4 sm:px-6 h-[52px] flex items-center justify-between">
        <div className="font-serif text-lg text-ink">
          Pro<span className="text-accent-primary">design</span> Africa — Templates
        </div>
        <button onClick={() => navigate('/dashboard')} className="font-sans text-sm text-muted hover:text-ink">
          <i className="ti ti-arrow-left text-sm" /> Dashboard
        </button>
      </div>

      <div className="p-4 sm:p-6">
        <h1 className="font-serif text-2xl text-ink mb-1">Starter floor plans</h1>
        <p className="font-sans text-sm text-muted mb-5">
          {data ? `${data.totalElements.toLocaleString()} generated layouts` : 'Loading…'} — pick a bedroom count and
          plot size, then customize in the editor.
        </p>

        <div className="flex gap-2 mb-6 overflow-x-auto pb-1">
          {BEDROOM_FILTERS.map((b) => (
            <button
              key={b ?? 'all'}
              onClick={() => {
                setBedrooms(b);
                setPage(0);
              }}
              className={`flex-shrink-0 px-3 py-1.5 rounded-full font-sans text-xs font-medium border transition-colors duration-150 ${
                bedrooms === b
                  ? 'bg-accent-primary text-white border-accent-primary'
                  : 'bg-surface text-ink border-stroke hover:bg-canvas'
              }`}
            >
              {b === null ? 'All' : b === 1 ? 'Studio / 1 Bed' : `${b} Bed`}
            </button>
          ))}
        </div>

        {isLoading && <p className="font-sans text-sm text-muted">Loading templates…</p>}

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-3">
          {data?.content.map((t) => (
            <button
              key={t.id}
              onClick={() => {
                setSelected(t);
                setProjectName(t.name);
              }}
              className="text-left bg-surface border border-stroke rounded-lg overflow-hidden hover:border-accent-primary hover:-translate-y-0.5 transition-all duration-150"
            >
              <div className="w-full aspect-[4/3] bg-canvas" dangerouslySetInnerHTML={{ __html: t.thumbnailSvg }} />
              <div className="p-2.5">
                <div className="font-sans text-[11px] font-medium text-ink truncate">{t.category}</div>
                <div className="font-mono text-[9px] text-muted mt-0.5">
                  {t.suggestedPlotWidthMeters}m × {t.suggestedPlotDepthMeters}m
                </div>
              </div>
            </button>
          ))}
        </div>

        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-center gap-3 mt-6">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="px-3 py-1.5 border border-stroke rounded-md font-sans text-xs text-ink disabled:opacity-40"
            >
              Previous
            </button>
            <span className="font-mono text-xs text-muted">
              Page {page + 1} of {data.totalPages}
            </span>
            <button
              disabled={page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1.5 border border-stroke rounded-md font-sans text-xs text-ink disabled:opacity-40"
            >
              Next
            </button>
          </div>
        )}
      </div>

      {selected && (
        <div
          className="fixed inset-0 bg-ink/40 flex items-center justify-center z-50 p-4"
          onClick={() => setSelected(null)}
        >
          <div
            className="bg-surface rounded-xl border border-stroke w-full max-w-sm p-6 panel-enter"
            onClick={(e) => e.stopPropagation()}
          >
            <div
              className="w-full aspect-[4/3] bg-canvas rounded-md mb-4 border border-stroke overflow-hidden"
              dangerouslySetInnerHTML={{ __html: selected.thumbnailSvg }}
            />
            <h2 className="font-serif text-lg text-ink mb-1">{selected.category}</h2>
            <p className="font-mono text-xs text-muted mb-4">
              {selected.suggestedPlotWidthMeters}m × {selected.suggestedPlotDepthMeters}m plot · Style{' '}
              {selected.style}
            </p>
            <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">
              Project name
            </label>
            <input
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              className="w-full h-10 mb-4 px-3 border border-stroke rounded-md font-sans text-sm bg-canvas outline-none focus:border-accent-primary"
            />
            <div className="flex gap-2">
              <button
                onClick={() => setSelected(null)}
                className="flex-1 h-10 border border-stroke rounded-md font-sans text-sm text-ink hover:bg-canvas transition-colors duration-150"
              >
                Cancel
              </button>
              <button
                onClick={() => useMutationResult.mutate()}
                disabled={useMutationResult.isPending || !projectName.trim()}
                className="flex-1 h-10 bg-accent-primary text-white rounded-md font-sans text-sm font-medium hover:bg-accent-secondary transition-colors duration-200 disabled:opacity-50"
              >
                {useMutationResult.isPending ? 'Creating…' : 'Use this template'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
