import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import sitemap from '@astrojs/sitemap';

export default defineConfig({
  site: 'https://jvmguard.github.io',
  base: '/docs',
  redirects: { '/': '/docs/main/introduction' },
  integrations: [
    sitemap(),
    starlight({
      title: 'jvmguard 5.0 documentation',
      logo: { src: './src/assets/logo.svg', replacesTitle: true },
      components: {
        Header: './src/components/overrides/Header.astro',
      },
      social: [{ label: 'GitHub', icon: 'github', href: 'https://github.com/jvmguard/jvmguard' }],
      sidebar: [
    { label: 'Introduction', slug: 'main/introduction' },
    { label: 'Architecture', slug: 'main/architecture' },
    { label: 'Installing', slug: 'main/installing' },
    { label: 'Monitoring JVMs', slug: 'main/monitoring' },
    { label: 'Basic concepts', slug: 'main/concepts' },
    { label: 'UI', slug: 'main/ui' },
    { label: 'Transactions', slug: 'main/transactions' },
    { label: 'Policies', slug: 'main/policies' },
    { label: 'Telemetries', slug: 'main/telemetries' },
    { label: 'Thresholds', slug: 'main/thresholds' },
    { label: 'Triggers', slug: 'main/triggers' },
    { label: 'MBean browser', slug: 'main/mbean' },
    { label: 'REST export API', slug: 'main/rest' },
    { label: 'Cross-over to profiling', slug: 'main/profiling' },
    {
      label: 'Configuration',
      collapsed: false,
      items: [
        { label: 'Server configuration', slug: 'config/server-config' },
        { label: 'Server administration', slug: 'config/admin' },
        { label: 'Import/Export', slug: 'config/impex' },
        { label: 'Unattended installations', slug: 'config/unattended-installations' },
        { label: 'Automatic agent update', slug: 'config/agent-update' },
      ],
    },
    {
      label: 'Advanced topics',
      collapsed: false,
      items: [
        { label: 'Annotation transactions', slug: 'advanced/annotated' },
        { label: 'POJO transactions', slug: 'advanced/pojo' },
        { label: 'DevOps transactions', slug: 'advanced/devops' },
        { label: 'Customizing net I/O methods', slug: 'advanced/netio' },
      ],
    },
  ],
    }),
  ],
});
