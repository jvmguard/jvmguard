import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import sitemap from '@astrojs/sitemap';

export default defineConfig({
  site: 'https://jvmguard.dev',
  base: '/docs',
  redirects: { '/': '/docs/main/introduction' },
  integrations: [
    sitemap(),
    starlight({
      title: 'jvmguard documentation',
      logo: { src: './src/assets/logo.svg', replacesTitle: true },
      components: {
        Header: './src/components/overrides/Header.astro',
      },
      // English stays at the root (no URL prefix); ko/ja/zh-cn are served under /docs/<locale>/.
      locales: {
        root: { label: 'English', lang: 'en' },
        ko: { label: '한국어' },
        ja: { label: '日本語' },
        'zh-cn': { label: '简体中文' },
      },
      social: [{ label: 'GitHub', icon: 'github', href: 'https://github.com/jvmguard/jvmguard' }],
      sidebar: [
    { label: 'Introduction', slug: 'main/introduction', translations: { ko: '소개', ja: 'はじめに', 'zh-CN': '简介' } },
    { label: 'Architecture', slug: 'main/architecture', translations: { ko: '아키텍처', ja: 'アーキテクチャ', 'zh-CN': '架构' } },
    { label: 'Installing', slug: 'main/installing', translations: { ko: '설치', ja: 'インストール', 'zh-CN': '安装' } },
    { label: 'Monitoring JVMs', slug: 'main/monitoring', translations: { ko: 'JVM 모니터링', ja: 'JVMの監視', 'zh-CN': '监控 JVM' } },
    { label: 'Basic concepts', slug: 'main/concepts', translations: { ko: '기본 개념', ja: '基本概念', 'zh-CN': '基本概念' } },
    { label: 'UI', slug: 'main/ui', translations: { ko: 'UI', ja: 'UI', 'zh-CN': 'UI' } },
    { label: 'Transactions', slug: 'main/transactions', translations: { ko: '트랜잭션', ja: 'トランザクション', 'zh-CN': '事务' } },
    { label: 'Policies', slug: 'main/policies', translations: { ko: '정책', ja: 'ポリシー', 'zh-CN': '策略' } },
    { label: 'Telemetries', slug: 'main/telemetries', translations: { ko: '텔레메트리', ja: 'テレメトリ', 'zh-CN': '遥测' } },
    { label: 'Thresholds', slug: 'main/thresholds', translations: { ko: '임계값', ja: 'しきい値', 'zh-CN': '阈值' } },
    { label: 'Triggers', slug: 'main/triggers', translations: { ko: '트리거', ja: 'トリガー', 'zh-CN': '触发器' } },
    { label: 'MBean browser', slug: 'main/mbean', translations: { ko: 'MBean 브라우저', ja: 'MBeanブラウザー', 'zh-CN': 'MBean 浏览器' } },
    { label: 'REST export API', slug: 'main/rest', translations: { ko: 'REST 보내기 API', ja: 'RESTエクスポートAPI', 'zh-CN': 'REST 导出 API' } },
    { label: 'MCP server', slug: 'main/mcp', translations: { ko: 'MCP 서버', ja: 'MCPサーバー', 'zh-CN': 'MCP 服务器' } },
    { label: 'Profiling in production', slug: 'main/profiling', translations: { ko: '프로덕션 프로파일링', ja: '本番環境でのプロファイリング', 'zh-CN': '生产环境分析' } },
    {
      label: 'Configuration',
      translations: { ko: '구성', ja: '設定', 'zh-CN': '配置' },
      collapsed: false,
      items: [
        { label: 'Server configuration', slug: 'config/server-config', translations: { ko: '서버 구성', ja: 'サーバー設定', 'zh-CN': '服务器配置' } },
        { label: 'Server administration', slug: 'config/admin', translations: { ko: '서버 관리', ja: 'サーバー管理', 'zh-CN': '服务器管理' } },
        { label: 'Single sign-on', slug: 'config/sso', translations: { ko: 'Single Sign-On(SSO)', ja: 'シングルサインオン(SSO)', 'zh-CN': '单点登录(SSO)' } },
        { label: 'Import/Export', slug: 'config/impex', translations: { ko: '가져오기/보내기', ja: 'インポート/エクスポート', 'zh-CN': '导入/导出' } },
        { label: 'Unattended installations', slug: 'config/unattended-installations', translations: { ko: '무인 설치', ja: '無人インストール', 'zh-CN': '无人值守安装' } },
        { label: 'Automatic agent update', slug: 'config/agent-update', translations: { ko: '에이전트 자동 업데이트', ja: 'エージェントの自動更新', 'zh-CN': '代理自动更新' } },
      ],
    },
    {
      label: 'Advanced topics',
      translations: { ko: '고급 주제', ja: '高度なトピック', 'zh-CN': '高级主题' },
      collapsed: false,
      items: [
        { label: 'Declared transactions', slug: 'advanced/declared', translations: { ko: '선언적 트랜잭션', ja: '宣言型トランザクション', 'zh-CN': '声明式事务' } },
        { label: 'Mapped transactions', slug: 'advanced/mapped', translations: { ko: '매핑 트랜잭션', ja: 'マッピングトランザクション', 'zh-CN': '映射事务' } },
        { label: 'Matched transactions', slug: 'advanced/matched', translations: { ko: '매칭 트랜잭션', ja: 'マッチングトランザクション', 'zh-CN': '匹配事务' } },
      ],
    },
  ],
    }),
  ],
});
