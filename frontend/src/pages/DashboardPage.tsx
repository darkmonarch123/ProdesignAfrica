import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { createProject, listProjects } from '../api/projects';
import { useAuthStore } from '../store/authStore';
import DashboardSidebar from '../components/dashboard/DashboardSidebar';

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  return parts
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('');
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const [showNewProject, setShowNewProject] = useState(false);
  const [newProjectName, setNewProjectName] = useState('');

  const { data: projects, isLoading } = useQuery({ queryKey: ['projects'], queryFn: listProjects });

  const createMutation = useMutation({
    mutationFn: () => createProject({ name: newProjectName || 'Untitled Project' }),
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      navigate(`/editor/${project.id}`);
    },
  });

  const firstName = user?.fullName?.split(' ')[0] || 'there';
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';

  return (
    <div className="min-h-screen bg-canvas flex flex-col">
      {/* Topbar */}
      <div className="bg-surface border-b border-stroke px-4 sm:px-6 h-[52px] flex items-center justify-between flex-shrink-0">
        <div className="font-serif text-lg text-ink">
          Pro<span className="text-accent-primary">design</span> Africa
        </div>
        <div className="flex items-center gap-2 sm:gap-3">
          <div className="hidden sm:block font-mono text-[10px] text-accent-gold border border-accent-gold rounded px-1.5 py-0.5 tracking-wider">
            FREE
          </div>
          <button
            onClick={() => setShowNewProject(true)}
            className="h-[30px] px-3 border border-stroke rounded-md font-sans text-xs text-ink bg-transparent flex items-center gap-1.5 hover:bg-canvas transition-colors duration-150"
          >
            <i className="ti ti-plus text-sm" /> <span className="hidden sm:inline">New project</span>
          </button>
          <div className="w-8 h-8 rounded-full bg-green-bg flex items-center justify-center font-sans text-[11px] font-medium text-accent-primary">
            {user ? initials(user.fullName) : '?'}
          </div>
        </div>
      </div>

      <div className="flex flex-col md:flex-row flex-1">
        <DashboardSidebar projectCount={projects?.length ?? 0} projectLimit={20} />

        <div className="flex-1 p-4 sm:p-6">
          <div className="font-serif text-xl sm:text-2xl text-ink mb-1">
            {greeting}, {firstName}
          </div>
          <div className="font-sans text-[13px] text-muted mb-5">Free plan · {projects?.length ?? 0} projects</div>

          <div className="grid grid-cols-2 lg:grid-cols-4 gap-2.5 mb-6">
            <div className="bg-surface border border-stroke rounded-lg px-4 py-3.5">
              <div className="font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">Projects</div>
              <div className="font-serif text-xl text-ink">{projects?.length ?? 0}</div>
            </div>
            <div className="bg-surface border border-stroke rounded-lg px-4 py-3.5">
              <div className="font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">Exports</div>
              <div className="font-serif text-xl text-accent-primary">0</div>
            </div>
            <div className="bg-surface border border-stroke rounded-lg px-4 py-3.5">
              <div className="font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">Compliance</div>
              <div className="font-serif text-xl text-accent-primary">—</div>
            </div>
            <div className="bg-surface border border-stroke rounded-lg px-4 py-3.5">
              <div className="font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">BOQ total</div>
              <div className="font-serif text-base text-ink">₦0</div>
            </div>
          </div>

          <div className="font-sans text-[11px] font-medium text-muted tracking-widest uppercase pb-2 border-b border-stroke mb-4">
            Recent projects
          </div>

          {showNewProject && (
            <div className="mb-4 flex flex-col sm:flex-row gap-2">
              <input
                autoFocus
                placeholder="New project name"
                value={newProjectName}
                onChange={(e) => setNewProjectName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && createMutation.mutate()}
                className="flex-1 h-10 px-3 border border-stroke rounded-md font-sans text-sm bg-surface outline-none focus:border-accent-primary transition-colors duration-150"
              />
              <button
                onClick={() => createMutation.mutate()}
                disabled={createMutation.isPending}
                className="h-10 px-4 bg-accent-primary text-white rounded-md font-sans text-sm font-medium hover:bg-accent-secondary transition-colors duration-200 disabled:opacity-60"
              >
                {createMutation.isPending ? 'Creating…' : 'Create'}
              </button>
            </div>
          )}

          {isLoading && <p className="text-muted text-sm">Loading projects…</p>}

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {projects?.map((project) => (
              <button
                key={project.id}
                onClick={() => navigate(`/editor/${project.id}`)}
                className="text-left bg-surface border border-stroke rounded-[10px] overflow-hidden hover:-translate-y-0.5 hover:border-accent-primary transition-all duration-200"
              >
                <div className="h-[90px] bg-panel-dark relative flex items-center justify-center">
                  <svg width="80" height="60" viewBox="0 0 80 60" className="opacity-50">
                    <rect x="10" y="20" width="60" height="35" fill="none" stroke="#10b981" strokeWidth="1" />
                    <rect x="10" y="10" width="28" height="10" fill="none" stroke="#10b981" strokeWidth="1" />
                    <line x1="38" y1="20" x2="38" y2="55" stroke="#10b981" strokeWidth=".5" />
                    <line x1="10" y1="38" x2="70" y2="38" stroke="#10b981" strokeWidth=".5" />
                  </svg>
                  <div className="absolute top-2 right-2 font-mono text-[9px] text-accent-secondary border border-accent-secondary rounded px-1.5 py-0.5 bg-accent-secondary/10">
                    Draft
                  </div>
                </div>
                <div className="px-3.5 py-3">
                  <div className="font-sans text-[13px] font-medium text-ink mb-0.5">{project.name}</div>
                  <div className="font-mono text-[10px] text-muted">
                    {project.location || 'No location'} · Updated {new Date(project.updatedAt).toLocaleDateString()}
                  </div>
                </div>
              </button>
            ))}

            <button
              onClick={() => setShowNewProject(true)}
              className="bg-canvas border border-dashed border-stroke rounded-[10px] flex flex-col items-center justify-center min-h-[158px] hover:border-accent-primary transition-colors duration-200"
            >
              <i className="ti ti-plus text-2xl text-stroke mb-2" />
              <span className="font-sans text-xs text-muted">New project</span>
            </button>
          </div>

          {!isLoading && projects?.length === 0 && (
            <p className="text-muted text-sm mt-2">No projects yet — create your first one above.</p>
          )}
        </div>
      </div>
    </div>
  );
}
