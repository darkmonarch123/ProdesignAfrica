import { useState } from 'react';
import { Link } from 'react-router-dom';

const FEATURES = [
  {
    icon: 'ti-vector-triangle',
    title: '2D CAD editor',
    desc: 'Smart snapping, auto-dimensioning, layer manager, and hatch fills for walls, columns, and slabs — all in the browser.',
    tag: 'NIS / ISO standard',
  },
  {
    icon: 'ti-cube',
    title: '2D → 3D in one click',
    desc: 'Extrude walls, generate hip or gable roofs, apply African material textures. Orbit camera with sun path simulation.',
    tag: 'WebGL · No plugin',
  },
  {
    icon: 'ti-shield-check',
    title: 'Compliance engine',
    desc: "Real-time setback, plot coverage, ROW, and height checks based on your state's regulations — flagged as you draw.",
    tag: 'LASDRI · FCTDA · KEBS',
  },
  {
    icon: 'ti-file-invoice',
    title: 'Bill of quantities',
    desc: 'Automatic BOQ with current material rates in NGN, GHS, and KES. Export to Excel or PDF instantly.',
    tag: 'Live market rates',
  },
  {
    icon: 'ti-robot',
    title: 'AI room planner',
    desc: 'Describe your brief — plot size, building type, family size — and get a starter layout you can refine in the editor.',
    tag: 'Claude API powered',
  },
  {
    icon: 'ti-users',
    title: 'Real-time collaboration',
    desc: 'Live multi-cursor editing. Share a view-only link with your client. Comment threads anchored to drawing elements.',
    tag: 'WebSocket · STOMP',
  },
];

const STEPS = [
  { num: '01 — Set up', title: 'Enter your plot', desc: 'Select your country, state, and plot dimensions. Prodesign loads the correct compliance rules automatically.' },
  { num: '02 — Design', title: 'Draw your plan', desc: 'Use 2D tools or let the AI planner generate a layout. Compliance checks run live as you place every element.' },
  { num: '03 — Review', title: 'Go to 3D', desc: 'Extrude your plan into a full 3D model. Apply local material finishes and simulate sunlight for your latitude.' },
  { num: '04 — Export', title: 'Submit & share', desc: 'Export print-ready PDFs with ARCON title block, BOQ to Excel, and glTF 3D models for client presentations.' },
];

const COMPLIANCE_BADGES = [
  { code: 'ARCON', name: 'Nigeria' },
  { code: 'LASDRI', name: 'Lagos state' },
  { code: 'COREN', name: 'Eng. Nigeria' },
  { code: 'FCTDA', name: 'Abuja FCT' },
  { code: 'KEBS', name: 'Kenya' },
  { code: 'NHBRC', name: 'South Africa' },
  { code: 'NBS', name: 'Specifications' },
];

const CITIES = ['Lagos', 'Abuja', 'Accra', 'Nairobi', 'Johannesburg', 'Addis Ababa', 'Dar es Salaam', 'Kampala', 'Dakar'];

export default function LandingPage() {
  const [navOpen, setNavOpen] = useState(false);

  return (
    <div className="min-h-screen bg-canvas">
      {/* NAV */}
      <div className="bg-surface border-b border-stroke h-14 flex items-center px-4 sm:px-8 gap-4 sm:gap-6 relative">
        <div className="font-serif text-xl text-ink">
          Pro<span className="text-accent-primary">design</span> Africa
        </div>
        <div className="hidden md:flex gap-5 ml-auto">
          <a href="#features" className="text-sm text-muted hover:text-ink">Features</a>
          <a href="#compliance" className="text-sm text-muted hover:text-ink">Compliance</a>
          <a href="#pricing" className="text-sm text-muted hover:text-ink">Pricing</a>
          <span className="text-sm text-muted hover:text-ink cursor-pointer">Templates</span>
        </div>
        <Link
          to="/login"
          className="hidden md:flex items-center text-sm text-muted border border-stroke rounded-md px-3 py-1.5 ml-2 hover:text-ink"
        >
          Sign in
        </Link>
        <Link
          to="/signup"
          className="hidden md:flex items-center gap-1.5 h-[34px] px-4 bg-accent-primary text-white rounded-md text-sm font-medium ml-1 hover:bg-accent-secondary transition-colors duration-200"
        >
          <i className="ti ti-pencil text-sm" /> Start free
        </Link>
        <button
          className="md:hidden ml-auto text-ink text-xl"
          onClick={() => setNavOpen((v) => !v)}
          aria-label="Toggle menu"
        >
          <i className={navOpen ? 'ti ti-x' : 'ti ti-menu-2'} />
        </button>
        {navOpen && (
          <div className="absolute top-14 left-0 right-0 bg-surface border-b border-stroke flex flex-col p-4 gap-3 md:hidden z-20">
            <a href="#features" className="text-sm text-muted">Features</a>
            <a href="#compliance" className="text-sm text-muted">Compliance</a>
            <a href="#pricing" className="text-sm text-muted">Pricing</a>
            <Link to="/login" className="text-sm text-ink font-medium">Sign in</Link>
            <Link
              to="/signup"
              className="flex items-center justify-center gap-1.5 h-10 bg-accent-primary text-white rounded-md text-sm font-medium"
            >
              <i className="ti ti-pencil text-sm" /> Start free
            </Link>
          </div>
        )}
      </div>

      {/* HERO */}
      <div className="bg-panel-dark px-4 sm:px-8 lg:px-10 pt-10 sm:pt-16 pb-0 flex flex-col lg:flex-row gap-8 lg:gap-10 items-stretch lg:items-end">
        <div className="flex-1 pb-8 sm:pb-14">
          <div className="font-mono text-[11px] text-accent-secondary tracking-widest mb-3.5 flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-accent-secondary" /> Built for African professionals
          </div>
          <h1 className="font-serif text-3xl sm:text-4xl lg:text-[42px] text-white leading-tight mb-5">
            Design buildings
            <br />
            that <em className="text-accent-secondary not-italic">belong</em>
            <br />
            in Africa
          </h1>
          <p className="font-sans text-sm sm:text-[15px] text-[#9FE1CB] leading-relaxed mb-8 max-w-[420px]">
            Professional CAD tools built around Nigerian plot standards, ARCON compliance, African climate zones,
            and local material costs — from Lagos to Nairobi.
          </p>
          <div className="flex gap-2.5 flex-wrap">
            <Link
              to="/signup"
              className="h-11 px-6 bg-accent-primary text-white rounded-lg text-sm font-medium flex items-center gap-2 hover:bg-accent-secondary transition-colors duration-200"
            >
              <i className="ti ti-vector-triangle text-base" /> Start designing free
            </Link>
            <button className="h-11 px-6 bg-transparent text-white border border-white/30 rounded-lg text-sm flex items-center gap-2 hover:bg-white/5 transition-colors duration-200">
              <i className="ti ti-player-play text-base" /> Watch demo
            </button>
          </div>
          <div className="flex gap-6 sm:gap-7 mt-8 pt-6 border-t border-white/10 flex-wrap">
            <div>
              <div className="font-serif text-2xl text-white">4,200+</div>
              <div className="font-mono text-[10px] text-[#5DCAA5] tracking-wider mt-0.5">Professionals</div>
            </div>
            <div>
              <div className="font-serif text-2xl text-white">18,000+</div>
              <div className="font-mono text-[10px] text-[#5DCAA5] tracking-wider mt-0.5">Projects drawn</div>
            </div>
            <div>
              <div className="font-serif text-2xl text-white">10</div>
              <div className="font-mono text-[10px] text-[#5DCAA5] tracking-wider mt-0.5">African countries</div>
            </div>
          </div>
        </div>

        <div className="w-full lg:w-[300px] flex-shrink-0 bg-white/[0.04] border border-white/10 border-b-0 rounded-t-xl overflow-hidden">
          <div className="px-3.5 py-2.5 border-b border-white/[0.08] flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-danger" />
            <span className="w-2 h-2 rounded-full bg-accent-gold" />
            <span className="w-2 h-2 rounded-full bg-accent-secondary" />
            <span className="font-mono text-[10px] text-[#5DCAA5] ml-1">Lekki Duplex — Ground Floor.pad</span>
          </div>
          <div className="p-3 bg-[#0f1e32]/60">
            <div
              className="rounded-md overflow-hidden p-3"
              style={{
                backgroundImage:
                  'linear-gradient(rgba(16,185,129,.15) 1px,transparent 1px),linear-gradient(90deg,rgba(16,185,129,.15) 1px,transparent 1px)',
                backgroundSize: '16px 16px',
              }}
            >
              <svg width="100%" viewBox="0 0 256 190">
                <rect x="2" y="2" width="252" height="186" fill="none" stroke="#10b981" strokeWidth="1.5" />
                <line x1="90" y1="2" x2="90" y2="188" stroke="#10b981" strokeWidth="1" />
                <line x1="2" y1="100" x2="254" y2="100" stroke="#10b981" strokeWidth="1" />
                <line x1="90" y1="56" x2="254" y2="56" stroke="#10b981" strokeWidth=".7" />
                <rect x="8" y="8" width="76" height="86" fill="rgba(16,185,129,.06)" />
                <text x="46" y="50" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#10b981">Living</text>
                <text x="46" y="61" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="7" fill="#5DCAA5">22 m²</text>
                <rect x="96" y="8" width="76" height="42" fill="rgba(16,185,129,.06)" />
                <text x="134" y="32" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#10b981">Master bed</text>
                <rect x="180" y="8" width="70" height="42" fill="rgba(16,185,129,.06)" />
                <text x="215" y="32" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#10b981">Bed 2</text>
                <rect x="96" y="62" width="76" height="32" fill="rgba(194,105,42,.07)" />
                <text x="134" y="81" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#C2692A">Kitchen</text>
                <rect x="8" y="106" width="76" height="78" fill="rgba(16,185,129,.06)" />
                <text x="46" y="148" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#10b981">Dining</text>
                <rect x="96" y="106" width="76" height="78" fill="rgba(16,185,129,.06)" />
                <text x="134" y="148" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#10b981">Bed 3</text>
                <rect x="180" y="106" width="70" height="78" fill="rgba(194,105,42,.07)" />
                <text x="215" y="148" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#C2692A">WC / Bath</text>
              </svg>
            </div>
            <div className="flex gap-1.5 mt-2 flex-wrap">
              <span className="font-mono text-[9px] text-accent-secondary bg-accent-secondary/10 border border-accent-secondary/30 px-1.5 py-0.5 rounded">
                <i className="ti ti-circle-check text-[11px]" /> LASDRI compliant
              </span>
              <span className="font-mono text-[9px] text-accent-gold bg-accent-gold/10 border border-accent-gold/30 px-1.5 py-0.5 rounded">
                1:100 scale
              </span>
              <span className="font-mono text-[9px] text-[#5DCAA5] px-1.5 py-0.5">3D ready</span>
            </div>
          </div>
        </div>
      </div>

      {/* COMPLIANCE BADGE ROW */}
      <div id="compliance" className="bg-canvas px-4 sm:px-8 py-6 border-t border-b border-stroke flex items-center gap-0 justify-around flex-wrap">
        {COMPLIANCE_BADGES.map((badge, i) => (
          <div key={badge.code} className="flex items-center gap-0">
            <div className="text-center px-3 py-1">
              <div className="font-mono text-[11px] text-accent-primary font-medium">{badge.code}</div>
              <div className="font-sans text-[11px] text-muted mt-0.5">{badge.name}</div>
            </div>
            {i < COMPLIANCE_BADGES.length - 1 && <div className="hidden sm:block w-px h-9 bg-stroke" />}
          </div>
        ))}
      </div>

      {/* FEATURES */}
      <div id="features" className="bg-canvas px-4 sm:px-8 lg:px-10 py-14 sm:py-16">
        <div className="font-mono text-[11px] text-accent-primary tracking-widest text-center mb-2.5">
          Platform features
        </div>
        <h2 className="font-serif text-2xl sm:text-3xl text-ink text-center mb-2">Everything your practice needs</h2>
        <p className="font-sans text-sm text-muted text-center max-w-[460px] mx-auto mb-10 sm:mb-12 leading-relaxed">
          From sketch to stamped drawing — tools built around how African architects and engineers actually work.
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {FEATURES.map((f) => (
            <div key={f.title} className="bg-surface border border-stroke rounded-xl p-6">
              <div className="w-10 h-10 rounded-lg bg-green-bg flex items-center justify-center mb-4">
                <i className={`ti ${f.icon} text-xl text-accent-primary`} />
              </div>
              <div className="font-sans text-sm font-medium text-ink mb-2">{f.title}</div>
              <div className="font-sans text-[13px] text-muted leading-relaxed">{f.desc}</div>
              <div className="inline-block mt-3 font-mono text-[10px] text-accent-primary bg-green-bg px-2 py-1 rounded">
                {f.tag}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* HOW IT WORKS */}
      <div className="bg-panel-dark px-4 sm:px-8 lg:px-10 py-14 sm:py-16">
        <div className="font-mono text-[11px] text-[#5DCAA5] tracking-widest text-center mb-2.5">How it works</div>
        <h2 className="font-serif text-2xl sm:text-3xl text-white text-center mb-10 sm:mb-12">
          From brief to stamped drawing
        </h2>
        <div className="flex flex-col sm:flex-row gap-8 sm:gap-0">
          {STEPS.map((step, i) => (
            <div key={step.num} className="flex-1 px-0 sm:px-5 relative">
              {i < STEPS.length - 1 && (
                <div className="hidden sm:block absolute right-0 top-6 w-px h-16 bg-white/[0.15]" />
              )}
              <div className="font-mono text-[10px] text-accent-secondary tracking-wider mb-3">{step.num}</div>
              <div className="font-serif text-lg text-white mb-2">{step.title}</div>
              <div className="font-sans text-[13px] text-[#9FE1CB] leading-relaxed">{step.desc}</div>
            </div>
          ))}
        </div>
      </div>

      {/* AFRICA MAP STRIP */}
      <div className="bg-panel-dark px-4 sm:px-8 lg:px-10 py-10 flex flex-col sm:flex-row items-center gap-8 sm:gap-10">
        <div className="flex-1">
          <h2 className="font-serif text-2xl sm:text-[28px] text-white mb-3">Built for the African continent</h2>
          <p className="font-sans text-[13px] text-[#9FE1CB] leading-relaxed max-w-[360px]">
            Localised plot standards, climate zones, material databases, and regulatory codes for 10 countries —
            and growing.
          </p>
          <div className="flex flex-wrap gap-2 mt-4">
            {CITIES.map((city) => (
              <span
                key={city}
                className="font-mono text-[10px] text-accent-secondary border border-accent-secondary/30 rounded px-2.5 py-1"
              >
                {city}
              </span>
            ))}
          </div>
        </div>
        <svg width="200" height="200" viewBox="0 0 220 220" className="flex-shrink-0">
          <path
            d="M110 18 L155 35 L175 65 L180 95 L170 120 L175 148 L155 175 L130 195 L110 202 L90 195 L65 175 L45 148 L50 120 L40 95 L45 65 L65 35 Z"
            fill="none"
            stroke="#10b981"
            strokeWidth="1.5"
            opacity=".4"
          />
          <path
            d="M110 18 L155 35 L175 65 L180 95 L170 120 L175 148 L155 175 L130 195 L110 202 L90 195 L65 175 L45 148 L50 120 L40 95 L45 65 L65 35 Z"
            fill="#10b981"
            opacity=".05"
          />
          <circle cx="120" cy="90" r="4" fill="#10b981" />
          <text x="128" y="94" fontFamily="JetBrains Mono,monospace" fontSize="9" fill="#5DCAA5">Lagos</text>
          <circle cx="118" cy="78" r="3" fill="#5DCAA5" opacity=".7" />
          <text x="126" y="82" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#5DCAA5" opacity=".7">Abuja</text>
          <circle cx="112" cy="102" r="3" fill="#5DCAA5" opacity=".6" />
          <text x="120" y="106" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#5DCAA5" opacity=".6">Accra</text>
          <circle cx="130" cy="130" r="3" fill="#5DCAA5" opacity=".6" />
          <text x="138" y="134" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#5DCAA5" opacity=".6">Nairobi</text>
          <circle cx="118" cy="160" r="3" fill="#5DCAA5" opacity=".5" />
          <text x="126" y="164" fontFamily="JetBrains Mono,monospace" fontSize="8" fill="#5DCAA5" opacity=".5">Joburg</text>
          <circle cx="100" cy="70" r="2.5" fill="#5DCAA5" opacity=".5" />
          <circle cx="140" cy="115" r="2.5" fill="#5DCAA5" opacity=".5" />
          <circle cx="85" cy="82" r="2" fill="#5DCAA5" opacity=".4" />
          <text x="110" y="12" textAnchor="middle" fontFamily="JetBrains Mono,monospace" fontSize="9" fill="#5DCAA5" opacity=".5" letterSpacing=".1em">
            10 COUNTRIES
          </text>
        </svg>
      </div>

      {/* PRICING */}
      <div id="pricing" className="bg-canvas px-4 sm:px-8 lg:px-10 py-14 sm:py-16">
        <div className="font-mono text-[11px] text-accent-primary tracking-widest text-center mb-2.5">Pricing</div>
        <h2 className="font-serif text-2xl sm:text-3xl text-ink text-center mb-2">Simple, honest pricing</h2>
        <p className="font-sans text-sm text-muted text-center max-w-[460px] mx-auto mb-10 sm:mb-12 leading-relaxed">
          Pay in your local currency. Cancel any time. No hidden export fees.
        </p>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 max-w-5xl mx-auto">
          <div className="bg-surface border border-stroke rounded-xl p-6">
            <div className="font-mono text-[11px] text-muted tracking-wider mb-2">Free</div>
            <div className="font-serif text-3xl text-ink mb-1">
              ₦0 <span className="font-sans text-[13px] text-muted">forever</span>
            </div>
            <div className="font-sans text-xs text-muted mb-5 pb-5 border-b border-stroke">
              For students and hobbyists getting started.
            </div>
            {['3 active projects', 'PDF export (watermarked)', 'Compliance checker', 'Basic BOQ'].map((f) => (
              <div key={f} className="flex items-start gap-2 mb-2.5 font-sans text-xs text-ink">
                <i className="ti ti-check text-sm text-accent-primary mt-px flex-shrink-0" /> {f}
              </div>
            ))}
            <Link
              to="/signup"
              className="block text-center w-full h-[38px] leading-[38px] mt-5 rounded-md text-sm font-medium border border-stroke bg-canvas text-ink hover:bg-stroke/20 transition-colors duration-200"
            >
              Get started
            </Link>
          </div>

          <div className="relative bg-surface border-[1.5px] border-accent-primary rounded-xl p-6">
            <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-accent-primary text-white font-mono text-[10px] px-3 py-1 rounded-full whitespace-nowrap">
              Most popular
            </div>
            <div className="font-mono text-[11px] text-muted tracking-wider mb-2">Professional</div>
            <div className="font-serif text-3xl text-ink mb-1">
              ₦12,000 <span className="font-sans text-[13px] text-muted">/month</span>
            </div>
            <div className="font-sans text-xs text-muted mb-5 pb-5 border-b border-stroke">
              For practising architects and engineers.
            </div>
            {['Unlimited projects', 'Clean PDF + ARCON stamp', 'Full BOQ + Excel export', '3D model + glTF export', 'AI room planner'].map((f) => (
              <div key={f} className="flex items-start gap-2 mb-2.5 font-sans text-xs text-ink">
                <i className="ti ti-check text-sm text-accent-primary mt-px flex-shrink-0" /> {f}
              </div>
            ))}
            <Link
              to="/signup"
              className="block text-center w-full h-[38px] leading-[38px] mt-5 rounded-md text-sm font-medium bg-accent-primary text-white hover:bg-accent-secondary transition-colors duration-200"
            >
              Start 14-day trial
            </Link>
          </div>

          <div className="bg-surface border border-stroke rounded-xl p-6">
            <div className="font-mono text-[11px] text-muted tracking-wider mb-2">Enterprise</div>
            <div className="font-serif text-3xl text-ink mb-1">
              ₦45,000 <span className="font-sans text-[13px] text-muted">/month</span>
            </div>
            <div className="font-sans text-xs text-muted mb-5 pb-5 border-b border-stroke">
              For firms, contractors, and institutions.
            </div>
            {['Up to 20 team seats', 'White-label exports', 'Contractor marketplace', 'Priority support + SLA', 'Custom compliance rules'].map((f) => (
              <div key={f} className="flex items-start gap-2 mb-2.5 font-sans text-xs text-ink">
                <i className="ti ti-check text-sm text-accent-primary mt-px flex-shrink-0" /> {f}
              </div>
            ))}
            <button className="w-full h-[38px] mt-5 rounded-md text-sm font-medium border border-stroke bg-canvas text-ink hover:bg-stroke/20 transition-colors duration-200">
              Contact sales
            </button>
          </div>
        </div>
      </div>

      {/* FOOTER */}
      <div className="bg-panel-dark border-t border-white/[0.08] px-4 sm:px-8 lg:px-10 py-8 flex flex-col sm:flex-row items-center gap-4 justify-between text-center sm:text-left">
        <div>
          <div className="font-serif text-lg text-white">
            Pro<span className="text-accent-secondary">design</span> Africa
          </div>
          <div className="font-mono text-[10px] text-white/30 mt-1">© 2026 · Made in Lagos, Nigeria</div>
        </div>
        <div className="flex gap-5 flex-wrap justify-center">
          <span className="font-sans text-xs text-white/40 cursor-pointer hover:text-white/70">Privacy</span>
          <span className="font-sans text-xs text-white/40 cursor-pointer hover:text-white/70">Terms</span>
          <span className="font-sans text-xs text-white/40 cursor-pointer hover:text-white/70">API docs</span>
          <span className="font-sans text-xs text-white/40 cursor-pointer hover:text-white/70">Twitter/X</span>
        </div>
      </div>
    </div>
  );
}
