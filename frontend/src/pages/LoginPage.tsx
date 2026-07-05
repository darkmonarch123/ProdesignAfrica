import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login as loginApi } from '../api/auth';
import { useAuthStore } from '../store/authStore';
import AuthShell from '../components/auth/AuthShell';
import { Field } from '../components/auth/Field';

const FEATURES = [
  'ARCON & COREN compliance engine',
  'Nigerian & pan-African plot standards',
  '2D to 3D in one click',
  'BOQ export — NGN, GHS, KES',
];

export default function LoginPage() {
  const navigate = useNavigate();
  const loginSuccess = useAuthStore((s) => s.loginSuccess);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await loginApi(email, password);
      loginSuccess(res);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell
      sidePanel={
        <>
          <div className="font-mono text-[10px] text-[#9FE1CB] tracking-wider mb-6 sm:mb-8 mt-1">
            Professional CAD Platform
          </div>
          <div className="hidden md:flex flex-col gap-4">
            {FEATURES.map((f) => (
              <div key={f} className="flex items-start gap-2.5">
                <span className="w-1.5 h-1.5 rounded-full bg-accent-secondary mt-1.5 flex-shrink-0" />
                <span className="font-sans text-xs text-[#9FE1CB] leading-relaxed">{f}</span>
              </div>
            ))}
          </div>
          <div className="hidden md:block mt-auto pt-8 font-mono text-[9px] text-[#9FE1CB]/40 tracking-wider">
            LASDRI · FCTDA · KEBS · NBS
          </div>
        </>
      }
    >
      <div className="font-mono text-[10px] text-accent-primary tracking-wider mb-2.5">Secure access</div>
      <h1 className="font-serif text-2xl sm:text-[26px] text-ink mb-7 leading-tight">Welcome back</h1>

      {error && <div className="mb-4 text-sm text-danger">{error}</div>}

      <form onSubmit={handleSubmit}>
        <Field
          label="Email address"
          type="email"
          placeholder="architect@studio.ng"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <Field
          label="Password"
          type="password"
          placeholder="••••••••"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <div className="flex justify-end mb-5">
          <span className="font-sans text-xs text-accent-primary cursor-pointer">Forgot password?</span>
        </div>
        <button
          type="submit"
          disabled={loading}
          className="w-full h-[42px] bg-accent-primary text-white rounded-lg font-sans text-[13px] font-medium tracking-wide hover:bg-[#065f46] transition-colors duration-150 disabled:opacity-60"
        >
          {loading ? 'Signing in…' : 'Sign in to workspace'}
        </button>
        <p className="font-sans text-xs text-muted text-center mt-4">
          No account?{' '}
          <Link to="/signup" className="text-accent-primary font-medium">
            Create one free
          </Link>
        </p>

        <div className="mt-6 pt-4 border-t border-stroke flex gap-2 items-center">
          <div className="flex-1 h-px bg-stroke" />
          <span className="font-mono text-[10px] text-muted">or continue with</span>
          <div className="flex-1 h-px bg-stroke" />
        </div>
        <div className="grid grid-cols-2 gap-2.5 mt-4">
          <button
            type="button"
            className="h-[38px] border border-stroke rounded-md bg-canvas font-sans text-xs text-ink flex items-center justify-center gap-1.5 hover:bg-stroke/20 transition-colors duration-150"
          >
            <i className="ti ti-brand-google text-base" /> Google
          </button>
          <button
            type="button"
            className="h-[38px] border border-stroke rounded-md bg-canvas font-sans text-xs text-ink flex items-center justify-center gap-1.5 hover:bg-stroke/20 transition-colors duration-150"
          >
            <i className="ti ti-brand-github text-base" /> Github
          </button>
        </div>
      </form>
    </AuthShell>
  );
}
