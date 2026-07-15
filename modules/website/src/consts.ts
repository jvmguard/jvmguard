// Single source of truth for brand references. Edit this file and swap
// src/assets/logo-inline.svg to rebrand. Nothing else in the site should
// hardcode the name, domain, or repo.

export const SITE = {
  name: 'jvmguard',
  title: 'jvmguard',
  canonical: 'https://jvmguard.dev',

  tagline: 'the watchman over your JVMs.',
  description:
    'jvmguard keeps watch over your production JVMs with near-zero overhead. When something is off, it captures a single deep profile that shows you why.',

  repo: 'https://github.com/jvmguard/jvmguard',
  releases: 'https://github.com/jvmguard/jvmguard/releases',
  changelog: 'https://github.com/jvmguard/jvmguard/blob/main/CHANGELOG.md',

  docs: '/docs/main/introduction',

  docsArchitecture: '/docs/main/architecture',
  docsInstalling: '/docs/main/installing',
  docsTriggers: '/docs/main/triggers',
  docsProfiling: '/docs/main/profiling',

  license: 'Apache License 2.0',
  licenseUrl: 'https://www.apache.org/licenses/LICENSE-2.0',
} as const;

// Brand palette
export const COLOR = {
  brand: '#2258D2',
  brandDeep: '#1A3FA0',
  brandStart: '#4139E7',
  alive: '#6abe1e',
} as const;
