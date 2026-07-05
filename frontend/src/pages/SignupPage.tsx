import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register as registerApi } from '../api/auth';
import AuthShell from '../components/auth/AuthShell';
import { Field, SelectField } from '../components/auth/Field';

export default function SignupPage() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [country, setCountry] = useState('Nigeria');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await registerApi(email, password, fullName);
      setDone(true);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Could not create your account.');
    } finally {
      setLoading(false);
    }
  }

  const sidePanel = (
    <>
      <div className="font-mono text-[10px] text-[#9FE1CB] tracking-wider mb-5 mt-1">
        Join 4,000+ professionals
      </div>
      <div className="hidden md:block font-sans text-xs text-[#9FE1CB] leading-loose">
        Free plan includes:
        <br />
        — 3 active projects
        <br />
        — PDF export (watermarked)
        <br />
        — Compliance checker
        <br />— Basic BOQ
      </div>
      <div className="hidden md:block mt-5 p-3.5 border border-accent-gold/30 rounded-lg bg-accent-gold/[0.06]">
        <div className="font-mono text-[9px] text-accent-gold tracking-wider mb-1.5">PRO PLAN</div>
        <div className="font-serif text-lg text-accent-gold">
          ₦12,000<span className="text-[11px] font-sans opacity-70">/mo</span>
        </div>
        <div className="font-sans text-[11px] text-[#c8a36b] mt-1">Unlimited projects · ARCON stamp</div>
      </div>
    </>
  );

  if (done) {
    return (
      <AuthShell sidePanel={sidePanel}>
        <div className="h-full flex flex-col items-center justify-center text-center py-12">
          <h1 className="font-serif text-2xl text-ink mb-2">Check your inbox</h1>
          <p className="text-muted mb-6">We sent a verification link to {email}.</p>
          <button onClick={() => navigate('/login')} className="text-accent-primary font-medium text-sm">
            Go to login
          </button>
        </div>
      </AuthShell>
    );
  }

  return (
    <AuthShell sidePanel={sidePanel}>
      <div className="font-mono text-[10px] text-accent-primary tracking-wider mb-2.5">Getting started</div>
      <h1 className="font-serif text-2xl sm:text-[26px] text-ink mb-7 leading-tight">Create account</h1>

      {error && <div className="mb-4 text-sm text-danger">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-0 sm:gap-4">
          <Field
            label="Full name"
            placeholder="Chidi Okeke"
            required
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
          <Field
            label="Phone (+234)"
            type="tel"
            placeholder="+234 801 234 5678"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />
        </div>
        <Field
          label="Email address"
          type="email"
          placeholder="chidi@studiodesign.ng"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-0 sm:gap-4">
          <SelectField label="Country" value={country} onChange={(e) => setCountry(e.target.value)}>
            <option>Nigeria</option>
            <option>Ghana</option>
            <option>Kenya</option>
            <option>South Africa</option>
          </SelectField>
          <Field
            label="Password"
            type="password"
            placeholder="••••••••"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className="w-full h-[42px] bg-accent-primary text-white rounded-lg font-sans text-[13px] font-medium tracking-wide hover:bg-[#065f46] transition-colors duration-150 disabled:opacity-60 mt-2"
        >
          {loading ? 'Creating account…' : 'Create free account'}
        </button>
        <p className="font-sans text-xs text-muted text-center mt-3">
          Already registered?{' '}
          <Link to="/login" className="text-accent-primary font-medium">
            Sign in
          </Link>
        </p>
      </form>
    </AuthShell>
  );
}
