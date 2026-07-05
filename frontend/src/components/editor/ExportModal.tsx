import { useState, Fragment } from 'react';
import { useQuery } from '@tanstack/react-query';
import { downloadBoqExcel, downloadBoqPdf, getBoqPreview, type Currency } from '../../api/boq';
import { downloadDrawingSheet } from '../../api/drawingSheet';

interface ExportModalProps {
  projectId: string;
  projectName: string;
  onClose: () => void;
}

const CURRENCIES: Currency[] = ['NGN', 'GHS', 'KES', 'ZAR'];
const SCALES = [50, 75, 100, 125, 150, 200];
const PAPER_SIZES: ('A4' | 'A3' | 'A2')[] = ['A4', 'A3', 'A2'];

type Tab = 'boq' | 'drawing';

export default function ExportModal({ projectId, projectName, onClose }: ExportModalProps) {
  const [tab, setTab] = useState<Tab>('boq');

  return (
    <div className="fixed inset-0 bg-ink/40 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <div
        className="bg-surface rounded-xl border border-stroke w-full max-w-lg max-h-[85vh] overflow-y-auto p-6 panel-enter"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center gap-1 mb-4 border-b border-stroke">
          <button
            onClick={() => setTab('boq')}
            className={`px-3 py-2 font-sans text-sm font-medium border-b-2 -mb-px transition-colors duration-150 ${
              tab === 'boq' ? 'border-accent-primary text-accent-primary' : 'border-transparent text-muted'
            }`}
          >
            Bill of Quantities
          </button>
          <button
            onClick={() => setTab('drawing')}
            className={`px-3 py-2 font-sans text-sm font-medium border-b-2 -mb-px transition-colors duration-150 ${
              tab === 'drawing' ? 'border-accent-primary text-accent-primary' : 'border-transparent text-muted'
            }`}
          >
            Drawing Sheet
          </button>
        </div>

        {tab === 'boq' ? (
          <BoqTab projectId={projectId} projectName={projectName} onClose={onClose} />
        ) : (
          <DrawingSheetTab projectId={projectId} projectName={projectName} onClose={onClose} />
        )}
      </div>
    </div>
  );
}

function BoqTab({ projectId, projectName, onClose }: { projectId: string; projectName: string; onClose: () => void }) {
  const [currency, setCurrency] = useState<Currency>('NGN');
  const [downloading, setDownloading] = useState<'xlsx' | 'pdf' | null>(null);

  const { data: boq, isLoading } = useQuery({
    queryKey: ['boq-preview', projectId, currency],
    queryFn: () => getBoqPreview(projectId, currency),
  });

  async function handleDownload(format: 'xlsx' | 'pdf') {
    setDownloading(format);
    try {
      if (format === 'xlsx') await downloadBoqExcel(projectId, currency, projectName);
      else await downloadBoqPdf(projectId, currency, projectName);
    } finally {
      setDownloading(null);
    }
  }

  let currentCategory = '';

  return (
    <div>
      <div className="flex items-center justify-between mb-1">
        <h2 className="font-serif text-xl text-ink">Bill of Quantities</h2>
        <select
          value={currency}
          onChange={(e) => setCurrency(e.target.value as Currency)}
          className="h-8 px-2 border border-stroke rounded-md font-mono text-xs bg-canvas outline-none focus:border-accent-primary"
        >
          {CURRENCIES.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>
      <p className="font-sans text-xs text-muted mb-4">{projectName}</p>

      {isLoading && <p className="font-sans text-sm text-muted">Calculating…</p>}

      {boq && boq.lineItems.length === 0 && (
        <p className="font-sans text-sm text-muted mb-4">
          {boq.notes[boq.notes.length - 1] || 'Nothing to price yet — draw your building first.'}
        </p>
      )}

      {boq && boq.lineItems.length > 0 && (
        <div className="mb-4 border border-stroke rounded-md overflow-hidden">
          <table className="w-full text-xs">
            <thead>
              <tr className="bg-panel-dark text-white">
                <th className="text-left font-sans font-medium px-2.5 py-1.5">Description</th>
                <th className="text-right font-sans font-medium px-2.5 py-1.5">Qty</th>
                <th className="text-left font-sans font-medium px-2.5 py-1.5">Unit</th>
                <th className="text-right font-sans font-medium px-2.5 py-1.5">Amount</th>
              </tr>
            </thead>
            <tbody>
              {boq.lineItems.map((item, i) => {
                const showCategory = item.category !== currentCategory;
                currentCategory = item.category;
                return (
                  <Fragment key={i}>
                    {showCategory && (
                      <tr className="bg-green-bg">
                        <td colSpan={4} className="font-mono text-[10px] text-accent-primary font-medium px-2.5 py-1">
                          {item.category}
                        </td>
                      </tr>
                    )}
                    <tr className="border-t border-stroke">
                      <td className="px-2.5 py-1.5 text-ink">{item.description}</td>
                      <td className="px-2.5 py-1.5 text-right font-mono text-ink">{item.quantity.toFixed(2)}</td>
                      <td className="px-2.5 py-1.5 text-muted">{item.unit}</td>
                      <td className="px-2.5 py-1.5 text-right font-mono text-ink">{item.amount.toLocaleString()}</td>
                    </tr>
                  </Fragment>
                );
              })}
              <tr className="border-t-2 border-ink font-medium">
                <td colSpan={3} className="px-2.5 py-2 text-right text-ink">
                  Total ({boq.currency})
                </td>
                <td className="px-2.5 py-2 text-right font-mono text-ink">{boq.totalAmount.toLocaleString()}</td>
              </tr>
            </tbody>
          </table>
        </div>
      )}

      {boq && boq.notes.length > 0 && (
        <ul className="mb-5 space-y-1">
          {boq.notes.map((note, i) => (
            <li key={i} className="font-sans text-[11px] text-muted leading-relaxed">
              {note}
            </li>
          ))}
        </ul>
      )}

      <div className="flex gap-2">
        <button
          onClick={onClose}
          className="flex-1 h-10 border border-stroke rounded-md font-sans text-sm text-ink hover:bg-canvas transition-colors duration-150"
        >
          Close
        </button>
        <button
          onClick={() => handleDownload('xlsx')}
          disabled={!boq || boq.lineItems.length === 0 || downloading !== null}
          className="flex-1 h-10 border border-accent-primary text-accent-primary rounded-md font-sans text-sm font-medium hover:bg-green-bg transition-colors duration-150 disabled:opacity-50 flex items-center justify-center gap-1.5"
        >
          <i className="ti ti-file-spreadsheet text-base" /> {downloading === 'xlsx' ? 'Downloading…' : 'Excel'}
        </button>
        <button
          onClick={() => handleDownload('pdf')}
          disabled={!boq || boq.lineItems.length === 0 || downloading !== null}
          className="flex-1 h-10 bg-accent-primary text-white rounded-md font-sans text-sm font-medium hover:bg-accent-secondary transition-colors duration-200 disabled:opacity-50 flex items-center justify-center gap-1.5"
        >
          <i className="ti ti-file-type-pdf text-base" /> {downloading === 'pdf' ? 'Downloading…' : 'PDF'}
        </button>
      </div>
    </div>
  );
}

function DrawingSheetTab({
  projectId,
  projectName,
  onClose,
}: {
  projectId: string;
  projectName: string;
  onClose: () => void;
}) {
  const [paperSize, setPaperSize] = useState<'A4' | 'A3' | 'A2'>('A3');
  const [scale, setScale] = useState(100);
  const [sheetTitle, setSheetTitle] = useState('Ground Floor Plan');
  const [downloading, setDownloading] = useState(false);

  async function handleDownload() {
    setDownloading(true);
    try {
      await downloadDrawingSheet(projectId, { paperSize, scale, sheetTitle }, projectName);
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div>
      <h2 className="font-serif text-xl text-ink mb-1">Drawing Sheet</h2>
      <p className="font-sans text-xs text-muted mb-4">{projectName}</p>

      <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">Sheet title</label>
      <input
        value={sheetTitle}
        onChange={(e) => setSheetTitle(e.target.value)}
        className="w-full h-9 mb-3 px-3 border border-stroke rounded-md font-sans text-sm bg-canvas outline-none focus:border-accent-primary"
      />

      <div className="grid grid-cols-2 gap-3 mb-4">
        <div>
          <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">
            Paper size
          </label>
          <select
            value={paperSize}
            onChange={(e) => setPaperSize(e.target.value as 'A4' | 'A3' | 'A2')}
            className="w-full h-9 px-2 border border-stroke rounded-md font-mono text-xs bg-canvas outline-none focus:border-accent-primary"
          >
            {PAPER_SIZES.map((p) => (
              <option key={p} value={p}>
                {p} (landscape)
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">Scale</label>
          <select
            value={scale}
            onChange={(e) => setScale(parseInt(e.target.value, 10))}
            className="w-full h-9 px-2 border border-stroke rounded-md font-mono text-xs bg-canvas outline-none focus:border-accent-primary"
          >
            {SCALES.map((s) => (
              <option key={s} value={s}>
                1:{s}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="mb-5 p-3 bg-canvas border border-stroke rounded-md">
        <p className="font-sans text-[11px] text-muted leading-relaxed">
          Shows overall building width/depth dimensions, a fixed north-up arrow (true orientation isn't tracked
          yet), and a scale bar. If the drawing doesn't fit at your chosen scale, it's automatically loosened to
          fit — the sheet will say so. The title block includes a blank ARCON reg. no. line and a note that this
          hasn't been reviewed by a registered professional.
        </p>
      </div>

      <div className="flex gap-2">
        <button
          onClick={onClose}
          className="flex-1 h-10 border border-stroke rounded-md font-sans text-sm text-ink hover:bg-canvas transition-colors duration-150"
        >
          Close
        </button>
        <button
          onClick={handleDownload}
          disabled={downloading}
          className="flex-1 h-10 bg-accent-primary text-white rounded-md font-sans text-sm font-medium hover:bg-accent-secondary transition-colors duration-200 disabled:opacity-50 flex items-center justify-center gap-1.5"
        >
          <i className="ti ti-file-type-pdf text-base" /> {downloading ? 'Downloading…' : 'Download PDF'}
        </button>
      </div>
    </div>
  );
}
