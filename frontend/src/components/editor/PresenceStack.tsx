import type { PresenceUser } from '../../hooks/useCollabSocket';

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  return parts.slice(0, 2).map((p) => p[0]?.toUpperCase()).join('');
}

export default function PresenceStack({ users, connected }: { users: PresenceUser[]; connected: boolean }) {
  if (!connected) {
    return (
      <span className="hidden md:flex items-center gap-1.5 font-mono text-[10px] text-white/40 px-2">
        <span className="w-1.5 h-1.5 rounded-full bg-white/30" /> offline
      </span>
    );
  }

  return (
    <div className="hidden md:flex items-center -space-x-2 px-1" title={`${users.length} active`}>
      {users.slice(0, 5).map((u) => (
        <div
          key={u.userId}
          title={u.fullName}
          className="w-7 h-7 rounded-full border-2 border-panel-dark flex items-center justify-center font-sans text-[10px] font-medium text-white"
          style={{ background: u.color }}
        >
          {initials(u.fullName)}
        </div>
      ))}
      {users.length > 5 && (
        <div className="w-7 h-7 rounded-full border-2 border-panel-dark bg-white/10 flex items-center justify-center font-mono text-[9px] text-white">
          +{users.length - 5}
        </div>
      )}
    </div>
  );
}
