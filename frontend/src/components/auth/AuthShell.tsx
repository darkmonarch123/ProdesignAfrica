import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface AuthShellProps {
  sidePanel: ReactNode;
  children: ReactNode;
}

export default function AuthShell({ sidePanel, children }: AuthShellProps) {
  return (
    <div className="min-h-screen bg-canvas flex items-center justify-center p-4 sm:p-6">
      <div className="w-full max-w-4xl flex flex-col md:flex-row min-h-0 md:min-h-[540px] border border-stroke rounded-xl overflow-hidden bg-surface panel-enter">
        <div className="w-full md:w-[220px] flex-shrink-0 bg-panel-dark p-6 sm:p-8 flex flex-col gap-0">
          <Link to="/" className="font-serif text-xl sm:text-2xl text-white leading-tight mb-1.5 block">
            Pro<span className="text-accent-secondary">design</span>
            <br className="hidden md:block" /> Africa
          </Link>
          {sidePanel}
        </div>
        <div className="flex-1 p-6 sm:p-9">{children}</div>
      </div>
    </div>
  );
}
