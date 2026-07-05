import { useNavigate } from 'react-router-dom';

const NAV_ITEMS = [
  { icon: 'ti-layout-dashboard', label: 'Dashboard', path: '/dashboard', active: true },
  { icon: 'ti-vector-triangle', label: 'Projects', path: '/dashboard' },
  { icon: 'ti-template', label: 'Templates', path: '/templates' },
];

interface DashboardSidebarProps {
  projectCount: number;
  projectLimit: number;
}

export default function DashboardSidebar({ projectCount, projectLimit }: DashboardSidebarProps) {
  const navigate = useNavigate();
  const pct = projectLimit > 0 ? Math.min(100, Math.round((projectCount / projectLimit) * 100)) : 0;

  return (
    <>
      {/* Mobile: horizontal pill scroller */}
      <div className="md:hidden flex gap-2 overflow-x-auto px-4 py-3 bg-surface border-b border-stroke -mx-0">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.label}
            onClick={() => navigate(item.path)}
            className={`flex-shrink-0 flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium whitespace-nowrap ${
              item.active ? 'bg-green-bg text-accent-primary' : 'text-muted border border-stroke'
            }`}
          >
            <i className={`ti ${item.icon} text-sm`} /> {item.label}
          </button>
        ))}
      </div>

      {/* Desktop: vertical rail */}
      <div className="hidden md:flex md:flex-col w-[190px] flex-shrink-0 bg-surface border-r border-stroke py-5">
        {NAV_ITEMS.map((item) => (
          <div
            key={item.label}
            onClick={() => navigate(item.path)}
            className={`flex items-center gap-2.5 px-5 py-2.5 font-sans text-[13px] cursor-pointer transition-colors duration-150 border-l-2 ${
              item.active
                ? 'text-accent-primary border-accent-primary bg-[#f0faf6]'
                : 'text-muted border-transparent hover:text-ink'
            }`}
          >
            <i className={`ti ${item.icon} text-base`} /> {item.label}
          </div>
        ))}
        <div className="mt-auto px-3 pb-3">
          <div className="flex items-center gap-2.5 px-2 py-2.5 font-sans text-[13px] text-muted cursor-pointer hover:text-ink">
            <i className="ti ti-settings text-base" /> Settings
          </div>
          <div className="mx-2 mt-3 p-2.5 bg-green-bg rounded-lg">
            <div className="font-mono text-[9px] text-accent-primary tracking-wider mb-1">STORAGE</div>
            <div className="h-1 bg-accent-primary/[0.15] rounded-full">
              <div className="h-full bg-accent-primary rounded-full" style={{ width: `${pct}%` }} />
            </div>
            <div className="font-mono text-[9px] text-accent-primary mt-1">
              {projectCount} / {projectLimit} projects
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
