// Single source of truth for brand references. Edit this file and swap
// src/assets/logo-inline.svg to rebrand. Nothing else in the site should
// hardcode the name, domain, or repo.

export const SITE = {
  name: 'jvmguard',
  title: 'jvmguard',
  canonical: 'https://jvmguard.github.io',

  tagline: 'the watchman over your JVMs.',
  description:
    'jvmguard keeps watch over your production JVMs with near-zero overhead. When something is off, it captures a single deep profile that shows you why.',

  repo: 'https://github.com/jvmguard/jvmguard',
  releases: 'https://github.com/jvmguard/jvmguard/releases',

  docs: '/docs/main/introduction',

  docsArchitecture: '/docs/main/architecture',
  docsInstalling: '/docs/main/installing',
  docsTriggers: '/docs/main/triggers',
  docsProfiling: '/docs/main/profiling',

  license: 'Apache License 2.0',
  licenseUrl: 'https://www.apache.org/licenses/LICENSE-2.0',
} as const;

/*
 * Download links. PLACEHOLDER: all point to the releases page. At publication
 * time, replace each URL with the actual release asset URL from the latest
 * GitHub release (e.g.
 * https://github.com/<org>/<name>/releases/download/v1.0/jvmguard_windows_1_0.exe).
 */
export const DOWNLOADS = {
  windows: 'https://github.com/jvmguard/jvmguard/releases/latest',
  macos: 'https://github.com/jvmguard/jvmguard/releases/latest',
  linuxInstaller: 'https://github.com/jvmguard/jvmguard/releases/latest',
  linuxRpm: 'https://github.com/jvmguard/jvmguard/releases/latest',
  linuxDeb: 'https://github.com/jvmguard/jvmguard/releases/latest',
} as const;

// Brand palette
export const COLOR = {
  brand: '#2258D2',
  brandDeep: '#1A3FA0',
  brandStart: '#4139E7',
  alive: '#6abe1e',
} as const;
