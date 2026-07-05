import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        canvas: 'rgb(var(--color-canvas) / <alpha-value>)',
        surface: 'rgb(var(--color-surface) / <alpha-value>)',
        ink: 'rgb(var(--color-ink) / <alpha-value>)',
        'accent-primary': 'rgb(var(--color-accent-primary) / <alpha-value>)',
        'accent-secondary': 'rgb(var(--color-accent-secondary) / <alpha-value>)',
        'accent-earth': 'rgb(var(--color-accent-earth) / <alpha-value>)',
        'accent-gold': 'rgb(var(--color-accent-gold) / <alpha-value>)',
        stroke: 'rgb(var(--color-stroke) / <alpha-value>)',
        muted: 'rgb(var(--color-muted) / <alpha-value>)',
        danger: 'rgb(var(--color-danger) / <alpha-value>)',
        warn: 'rgb(var(--color-warn) / <alpha-value>)',
        'panel-dark': 'rgb(var(--color-panel-dark) / <alpha-value>)',
        blueprint: 'rgb(var(--color-blueprint) / <alpha-value>)',
        blue: 'rgb(var(--color-blue) / <alpha-value>)',
        'green-bg': 'rgb(var(--color-green-bg) / <alpha-value>)',
      },
      fontFamily: {
        serif: ['"DM Serif Display"', 'serif'],
        sans: ['Inter', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
      },
      transitionTimingFunction: {
        spring: 'var(--ease-spring)',
      },
    },
  },
  plugins: [],
} satisfies Config;
