import type { InputHTMLAttributes, SelectHTMLAttributes, ReactNode } from 'react';

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
}

export function Field({ label, ...inputProps }: FieldProps) {
  return (
    <div className="mb-4">
      <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">{label}</label>
      <input
        {...inputProps}
        className="w-full h-[38px] border border-stroke rounded-md px-3 font-sans text-[13px] text-ink bg-canvas outline-none focus:border-accent-primary transition-colors duration-150"
      />
    </div>
  );
}

interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  children: ReactNode;
}

export function SelectField({ label, children, ...selectProps }: SelectFieldProps) {
  return (
    <div className="mb-4">
      <label className="block font-mono text-[10px] text-muted tracking-wider uppercase mb-1.5">{label}</label>
      <select
        {...selectProps}
        className="w-full h-[38px] border border-stroke rounded-md px-3 font-sans text-[13px] text-ink bg-canvas outline-none focus:border-accent-primary transition-colors duration-150"
      >
        {children}
      </select>
    </div>
  );
}
