import { create } from 'zustand';

interface AuthUser {
  userId: string;
  email: string;
  fullName: string;
  role: string;
}

interface AuthStore {
  accessToken: string | null;
  refreshToken: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: AuthUser) => void;
  loginSuccess: (payload: AuthUser & { accessToken: string; refreshToken: string }) => void;
  logout: () => void;
}

const STORAGE_KEY = 'prodesign_auth';

function loadPersisted(): { accessToken: string | null; refreshToken: string | null; user: AuthUser | null } {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return { accessToken: null, refreshToken: null, user: null };
    return JSON.parse(raw);
  } catch {
    return { accessToken: null, refreshToken: null, user: null };
  }
}

function persist(state: { accessToken: string | null; refreshToken: string | null; user: AuthUser | null }) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

const persisted = loadPersisted();

export const useAuthStore = create<AuthStore>((set, get) => ({
  accessToken: persisted.accessToken,
  refreshToken: persisted.refreshToken,
  user: persisted.user,
  isAuthenticated: !!persisted.accessToken,

  setTokens: (accessToken, refreshToken) => {
    set({ accessToken, refreshToken, isAuthenticated: true });
    persist({ accessToken, refreshToken, user: get().user });
  },

  setUser: (user) => {
    set({ user });
    persist({ accessToken: get().accessToken, refreshToken: get().refreshToken, user });
  },

  loginSuccess: (payload) => {
    const user = { userId: payload.userId, email: payload.email, fullName: payload.fullName, role: payload.role };
    set({ accessToken: payload.accessToken, refreshToken: payload.refreshToken, user, isAuthenticated: true });
    persist({ accessToken: payload.accessToken, refreshToken: payload.refreshToken, user });
  },

  logout: () => {
    set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false });
    sessionStorage.removeItem(STORAGE_KEY);
  },
}));
