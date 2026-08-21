// Message catalog for all user-facing website copy. One flat object per locale,
// dotted keys (`area.section.element`). English is the source locale and must be
// byte-identical to the copy that shipped before localization. Translations follow
// modules/docs/agent/i18n-glossary.md and website-voice.md.

export const LOCALES = ['en', 'ko', 'ja', 'zh-cn'] as const;
export type Locale = (typeof LOCALES)[number];
export const DEFAULT_LOCALE: Locale = 'en';

/** Native display names, always rendered in their own language. */
export const LOCALE_NAMES: Record<Locale, string> = {
  en: 'English',
  ko: '한국어',
  ja: '日本語',
  'zh-cn': '简体中文',
};

/** Value for the <html lang> attribute (BCP-47). */
export function htmlLang(locale: Locale): string {
  return locale === 'zh-cn' ? 'zh-CN' : locale;
}

/** Prefix a site path with the locale, keeping the default locale unprefixed. */
export function localeUrl(locale: Locale, path: string): string {
  const prefix = locale === DEFAULT_LOCALE ? '' : `/${locale}`;
  return path === '/' ? prefix || '/' : `${prefix}${path}`;
}

/**
 * Locale-aware URL into the docs site. The docs Starlight site uses the same
 * locale codes: English at /docs/..., the others under /docs/<locale>/....
 */
export function docsUrl(locale: Locale, docsPath: string): string {
  return locale === DEFAULT_LOCALE ? docsPath : docsPath.replace(/^\/docs/, `/docs/${locale}`);
}

export const en = {
  // PageFrame
  'nav.overview': 'Overview',
  'nav.security': 'Security',
  'nav.compare': 'Compare',
  'nav.download': 'Download',
  'nav.docs': 'Docs',
  'a11y.skipToContent': 'Skip to content',
  'a11y.primaryNav': 'Primary',
  'a11y.toggleTheme': 'Toggle color theme',
  'a11y.home': 'jvmguard home',
  'a11y.language': 'Language',
  'footer.note': 'Built by the team behind JProfiler.',

  // Home
  'home.meta.title': 'jvmguard',
  'home.meta.description':
    'jvmguard keeps watch over your production JVMs with near-zero overhead. When something is off, it captures a single deep profile that shows you why.',
  'home.hero.eyebrow': 'Automatic Production Profiling',
  'home.hero.title.1': 'Production profiling,',
  'home.hero.title.2': 'controlled and audited.',
  'home.hero.title.3': 'For humans and agents.',
  'home.hero.lede':
    'Deep profiling captures in production are risky, but sometimes necessary. jvmguard brings them under control: every capture is authorized and audited, whether it is triggered by a person, a monitoring alert, or an agent.',
  'home.hero.cta.download': 'Download',
  'home.hero.cta.docs': 'Read the docs',
  'home.hero.cta.github': 'View on GitHub',
  'home.hero.cred': 'Open source under the Apache License 2.0.',
  'home.shot.alt':
    'The jvmguard web UI showing a fleet of JVMs with live telemetry, and the Record JProfiler snapshot dialog with subsystem, heap dump, and MBean snapshot options.',
  'home.shot.caption':
    'The jvmguard web UI: your whole fleet on one screen. Live telemetry, and every action one click away.',
  'home.features.rule': 'What it does',
  'home.feature.1.title': 'Pure Java agent by default',
  'home.feature.1.body':
    'The agent instruments selected methods through class loading. It loads no native libraries unless you explicitly run a profiling action.',
  'home.feature.2.title': 'Triggers and actions',
  'home.feature.2.body':
    'Set a threshold on a telemetry or a transaction. When it is crossed, jvmguard runs the actions you configured: record JFR, take a heap or thread dump, write to the inbox, send an email, or call a webhook. You compose the response in the UI.',
  'home.feature.3.title': 'Capture on demand',
  'home.feature.3.body':
    'Lightweight telemetries and the transactions you define provide the signals that trigger a deep capture. Steady-state overhead stays close to zero.',
  'home.feature.4.title': 'No JMX port required',
  'home.feature.4.body':
    'The agent reads MBeans in process. You can inspect and operate on MBeans without exposing a JMX connector server to the network.',
  'home.feature.5.title': 'Manage a fleet of JVMs',
  'home.feature.5.body':
    'Organize hundreds of JVMs into hierarchical groups. Configuration is inherited, so a group applies to every JVM under it and new JVMs need no up-front setup.',
  'home.feature.6.title': 'Configurable retention',
  'home.feature.6.body':
    'Recorded data is aggregated to larger intervals as it ages. Transactions can be discarded after a threshold you set.',
  'home.feature.7.title': 'Self-hosted and air-gapped',
  'home.feature.7.body':
    'Install on Windows, macOS, or Linux. All recorded data stays local with no outbound cloud dependency, so the server can run on your own infrastructure, including air-gapped networks.',
  'home.steps.rule': 'Three ways to capture',
  'home.step.1.title': 'Manually',
  'home.step.1.body': 'Start a capture from the UI for any connected JVM. The capture lands in the inbox.',
  'home.step.2.title': 'Triggered',
  'home.step.2.body':
    'A threshold you set on a telemetry or transaction fires a capture automatically: a JFR recording, a heap dump, or a thread dump for the affected JVM.',
  'home.step.3.title': 'Agent-driven',
  'home.step.3.body':
    'An AI agent starts a capture through the MCP server. Scope limits and an audit record of every operation keep that access accountable.',
  'home.open.rule': 'Open captures, open analysis',
  'home.open.p1':
    'Captures are standard files. JFR recordings and HPROF heap dumps open in JProfiler, JDK Mission Control, or Eclipse MAT.',
  'home.open.p2':
    'For the deepest analysis, jvmguard can record a JProfiler snapshot, which breaks down subsystems such as JDBC, JPA, MongoDB, HTTP and gRPC calls, Kafka, and LLM invocations.',
  'home.open.cta': 'How it compares',
  'home.agent.rule': 'Drive it from an AI agent',
  'home.agent.p1':
    'jvmguard includes an MCP server. An AI agent connects to it to discover the JVM fleet, read telemetries, transactions, and MBeans, start a JFR recording or a heap or thread dump, and retrieve the capture. The same operations that the UI exposes are also available to an agent.',
  'home.agent.p2':
    'The direction is a supervised loop: an agent detects a problem, captures the right profile, and recommends a next step, with a person approving each capture. The guardrails that make this production-safe are the rate and overhead limits, and a full audit record of every agent operation.',
  'home.agent.cta': 'How it stays safe in production',

  // Security
  'security.meta.title': 'Security and data flow - jvmguard',
  'security.meta.description':
    'What the jvmguard agent does and does not do in your JVM: no default collection, no phone home, no cloud egress, source-available under Apache 2.0.',
  'security.hero.eyebrow': 'Security and data flow',
  'security.hero.title': 'What jvmguard does inside your JVM',
  'security.hero.lede':
    'This page states what the jvmguard agent collects in your JVM, where it sends that data, and what you can check in the source. For a tool that runs in production, that is the first question to answer.',
  'security.arch.rule': 'Architecture',
  'security.arch.alt':
    'jvmguard architecture: agents in monitored VMs connect to a server that runs the collector, an embedded H2 database, and the web UI.',
  'security.arch.caption':
    'Agents push to your server over an authenticated connection. Users reach the web UI over HTTPS. No component talks to a vendor.',
  'security.collect.rule': 'Collected by default',
  'security.collect.intro': 'jvmguard records only lightweight data, at low steady-state overhead:',
  'security.collect.1': 'Built-in scalar telemetries like heap, CPU, threads or GC',
  'security.collect.2': 'Scalar telemetries from configured sources',
  'security.collect.3': 'Transactions you explicitly configured',
  'security.capture.rule': 'Captured on demand',
  'security.capture.intro':
    'Deep artifacts are not collected by default. A capture runs only when a trigger fires, or when you start one manually on a single JVM.',
  'security.capture.1': 'A JFR recording, for a bounded duration',
  'security.capture.2': 'A heap dump (HPROF)',
  'security.capture.3': 'A thread dump',
  'security.capture.4': 'A JProfiler snapshot',
  'security.posture.rule': 'Operational posture',
  'security.fact.1.k': 'No phone home',
  'security.fact.1.v':
    'Agents connect to your jvmguard server. The server makes no outbound calls. All data stays inside your network.',
  'security.fact.2.k': 'No JMX port',
  'security.fact.2.v':
    'The agent reads MBeans in process. You do not have to open a JMX connector server on the monitored JVM to inspect or operate on MBeans.',
  'security.fact.3.k': 'Single H2 file',
  'security.fact.3.v':
    'Recorded data and configuration both live in a single embedded H2 file. There is no external database to operate.',
  'security.fact.4.k': 'TLS for agents',
  'security.fact.4.v': 'Agent-to-server traffic can be encrypted and authenticated via TLS.',
  'security.fact.5.k': 'Three access levels',
  'security.fact.5.v': 'Admin, Profiler, and Viewer roles partition a single server for separate teams.',
  'security.fact.6.k': 'Local, LDAP, or SSO',
  'security.fact.6.v':
    'Authentication uses locally configured accounts, LDAP, or SSO, all configurable in the UI.',
  'security.license.rule': 'License and deployment',
  'security.license.1.strong': 'Apache License 2.0.',
  'security.license.1.body':
    'Sources are available. Read it, build it, and audit it with your own tooling before it touches production.',
  'security.license.2.strong': 'Self-hosted.',
  'security.license.2.body':
    'Runs behind your firewall. Suitable for air-gapped networks. No outbound cloud dependency.',
  'security.license.3.strong': 'Single server.',
  'security.license.3.body':
    'The collector, the embedded database, and the web UI run in one process. No separate services to secure.',

  // Compare
  'compare.meta.title': 'How jvmguard compares',
  'compare.meta.description': 'How jvmguard compares to APMs, continuous profilers and JFR and jcmd.',
  'compare.hero.eyebrow': 'Comparison',
  'compare.hero.title': 'How jvmguard compares',
  'compare.hero.lede':
    'jvmguard captures deep JVM profiles only when something warrants it, and only with authorization and an audit trail. Here is how that differs from the tools you may already run.',
  'compare.adjacent.rule': 'Adjacent approaches',
  'compare.cmp.1.name': 'APM suites',
  'compare.cmp.1.base': 'Datadog, Dynatrace, New Relic, and similar platforms',
  'compare.cmp.1.d1':
    'APMs instrument your frameworks and measure spans continuously. jvmguard records only the transactions you define.',
  'compare.cmp.1.d2':
    'APMs gather application and end-user data and send it to a cloud backend. jvmguard stays inside your network.',
  'compare.cmp.1.d3':
    'jvmguard is a complement, not a replacement. APMs give you breadth and alerting, jvmguard gives you deep, targeted captures.',
  'compare.cmp.2.name': 'Continuous profilers',
  'compare.cmp.2.base': 'Grafana Pyroscope, Parca and Polar Signals, Elastic Universal Profiling',
  'compare.cmp.2.d1':
    'Continuous profilers sample broadly and never stop. jvmguard captures a deep artifact, a heap dump or a JFR recording, from the one JVM that needs it, only when warranted.',
  'compare.cmp.2.d2':
    'A continuous profiler pays a small constant overhead on every JVM, always. jvmguard pays that cost only during a capture.',
  'compare.cmp.3.name': 'Plain JFR and jcmd',
  'compare.cmp.3.base':
    'Built into the JDK. JFR recordings, jcmd operations, and JDK Mission Control for analysis.',
  'compare.cmp.3.d1':
    'Without jvmguard, getting a heap dump means SSH access to the production host, running jcmd by hand, and transferring the file. In most organizations, developers do not have that access.',
  'compare.cmp.3.d2':
    'jvmguard replaces the SSH workflow with authorized access. A profiler-role user starts a capture from the UI or through a coding agent, and the audit log records who ran it, when, and against which JVM.',
  'compare.cmp.3.d3':
    'jcmd captures whatever you run at that moment. jvmguard can also fire the capture automatically the instant a threshold is crossed, with no one watching.',
  'compare.cmp.3.d4':
    'One trigger fires across all matching JVMs in the fleet, not one-by-one over individual JVMs.',
  'compare.isnot.rule': 'What jvmguard is not',
  'compare.isnot.p':
    'jvmguard is not an APM. There is no continuous tracing, no distributed trace export, and no metrics pipeline, and it does not auto-instrument your frameworks. It records only the lightweight telemetries and the transactions you define, so the always-on overhead stays minimal. Deep captures are intentional and scoped, and they stay on your infrastructure. No user data is exported to a remote backend.',
  'compare.fits.rule': 'Where jvmguard fits',
  'compare.fits.1':
    'A developer gets a deep capture from a production JVM through authorized, audited access, without an SSH session on the host.',
  'compare.fits.2':
    'An AI agent operates on production JVMs within scope and rate limits, and every operation is written to a SIEM-compatible audit log.',
  'compare.fits.3':
    'Lightweight always-on monitoring triggers a capture automatically when a threshold is crossed.',

  // Download
  'download.meta.title': 'Download - jvmguard',
  'download.meta.description':
    'Download jvmguard for Windows, macOS, or Linux, or run the current development version from source.',
  'download.hero.eyebrow': 'Download',
  'download.hero.title': 'Get jvmguard',
  'download.hero.lede':
    'Install jvmguard as a single self-contained server with a bundled Java runtime. You can also run it from source to track the current development version.',
  'download.hero.currentVersion': 'Current version:',
  'download.hero.changelog': 'Changelog',
  'download.installer.rule': 'Installer',
  'download.platform.linux.file': 'Installer or archives',
  'download.btn.download': 'Download',
  'download.btn.installerSh': 'Installer (.sh)',
  'download.btn.tar': 'Tar archive',
  'download.btn.rpm': 'RPM',
  'download.btn.deb': 'DEB',
  'download.installer.pending': 'No release available yet.',
  'download.installer.note.1': 'After installing, open ',
  'download.installer.note.2':
    ' and complete the setup wizard. Then point a JVM at the agent port, default ',
  'download.installer.note.3': ', to start monitoring. See the ',
  'download.installer.note.link': 'installation guide',
  'download.installer.note.4': ' for details.',
  'download.source.rule': 'Run from source',
  'download.source.p1':
    'To run the current development version without building an installer, clone the repository and launch the server. The build auto-provisions a Java 25 toolchain, so no separate Java setup is needed.',
  'download.source.note.1': 'Once you see ',
  'download.source.note.2': ', open ',
  'download.source.note.3': '. The default web port is ',
  'download.source.note.4': ' and the agent port is ',
  'download.source.note.5': '.',
} as const;

export type MessageKey = keyof typeof en;
export type Messages = Record<MessageKey, string>;

export const ko: Messages = {
  // PageFrame
  'nav.overview': '개요',
  'nav.security': '보안',
  'nav.compare': '비교',
  'nav.download': '다운로드',
  'nav.docs': '문서',
  'a11y.skipToContent': '본문으로 건너뛰기',
  'a11y.primaryNav': '주 메뉴',
  'a11y.toggleTheme': '색상 테마 전환',
  'a11y.home': 'jvmguard 홈',
  'a11y.language': '언어',
  'footer.note': 'JProfiler을 만든 팀이 개발했습니다.',

  // Home
  'home.meta.title': 'jvmguard',
  'home.meta.description':
    'jvmguard는 거의 0에 가까운 오버헤드로 프로덕션 JVM을 감시합니다. 문제가 발생하면 그 이유를 보여주는 심층 프로파일 하나를 캡처합니다.',
  'home.hero.eyebrow': '자동 프로덕션 프로파일링',
  'home.hero.title.1': '프로덕션 프로파일링.',
  'home.hero.title.2': '통제되고 감사됩니다.',
  'home.hero.title.3': '사람과 에이전트를 위해.',
  'home.hero.lede':
    '프로덕션에서의 심층 프로파일링 캡처는 위험하지만 때로는 필요합니다. jvmguard는 이를 통제하에 둡니다. 사람, 모니터링 경보, 에이전트 중 무엇이 트리거했든 모든 캡처는 승인되고 감사됩니다.',
  'home.hero.cta.download': '다운로드',
  'home.hero.cta.docs': '문서 읽기',
  'home.hero.cta.github': 'GitHub에서 보기',
  'home.hero.cred': 'Apache License 2.0 아래의 오픈 소스입니다.',
  'home.shot.alt':
    '실시간 텔레메트리와 함께 JVM 플릿을 보여주는 jvmguard 웹 UI와, 서브시스템, 힙 덤프, MBean 스냅샷 옵션이 있는 JProfiler 스냅샷 기록 대화 상자.',
  'home.shot.caption':
    'jvmguard 웹 UI: 전체 플릿을 한 화면에. 실시간 텔레메트리와 모든 작업이 클릭 한 번 거리에 있습니다.',
  'home.features.rule': '무엇을 하는지',
  'home.feature.1.title': '기본적으로 순수 Java 에이전트',
  'home.feature.1.body':
    '에이전트는 클래스 로딩을 통해 선택한 메서드를 계측합니다. 프로파일링 작업을 명시적으로 실행하지 않는 한 네이티브 라이브러리를 로드하지 않습니다.',
  'home.feature.2.title': '트리거와 작업',
  'home.feature.2.body':
    '텔레메트리나 트랜잭션에 임계값을 설정하세요. 임계값을 넘으면 jvmguard가 구성한 작업을 실행합니다: JFR 기록, 힙 또는 스레드 덤프, 수신함에 기록, 이메일 전송, 웹훅 호출. 응답은 UI에서 구성합니다.',
  'home.feature.3.title': '필요할 때 캡처',
  'home.feature.3.body':
    '가벼운 텔레메트리와 정의한 트랜잭션이 심층 캡처를 트리거하는 신호를 제공합니다. 정상 상태의 오버헤드는 0에 가깝게 유지됩니다.',
  'home.feature.4.title': 'JMX 포트 불필요',
  'home.feature.4.body':
    '에이전트는 프로세스 내에서 MBean을 읽습니다. JMX 커넥터 서버를 네트워크에 노출하지 않고도 MBean을 검사하고 조작할 수 있습니다.',
  'home.feature.5.title': 'JVM 플릿 관리',
  'home.feature.5.body':
    '수백 개의 JVM을 계층적 그룹으로 조직하세요. 설정은 상속되므로 그룹은 그 아래의 모든 JVM에 적용되고 새 JVM은 사전 설정이 필요 없습니다.',
  'home.feature.6.title': '구성 가능한 데이터 보관',
  'home.feature.6.body':
    '기록된 데이터는 오래될수록 더 큰 간격으로 집계됩니다. 트랜잭션은 설정한 임계값 이후에 폐기할 수 있습니다.',
  'home.feature.7.title': '자체 호스팅 및 에어갭 지원',
  'home.feature.7.body':
    'Windows, macOS, Linux에 설치하세요. 모든 기록된 데이터는 로컬에 남고 아웃바운드 클라우드 의존성이 없으므로 에어갭 네트워크를 포함한 자체 인프라에서 서버를 실행할 수 있습니다.',
  'home.steps.rule': '세 가지 캡처 방법',
  'home.step.1.title': '수동으로',
  'home.step.1.body': 'UI에서 연결된 JVM의 캡처를 시작하세요. 캡처는 수신함에 도착합니다.',
  'home.step.2.title': '트리거',
  'home.step.2.body':
    '텔레메트리나 트랜잭션에 설정한 임계값이 캡처를 자동으로 발생시킵니다: 해당 JVM의 JFR 기록, 힙 덤프 또는 스레드 덤프.',
  'home.step.3.title': '에이전트 주도',
  'home.step.3.body':
    'AI 에이전트가 MCP 서버를 통해 캡처를 시작합니다. 범위 제한과 모든 작업의 감사 기록이 그 접근에 책임을 부여합니다.',
  'home.open.rule': '열린 캡처, 열린 분석',
  'home.open.p1':
    '캡처는 표준 파일입니다. JFR 기록과 HPROF 힙 덤프는 JProfiler, JDK Mission Control 또는 Eclipse MAT에서 열립니다.',
  'home.open.p2':
    '가장 심층적인 분석을 위해 jvmguard는 JProfiler 스냅샷을 기록할 수 있으며, 이는 JDBC, JPA, MongoDB, HTTP 및 gRPC 호출, Kafka, LLM 호출 같은 서브시스템을 분석합니다.',
  'home.open.cta': '다른 도구와의 비교',
  'home.agent.rule': 'AI 에이전트로 구동',
  'home.agent.p1':
    'jvmguard에는 MCP 서버가 포함되어 있습니다. AI 에이전트가 여기에 연결하여 JVM 플릿을 검색하고, 텔레메트리, 트랜잭션, MBean을 읽고, JFR 기록이나 힙/스레드 덤프를 시작하고, 캡처를 가져옵니다. UI가 제공하는 것과 동일한 작업을 에이전트도 사용할 수 있습니다.',
  'home.agent.p2':
    '지향점은 감독된 루프입니다. 에이전트가 문제를 감지하고, 적절한 프로파일을 캡처하고, 다음 단계를 권장하며, 사람이 각 캡처를 승인합니다. 이를 프로덕션에서 안전하게 만드는 가드레일은 속도 및 오버헤드 제한과 모든 에이전트 작업의 완전한 감사 기록입니다.',
  'home.agent.cta': '프로덕션에서 안전을 유지하는 방법',

  // Security
  'security.meta.title': '보안 및 데이터 흐름 - jvmguard',
  'security.meta.description':
    'jvmguard 에이전트가 JVM에서 무엇을 하고 하지 않는지: 기본 수집 없음, 폰 홈 없음, 클라우드 송출 없음, Apache 2.0으로 소스 공개.',
  'security.hero.eyebrow': '보안 및 데이터 흐름',
  'security.hero.title': 'jvmguard가 JVM 내부에서 하는 일',
  'security.hero.lede':
    '이 페이지는 jvmguard 에이전트가 JVM에서 무엇을 수집하는지, 그 데이터를 어디로 보내는지, 소스에서 무엇을 확인할 수 있는지를 설명합니다. 프로덕션에서 실행되는 도구라면 가장 먼저 답해야 할 질문입니다.',
  'security.arch.rule': '아키텍처',
  'security.arch.alt':
    'jvmguard 아키텍처: 모니터링되는 VM의 에이전트가 수집기, 임베디드 H2 데이터베이스, 웹 UI를 실행하는 서버에 연결합니다.',
  'security.arch.caption':
    '에이전트는 인증된 연결을 통해 서버로 푸시합니다. 사용자는 HTTPS를 통해 웹 UI에 접근합니다. 어떤 구성 요소도 벤더와 통신하지 않습니다.',
  'security.collect.rule': '기본적으로 수집되는 항목',
  'security.collect.intro': 'jvmguard는 낮은 정상 상태 오버헤드로 가벼운 데이터만 기록합니다:',
  'security.collect.1': '힙, CPU, 스레드, GC 같은 내장 스칼라 텔레메트리',
  'security.collect.2': '구성된 소스의 스칼라 텔레메트리',
  'security.collect.3': '명시적으로 구성한 트랜잭션',
  'security.capture.rule': '필요 시 캡처되는 항목',
  'security.capture.intro':
    '심층 아티팩트는 기본적으로 수집되지 않습니다. 캡처는 트리거가 발생하거나 단일 JVM에서 수동으로 시작할 때만 실행됩니다.',
  'security.capture.1': '제한된 기간의 JFR 기록',
  'security.capture.2': '힙 덤프(HPROF)',
  'security.capture.3': '스레드 덤프',
  'security.capture.4': 'JProfiler 스냅샷',
  'security.posture.rule': '운영 자세',
  'security.fact.1.k': '폰 홈 없음',
  'security.fact.1.v':
    '에이전트는 사용자의 jvmguard 서버에 연결합니다. 서버는 아웃바운드 호출을 하지 않습니다. 모든 데이터는 네트워크 내부에 남습니다.',
  'security.fact.2.k': 'JMX 포트 없음',
  'security.fact.2.v':
    '에이전트는 프로세스 내에서 MBean을 읽습니다. MBean을 검사하거나 조작하기 위해 모니터링되는 JVM에서 JMX 커넥터 서버를 열 필요가 없습니다.',
  'security.fact.3.k': '단일 H2 파일',
  'security.fact.3.v':
    '기록된 데이터와 설정은 모두 단일 임베디드 H2 파일에 있습니다. 운영할 외부 데이터베이스가 없습니다.',
  'security.fact.4.k': '에이전트용 TLS',
  'security.fact.4.v': '에이전트-서버 간 트래픽은 TLS로 암호화하고 인증할 수 있습니다.',
  'security.fact.5.k': '세 가지 액세스 수준',
  'security.fact.5.v': '관리자, 프로파일러, 뷰어 역할이 단일 서버를 분리된 팀을 위해 분할합니다.',
  'security.fact.6.k': '로컬, LDAP 또는 SSO',
  'security.fact.6.v':
    '인증은 로컬로 구성된 계정, LDAP 또는 SSO를 사용하며, 모두 UI에서 구성할 수 있습니다.',
  'security.license.rule': '라이선스 및 배포',
  'security.license.1.strong': 'Apache License 2.0.',
  'security.license.1.body':
    '소스가 공개되어 있습니다. 프로덕션에 적용하기 전에 직접 읽고, 빌드하고, 자체 도구로 감사하세요.',
  'security.license.2.strong': '자체 호스팅.',
  'security.license.2.body':
    '방화벽 뒤에서 실행됩니다. 에어갭 네트워크에 적합합니다. 아웃바운드 클라우드 의존성이 없습니다.',
  'security.license.3.strong': '단일 서버.',
  'security.license.3.body':
    '수집기, 임베디드 데이터베이스, 웹 UI가 하나의 프로세스에서 실행됩니다. 별도로 보호할 서비스가 없습니다.',

  // Compare
  'compare.meta.title': 'jvmguard 비교',
  'compare.meta.description': 'jvmguard가 APM, 지속적 프로파일러, JFR 및 jcmd와 어떻게 다른지.',
  'compare.hero.eyebrow': '비교',
  'compare.hero.title': 'jvmguard 비교',
  'compare.hero.lede':
    'jvmguard는 무언가 필요할 때만, 그리고 승인과 감사 추적이 있을 때만 심층 JVM 프로파일을 캡처합니다. 이미 사용 중일 수 있는 도구들과 어떻게 다른지 살펴보세요.',
  'compare.adjacent.rule': '인접 접근 방식',
  'compare.cmp.1.name': 'APM 제품군',
  'compare.cmp.1.base': 'Datadog, Dynatrace, New Relic 및 유사한 플랫폼',
  'compare.cmp.1.d1':
    'APM은 프레임워크를 계측하고 스팬을 지속적으로 측정합니다. jvmguard는 정의한 트랜잭션만 기록합니다.',
  'compare.cmp.1.d2':
    'APM은 애플리케이션 및 최종 사용자 데이터를 수집하여 클라우드 백엔드로 보냅니다. jvmguard는 네트워크 내부에 머뭅니다.',
  'compare.cmp.1.d3':
    'jvmguard는 대체품이 아니라 보완재입니다. APM은 범위와 경보를 제공하고, jvmguard는 심층적이고 대상이 지정된 캡처를 제공합니다.',
  'compare.cmp.2.name': '지속적 프로파일러',
  'compare.cmp.2.base': 'Grafana Pyroscope, Parca와 Polar Signals, Elastic Universal Profiling',
  'compare.cmp.2.d1':
    '지속적 프로파일러는 광범위하게 샘플링하고 멈추지 않습니다. jvmguard는 필요할 때만, 필요한 바로 그 JVM에서 힙 덤프나 JFR 기록 같은 심층 아티팩트를 캡처합니다.',
  'compare.cmp.2.d2':
    '지속적 프로파일러는 항상 모든 JVM에서 작은 일정한 오버헤드를 지불합니다. jvmguard는 캡처 중에만 그 비용을 지불합니다.',
  'compare.cmp.3.name': '순수 JFR과 jcmd',
  'compare.cmp.3.base': 'JDK에 내장. 분석에는 JFR 기록, jcmd 작업, JDK Mission Control.',
  'compare.cmp.3.d1':
    'jvmguard 없이 힙 덤프를 얻으려면 프로덕션 호스트에 SSH로 접속하고, jcmd를 직접 실행하고, 파일을 전송해야 합니다. 대부분의 조직에서 개발자는 그런 접근 권한이 없습니다.',
  'compare.cmp.3.d2':
    'jvmguard는 SSH 워크플로를 승인된 접근으로 대체합니다. 프로파일러 역할의 사용자가 UI나 코딩 에이전트를 통해 캡처를 시작하고, 감사 로그는 누가, 언제, 어떤 JVM에 대해 실행했는지 기록합니다.',
  'compare.cmp.3.d3':
    'jcmd는 그 순간에 실행한 것을 캡처합니다. jvmguard는 아무도 보고 있지 않아도 임계값이 넘는 즉시 캡처를 자동으로 발생시킬 수도 있습니다.',
  'compare.cmp.3.d4':
    '하나의 트리거가 개별 JVM을 하나씩 처리하는 대신 플릿의 일치하는 모든 JVM에서 발생합니다.',
  'compare.isnot.rule': 'jvmguard가 아닌 것',
  'compare.isnot.p':
    'jvmguard는 APM이 아닙니다. 지속적 트레이싱도, 분산 트레이스보내기도, 메트릭 파이프라인도 없으며 프레임워크를 자동 계측하지 않습니다. 가벼운 텔레메트리와 정의한 트랜잭션만 기록하므로 상시 오버헤드는 최소로 유지됩니다. 심층 캡처는 의도적이고 범위가 지정되며 인프라에 머뭅니다. 어떤 사용자 데이터도 원격 백엔드로보내지 않습니다.',
  'compare.fits.rule': 'jvmguard가 맞는 경우',
  'compare.fits.1':
    '개발자가 호스트에 SSH 세션 없이 승인되고 감사된 접근을 통해 프로덕션 JVM에서 심층 캡처를 얻습니다.',
  'compare.fits.2':
    'AI 에이전트가 범위 및 속도 제한 내에서 프로덕션 JVM을 조작하고, 모든 작업은 SIEM 호환 감사 로그에 기록됩니다.',
  'compare.fits.3': '가벼운 상시 모니터링이 임계값을 넘으면 자동으로 캡처를 트리거합니다.',

  // Download
  'download.meta.title': '다운로드 - jvmguard',
  'download.meta.description':
    'Windows, macOS, Linux용 jvmguard를 다운로드하거나 소스에서 현재 개발 버전을 실행하세요.',
  'download.hero.eyebrow': '다운로드',
  'download.hero.title': 'jvmguard 받기',
  'download.hero.lede':
    'jvmguard를 번들된 Java 런타임이 포함된 단일 자체 완결 서버로 설치하세요. 소스에서 실행하여 현재 개발 버전을 추적할 수도 있습니다.',
  'download.hero.currentVersion': '현재 버전:',
  'download.hero.changelog': '변경 로그',
  'download.installer.rule': '인스톨러',
  'download.platform.linux.file': '인스톨러 또는 아카이브',
  'download.btn.download': '다운로드',
  'download.btn.installerSh': '인스톨러 (.sh)',
  'download.btn.tar': 'Tar 아카이브',
  'download.btn.rpm': 'RPM',
  'download.btn.deb': 'DEB',
  'download.installer.pending': '아직 사용 가능한 릴리스가 없습니다.',
  'download.installer.note.1': '설치 후 ',
  'download.installer.note.2': '을(를) 열어 설정 마법사를 완료하세요. 그런 다음 JVM이 에이전트 포트(기본값 ',
  'download.installer.note.3': ')를 가리키도록 하여 모니터링을 시작하세요. 자세한 내용은 ',
  'download.installer.note.link': '설치 가이드',
  'download.installer.note.4': '를 참조하세요.',
  'download.source.rule': '소스에서 실행',
  'download.source.p1':
    '인스톨러를 빌드하지 않고 현재 개발 버전을 실행하려면 리포지토리를 클론하고 서버를 시작하세요. 빌드가 Java 25 툴체인을 자동으로 프로비저닝하므로 별도의 Java 설정이 필요 없습니다.',
  'download.source.note.1': '',
  'download.source.note.2': '가 표시되면 ',
  'download.source.note.3': '을(를) 여세요. 기본 웹 포트는 ',
  'download.source.note.4': '이고 에이전트 포트는 ',
  'download.source.note.5': '입니다.',
};

export const ja: Messages = {
  // PageFrame
  'nav.overview': '概要',
  'nav.security': 'セキュリティ',
  'nav.compare': '比較',
  'nav.download': 'ダウンロード',
  'nav.docs': 'ドキュメント',
  'a11y.skipToContent': 'コンテンツへスキップ',
  'a11y.primaryNav': 'メインナビゲーション',
  'a11y.toggleTheme': 'カラーテーマの切り替え',
  'a11y.home': 'jvmguard ホーム',
  'a11y.language': '言語',
  'footer.note': 'JProfilerのチームが開発しました。',

  // Home
  'home.meta.title': 'jvmguard',
  'home.meta.description':
    'jvmguardはほぼゼロのオーバーヘッドでプロダクションJVMを監視します。問題が発生すると、その理由を示す1つのディーププロファイルをキャプチャします。',
  'home.hero.eyebrow': '自動プロダクションプロファイリング',
  'home.hero.title.1': 'プロダクションプロファイリング。',
  'home.hero.title.2': '制御され、監査されます。',
  'home.hero.title.3': '人間にもエージェントにも。',
  'home.hero.lede':
    'プロダクションでのディーププロファイリングのキャプチャはリスクがありますが、必要な場合もあります。jvmguardはこれを管理下に置きます。人、監視アラート、エージェントのいずれによってトリガーされた場合でも、すべてのキャプチャは承認され、監査されます。',
  'home.hero.cta.download': 'ダウンロード',
  'home.hero.cta.docs': 'ドキュメントを読む',
  'home.hero.cta.github': 'GitHubで見る',
  'home.hero.cred': 'Apache License 2.0のオープンソースです。',
  'home.shot.alt':
    'ライブテレメトリとともにJVMフリートを表示するjvmguard Web UIと、サブシステム、ヒープダンプ、MBeanスナップショットのオプションを備えたJProfilerスナップショット記録ダイアログ。',
  'home.shot.caption':
    'jvmguard Web UI: フリート全体を1つの画面に。ライブテレメトリとすべての操作がワンクリックで利用できます。',
  'home.features.rule': '機能',
  'home.feature.1.title': 'デフォルトでは純粋なJavaエージェント',
  'home.feature.1.body':
    'エージェントはクラスロードを通じて選択したメソッドを計装します。プロファイリング操作を明示的に実行しない限り、ネイティブライブラリはロードされません。',
  'home.feature.2.title': 'トリガーとアクション',
  'home.feature.2.body':
    'テレメトリまたはトランザクションにしきい値を設定します。しきい値を超えると、jvmguardは設定したアクションを実行します: JFRの記録、ヒープまたはスレッドダンプの取得、受信トレイへの書き込み、メール送信、Webhookの呼び出し。応答はUIで構成します。',
  'home.feature.3.title': 'オンデマンドでキャプチャ',
  'home.feature.3.body':
    '軽量なテレメトリと定義したトランザクションが、ディープキャプチャをトリガーするシグナルを提供します。定常状態のオーバーヘッドはほぼゼロに保たれます。',
  'home.feature.4.title': 'JMXポートは不要',
  'home.feature.4.body':
    'エージェントはプロセス内でMBeanを読み取ります。JMXコネクタサーバーをネットワークに公開せずに、MBeanを検査して操作できます。',
  'home.feature.5.title': 'JVMフリートの管理',
  'home.feature.5.body':
    '数百のJVMを階層グループに編成します。設定は継承されるため、グループはその配下のすべてのJVMに適用され、新しいJVMに事前のセットアップは必要ありません。',
  'home.feature.6.title': '設定可能なデータ保持',
  'home.feature.6.body':
    '記録されたデータは古くなるとより大きな間隔に集約されます。トランザクションは設定したしきい値の後に破棄できます。',
  'home.feature.7.title': 'セルフホストでエアギャップ対応',
  'home.feature.7.body':
    'Windows、macOS、Linuxにインストールできます。記録されたすべてのデータはローカルに保持され、外部クラウドへの依存がないため、エアギャップネットワークを含む自社インフラでサーバーを実行できます。',
  'home.steps.rule': '3つのキャプチャ方法',
  'home.step.1.title': '手動',
  'home.step.1.body': '接続されたJVMのキャプチャをUIから開始します。キャプチャは受信トレイに届きます。',
  'home.step.2.title': 'トリガー',
  'home.step.2.body':
    'テレメトリまたはトランザクションに設定したしきい値がキャプチャを自動的に発生させます: 対象JVMのJFR記録、ヒープダンプ、またはスレッドダンプ。',
  'home.step.3.title': 'エージェント駆動',
  'home.step.3.body':
    'AIエージェントがMCPサーバーを通じてキャプチャを開始します。スコープの制限とすべての操作の監査記録が、そのアクセスに説明責任を持たせます。',
  'home.open.rule': 'オープンなキャプチャ、オープンな分析',
  'home.open.p1':
    'キャプチャは標準的なファイルです。JFR記録とHPROFヒープダンプは、JProfiler、JDK Mission Control、Eclipse MATで開けます。',
  'home.open.p2':
    '最も深い分析のために、jvmguardはJProfilerスナップショットを記録できます。これはJDBC、JPA、MongoDB、HTTPおよびgRPC呼び出し、Kafka、LLM呼び出しなどのサブシステムを内訳表示します。',
  'home.open.cta': '他ツールとの比較',
  'home.agent.rule': 'AIエージェントからの操作',
  'home.agent.p1':
    'jvmguardにはMCPサーバーが含まれています。AIエージェントはこれに接続して、JVMフリートを検出し、テレメトリ、トランザクション、MBeanを読み取り、JFR記録やヒープ/スレッドダンプを開始し、キャプチャを取得します。UIが提供するのと同じ操作がエージェントにも利用できます。',
  'home.agent.p2':
    '方向性は監督されたループです: エージェントが問題を検出し、適切なプロファイルをキャプチャし、次のステップを推奨し、人が各キャプチャを承認します。これをプロダクションで安全にするガードレールは、レートとオーバーヘッドの制限、およびすべてのエージェント操作の完全な監査記録です。',
  'home.agent.cta': 'プロダクションで安全を保つ方法',

  // Security
  'security.meta.title': 'セキュリティとデータフロー - jvmguard',
  'security.meta.description':
    'jvmguardエージェントがJVM内で行うことと行わないこと: デフォルトの収集なし、フォーンホームなし、クラウドへの送信なし、Apache 2.0でソース公開。',
  'security.hero.eyebrow': 'セキュリティとデータフロー',
  'security.hero.title': 'jvmguardがJVM内で行うこと',
  'security.hero.lede':
    'このページでは、jvmguardエージェントがJVMで何を収集するか、そのデータをどこへ送るか、ソースで何を確認できるかを説明します。プロダクションで実行するツールにとって、これは最初に答えるべき質問です。',
  'security.arch.rule': 'アーキテクチャ',
  'security.arch.alt':
    'jvmguardのアーキテクチャ: 監視対象VM内のエージェントが、コレクター、組み込みH2データベース、Web UIを実行するサーバーに接続します。',
  'security.arch.caption':
    'エージェントは認証された接続でサーバーにプッシュします。ユーザーはHTTPS経由でWeb UIにアクセスします。どのコンポーネントもベンダーと通信しません。',
  'security.collect.rule': 'デフォルトで収集されるもの',
  'security.collect.intro': 'jvmguardは低い定常状態オーバーヘッドで軽量なデータのみを記録します:',
  'security.collect.1': 'ヒープ、CPU、スレッド、GCなどの組み込みスカラーテレメトリ',
  'security.collect.2': '設定されたソースからのスカラーテレメトリ',
  'security.collect.3': '明示的に設定したトランザクション',
  'security.capture.rule': 'オンデマンドでキャプチャされるもの',
  'security.capture.intro':
    'ディープアーティファクトはデフォルトでは収集されません。キャプチャはトリガーが発生したとき、または単一のJVMで手動で開始したときにのみ実行されます。',
  'security.capture.1': '限定された期間のJFR記録',
  'security.capture.2': 'ヒープダンプ(HPROF)',
  'security.capture.3': 'スレッドダンプ',
  'security.capture.4': 'JProfilerスナップショット',
  'security.posture.rule': '運用の姿勢',
  'security.fact.1.k': 'フォーンホームなし',
  'security.fact.1.v':
    'エージェントはお客様のjvmguardサーバーに接続します。サーバーは外部への呼び出しを行いません。すべてのデータはネットワーク内に保持されます。',
  'security.fact.2.k': 'JMXポートなし',
  'security.fact.2.v':
    'エージェントはプロセス内でMBeanを読み取ります。MBeanを検査または操作するために、監視対象JVMでJMXコネクタサーバーを開く必要はありません。',
  'security.fact.3.k': '単一のH2ファイル',
  'security.fact.3.v':
    '記録されたデータと設定は、どちらも単一の組み込みH2ファイルに格納されます。運用する外部データベースはありません。',
  'security.fact.4.k': 'エージェント向けTLS',
  'security.fact.4.v': 'エージェントとサーバー間のトラフィックはTLSで暗号化および認証できます。',
  'security.fact.5.k': '3つのアクセスレベル',
  'security.fact.5.v':
    '管理者、プロファイラー、ビューアーのロールにより、単一のサーバーを別々のチーム用に分割できます。',
  'security.fact.6.k': 'ローカル、LDAP、またはSSO',
  'security.fact.6.v':
    '認証にはローカルに設定されたアカウント、LDAP、またはSSOを使用し、すべてUIで設定できます。',
  'security.license.rule': 'ライセンスとデプロイ',
  'security.license.1.strong': 'Apache License 2.0.',
  'security.license.1.body':
    'ソースは公開されています。プロダクションに導入する前に、読み、ビルドし、独自のツールで監査してください。',
  'security.license.2.strong': 'セルフホスト。',
  'security.license.2.body':
    'ファイアウォールの内側で実行されます。エアギャップネットワークに適しています。外部クラウドへの依存はありません。',
  'security.license.3.strong': '単一サーバー。',
  'security.license.3.body':
    'コレクター、組み込みデータベース、Web UIは1つのプロセスで実行されます。個別に保護するサービスはありません。',

  // Compare
  'compare.meta.title': 'jvmguardの比較',
  'compare.meta.description': 'jvmguardとAPM、継続的プロファイラー、JFRおよびjcmdとの比較。',
  'compare.hero.eyebrow': '比較',
  'compare.hero.title': 'jvmguardの比較',
  'compare.hero.lede':
    'jvmguardは、必要とされる場合にのみ、承認と監査証跡のもとでのみ、ディープなJVMプロファイルをキャプチャします。すでにお使いのツールとどう異なるかを説明します。',
  'compare.adjacent.rule': '隣接するアプローチ',
  'compare.cmp.1.name': 'APMスイート',
  'compare.cmp.1.base': 'Datadog、Dynatrace、New Relic、および類似のプラットフォーム',
  'compare.cmp.1.d1':
    'APMはフレームワークを計装し、スパンを継続的に測定します。jvmguardは定義したトランザクションのみを記録します。',
  'compare.cmp.1.d2':
    'APMはアプリケーションとエンドユーザーのデータを収集し、クラウドバックエンドに送信します。jvmguardはネットワーク内にとどまります。',
  'compare.cmp.1.d3':
    'jvmguardは置き換えではなく補完です。APMは広範囲の監視とアラートを提供し、jvmguardはディープで的を絞ったキャプチャを提供します。',
  'compare.cmp.2.name': '継続的プロファイラー',
  'compare.cmp.2.base': 'Grafana Pyroscope、ParcaおよびPolar Signals、Elastic Universal Profiling',
  'compare.cmp.2.d1':
    '継続的プロファイラーは広範にサンプリングし続け、止まりません。jvmguardは、必要な場合にのみ、必要な1台のJVMからヒープダンプやJFR記録といったディープアーティファクトをキャプチャします。',
  'compare.cmp.2.d2':
    '継続的プロファイラーは常にすべてのJVMで小さな一定のオーバーヘッドを支払います。jvmguardはキャプチャ中にのみそのコストを支払います。',
  'compare.cmp.3.name': '素のJFRとjcmd',
  'compare.cmp.3.base': 'JDKに内蔵。JFR記録、jcmd操作、分析にはJDK Mission Control。',
  'compare.cmp.3.d1':
    'jvmguardなしでヒープダンプを取得するには、プロダクションホストへのSSHアクセス、jcmdの手動実行、ファイルの転送が必要です。ほとんどの組織では、開発者にそのアクセス権がありません。',
  'compare.cmp.3.d2':
    'jvmguardはSSHワークフローを承認されたアクセスに置き換えます。プロファイラーロールのユーザーがUIまたはコーディングエージェントからキャプチャを開始し、監査ログが誰が、いつ、どのJVMに対して実行したかを記録します。',
  'compare.cmp.3.d3':
    'jcmdはその瞬間に実行したものをキャプチャします。jvmguardは、誰も監視していなくても、しきい値を超えた瞬間にキャプチャを自動的に発生させることもできます。',
  'compare.cmp.3.d4':
    '1つのトリガーが、個々のJVMを1台ずつではなく、フリート内の一致するすべてのJVMで発生します。',
  'compare.isnot.rule': 'jvmguardではないもの',
  'compare.isnot.p':
    'jvmguardはAPMではありません。継続的なトレーシング、分散トレースのエクスポート、メトリクスパイプラインはなく、フレームワークを自動計装もしません。軽量なテレメトリと定義したトランザクションのみを記録するため、常時オーバーヘッドは最小限に保たれます。ディープキャプチャは意図的でスコープが限定され、インフラ内にとどまります。ユーザーデータがリモートバックエンドにエクスポートされることはありません。',
  'compare.fits.rule': 'jvmguardが適する場面',
  'compare.fits.1':
    '開発者がホスト上のSSHセッションなしに、承認され監査されたアクセスを通じてプロダクションJVMからディープキャプチャを取得します。',
  'compare.fits.2':
    'AIエージェントがスコープとレート制限の範囲内でプロダクションJVMを操作し、すべての操作がSIEM互換の監査ログに書き込まれます。',
  'compare.fits.3':
    '軽量な常時監視が、しきい値を超えたときに自動的にキャプチャをトリガーします。',

  // Download
  'download.meta.title': 'ダウンロード - jvmguard',
  'download.meta.description':
    'Windows、macOS、Linux用のjvmguardをダウンロードするか、現在の開発バージョンをソースから実行します。',
  'download.hero.eyebrow': 'ダウンロード',
  'download.hero.title': 'jvmguardの入手',
  'download.hero.lede':
    'jvmguardは、バンドルされたJavaランタイムを持つ単一の自己完結型サーバーとしてインストールできます。ソースから実行して現在の開発バージョンを追跡することもできます。',
  'download.hero.currentVersion': '現在のバージョン:',
  'download.hero.changelog': '変更履歴',
  'download.installer.rule': 'インストーラー',
  'download.platform.linux.file': 'インストーラーまたはアーカイブ',
  'download.btn.download': 'ダウンロード',
  'download.btn.installerSh': 'インストーラー(.sh)',
  'download.btn.tar': 'Tarアーカイブ',
  'download.btn.rpm': 'RPM',
  'download.btn.deb': 'DEB',
  'download.installer.pending': '利用可能なリリースはまだありません。',
  'download.installer.note.1': 'インストール後、',
  'download.installer.note.2': 'を開いてセットアップウィザードを完了します。次に、JVMがエージェントポート(デフォルトは',
  'download.installer.note.3': ')を指すようにして監視を開始します。詳細は',
  'download.installer.note.link': 'インストールガイド',
  'download.installer.note.4': 'を参照してください。',
  'download.source.rule': 'ソースから実行',
  'download.source.p1':
    'インストーラーをビルドせずに現在の開発バージョンを実行するには、リポジトリをクローンしてサーバーを起動します。ビルドはJava 25ツールチェーンを自動的にプロビジョニングするため、個別のJavaセットアップは必要ありません。',
  'download.source.note.1': '',
  'download.source.note.2': 'が表示されたら、',
  'download.source.note.3': 'を開きます。デフォルトのWebポートは',
  'download.source.note.4': 'で、エージェントポートは',
  'download.source.note.5': 'です。',
};

export const zhCn: Messages = {
  // PageFrame
  'nav.overview': '概览',
  'nav.security': '安全',
  'nav.compare': '对比',
  'nav.download': '下载',
  'nav.docs': '文档',
  'a11y.skipToContent': '跳转到内容',
  'a11y.primaryNav': '主导航',
  'a11y.toggleTheme': '切换颜色主题',
  'a11y.home': 'jvmguard 主页',
  'a11y.language': '语言',
  'footer.note': '由 JProfiler 背后的团队打造。',

  // Home
  'home.meta.title': 'jvmguard',
  'home.meta.description':
    'jvmguard 以接近零的开销监视生产 JVM。出现异常时,它会捕获一份深层画像,展示原因所在。',
  'home.hero.eyebrow': '自动化生产环境画像',
  'home.hero.title.1': '生产环境画像,',
  'home.hero.title.2': '可控且可审计。',
  'home.hero.title.3': '面向人类与智能体。',
  'home.hero.lede':
    '在生产环境中进行深层画像捕获存在风险,但有时必不可少。jvmguard 将其纳入管控:无论由人、监控告警还是智能体触发,每次捕获都经过授权并留有审计记录。',
  'home.hero.cta.download': '下载',
  'home.hero.cta.docs': '阅读文档',
  'home.hero.cta.github': '在 GitHub 上查看',
  'home.hero.cred': '基于 Apache License 2.0 的开源软件。',
  'home.shot.alt':
    'jvmguard Web 界面显示带有实时遥测的 JVM 集群,以及包含子系统、堆转储和 MBean 快照选项的记录 JProfiler 快照对话框。',
  'home.shot.caption': 'jvmguard Web 界面:整个集群尽在同一个屏幕。实时遥测,每个操作只需点击一次。',
  'home.features.rule': '功能',
  'home.feature.1.title': '默认纯 Java 代理',
  'home.feature.1.body':
    '代理通过类加载对选定的方法进行插桩。除非明确执行画像操作,否则不加载任何本地库。',
  'home.feature.2.title': '触发器与动作',
  'home.feature.2.body':
    '在遥测或事务上设置阈值。一旦越过阈值,jvmguard 就会运行配置的动作:记录 JFR、获取堆转储或线程转储、写入收件箱、发送电子邮件或调用 Webhook。响应在界面中组合配置。',
  'home.feature.3.title': '按需捕获',
  'home.feature.3.body':
    '轻量遥测和定义的事务提供触发深层捕获的信号。稳态开销保持接近零。',
  'home.feature.4.title': '无需 JMX 端口',
  'home.feature.4.body':
    '代理在进程内读取 MBean。无需向网络暴露 JMX 连接器服务器,即可检查和操作 MBean。',
  'home.feature.5.title': '管理 JVM 集群',
  'home.feature.5.body':
    '将数百个 JVM 组织为层级组。配置会被继承,组对其下的每个 JVM 生效,新加入的 JVM 无需预先设置。',
  'home.feature.6.title': '可配置的数据保留',
  'home.feature.6.body':
    '记录的数据随时间推移聚合为更大的间隔。事务可在设定的阈值之后丢弃。',
  'home.feature.7.title': '自托管,支持离线环境',
  'home.feature.7.body':
    '可安装在 Windows、macOS 或 Linux 上。所有记录的数据都保存在本地,不依赖任何外部云服务,因此服务器可以运行在自己的基础设施上,包括物理隔离的网络。',
  'home.steps.rule': '三种捕获方式',
  'home.step.1.title': '手动',
  'home.step.1.body': '在界面中为任何已连接的 JVM 启动捕获。捕获结果会进入收件箱。',
  'home.step.2.title': '触发',
  'home.step.2.body':
    '在遥测或事务上设置的阈值会自动触发捕获:受影响 JVM 的 JFR 记录、堆转储或线程转储。',
  'home.step.3.title': '智能体驱动',
  'home.step.3.body':
    'AI 智能体通过 MCP 服务器启动捕获。范围限制和每次操作的审计记录确保这种访问可追溯。',
  'home.open.rule': '开放的捕获,开放的分析',
  'home.open.p1':
    '捕获产物是标准文件。JFR 记录和 HPROF 堆转储可以在 JProfiler、JDK Mission Control 或 Eclipse MAT 中打开。',
  'home.open.p2':
    '要进行最深入的分析,jvmguard 可以记录 JProfiler 快照,细分 JDBC、JPA、MongoDB、HTTP 和 gRPC 调用、Kafka 以及 LLM 调用等子系统。',
  'home.open.cta': '与其他工具的对比',
  'home.agent.rule': '用 AI 智能体驱动',
  'home.agent.p1':
    'jvmguard 内置 MCP 服务器。AI 智能体连接它,即可发现 JVM 集群,读取遥测、事务和 MBean,启动 JFR 记录或堆转储、线程转储,并取回捕获产物。界面提供的所有操作同样对智能体开放。',
  'home.agent.p2':
    '方向是一个受监督的闭环:智能体发现问题,捕获合适的画像,并建议下一步,每一步捕获都由人审批。使其可安全用于生产的护栏是速率与开销限制,以及每个智能体操作的完整审计记录。',
  'home.agent.cta': '如何在生产中保持安全',

  // Security
  'security.meta.title': '安全与数据流 - jvmguard',
  'security.meta.description':
    'jvmguard 代理在 JVM 中做什么与不做什么:默认不采集、不回传、无云端外发,以 Apache 2.0 源码可用。',
  'security.hero.eyebrow': '安全与数据流',
  'security.hero.title': 'jvmguard 在 JVM 内部做什么',
  'security.hero.lede':
    '本页说明 jvmguard 代理在 JVM 中收集什么、把数据发送到哪里,以及可以在源码中核查什么。对于运行在生产环境中的工具,这是首先要回答的问题。',
  'security.arch.rule': '架构',
  'security.arch.alt':
    'jvmguard 架构:受监控 VM 中的代理连接到运行收集器、嵌入式 H2 数据库和 Web 界面的服务器。',
  'security.arch.caption':
    '代理通过经过身份验证的连接推送到服务器。用户通过 HTTPS 访问 Web 界面。没有任何组件与厂商通信。',
  'security.collect.rule': '默认收集的内容',
  'security.collect.intro': 'jvmguard 只记录轻量数据,稳态开销很低:',
  'security.collect.1': '堆、CPU、线程、GC 等内置标量遥测',
  'security.collect.2': '来自已配置来源的标量遥测',
  'security.collect.3': '显式配置的事务',
  'security.capture.rule': '按需捕获的内容',
  'security.capture.intro':
    '深层产物默认不采集。只有当触发器触发,或在单个 JVM 上手动启动时,才会执行捕获。',
  'security.capture.1': '限定时长的 JFR 记录',
  'security.capture.2': '堆转储(HPROF)',
  'security.capture.3': '线程转储',
  'security.capture.4': 'JProfiler 快照',
  'security.posture.rule': '运维姿态',
  'security.fact.1.k': '不回传厂商',
  'security.fact.1.v':
    '代理连接到您自己的 jvmguard 服务器。服务器不发起任何外发调用。所有数据都留在您的网络内。',
  'security.fact.2.k': '无 JMX 端口',
  'security.fact.2.v':
    '代理在进程内读取 MBean。检查或操作 MBean 无需在受监控的 JVM 上开放 JMX 连接器服务器。',
  'security.fact.3.k': '单一 H2 文件',
  'security.fact.3.v':
    '记录的数据和配置都存放在一个嵌入式 H2 文件中,没有需要运维的外部数据库。',
  'security.fact.4.k': '代理通信 TLS',
  'security.fact.4.v': '代理到服务器的流量可以通过 TLS 加密和认证。',
  'security.fact.5.k': '三级访问权限',
  'security.fact.5.v': '管理员、分析员和查看者角色将一台服务器划分给不同团队使用。',
  'security.fact.6.k': '本地、LDAP 或 SSO',
  'security.fact.6.v': '认证可使用本地配置的账户、LDAP 或 SSO,均可在界面中配置。',
  'security.license.rule': '许可与部署',
  'security.license.1.strong': 'Apache License 2.0。',
  'security.license.1.body':
    '源码公开。在进入生产环境之前,可以用自己的工具阅读、构建和审计它。',
  'security.license.2.strong': '自托管。',
  'security.license.2.body': '运行在防火墙之后。适用于物理隔离的网络。不依赖外部云服务。',
  'security.license.3.strong': '单一服务器。',
  'security.license.3.body':
    '收集器、嵌入式数据库和 Web 界面运行在同一个进程中。没有需要单独保护的服务。',

  // Compare
  'compare.meta.title': 'jvmguard 对比',
  'compare.meta.description': 'jvmguard 与 APM、持续画像工具以及 JFR 和 jcmd 的对比。',
  'compare.hero.eyebrow': '对比',
  'compare.hero.title': 'jvmguard 对比',
  'compare.hero.lede':
    'jvmguard 只在确有必要时、且只在获得授权并留有审计轨迹的情况下捕获深层 JVM 画像。下面说明它与可能已在使用的工具有何不同。',
  'compare.adjacent.rule': '相邻方案',
  'compare.cmp.1.name': 'APM 套件',
  'compare.cmp.1.base': 'Datadog、Dynatrace、New Relic 及类似平台',
  'compare.cmp.1.d1':
    'APM 对框架插桩并持续测量 span。jvmguard 只记录定义的事务。',
  'compare.cmp.1.d2':
    'APM 收集应用和最终用户数据并发送到云端后端。jvmguard 始终留在您的网络内部。',
  'compare.cmp.1.d3':
    'jvmguard 是补充而非替代。APM 提供广度和告警,jvmguard 提供深层、定向的捕获。',
  'compare.cmp.2.name': '持续画像工具',
  'compare.cmp.2.base': 'Grafana Pyroscope、Parca 和 Polar Signals、Elastic Universal Profiling',
  'compare.cmp.2.d1':
    '持续画像工具广泛采样且永不停止。jvmguard 只在确有必要时,从需要它的那一个 JVM 捕获堆转储或 JFR 记录这样的深层产物。',
  'compare.cmp.2.d2':
    '持续画像工具在每个 JVM 上始终付出一笔虽小但恒定的开销。jvmguard 只在捕获期间付出这笔开销。',
  'compare.cmp.3.name': '原生 JFR 与 jcmd',
  'compare.cmp.3.base': '内置于 JDK。JFR 记录、jcmd 操作,以及用于分析的 JDK Mission Control。',
  'compare.cmp.3.d1':
    '没有 jvmguard,获取堆转储意味着 SSH 登录生产主机、手工运行 jcmd 并传输文件。在大多数组织中,开发人员没有这样的权限。',
  'compare.cmp.3.d2':
    'jvmguard 用授权访问取代 SSH 流程。拥有分析员角色的用户从界面或通过编程智能体启动捕获,审计日志记录谁在何时对哪个 JVM 执行了操作。',
  'compare.cmp.3.d3':
    'jcmd 只能捕获当下手动执行的内容。jvmguard 还可以在阈值被越过的瞬间自动触发捕获,无需有人值守。',
  'compare.cmp.3.d4':
    '一个触发器在集群中所有匹配的 JVM 上同时触发,而不是逐个 JVM 单独执行。',
  'compare.isnot.rule': 'jvmguard 不是什么',
  'compare.isnot.p':
    'jvmguard 不是 APM。它没有持续追踪、没有分布式跟踪导出、没有指标管道,也不会自动插桩框架。它只记录轻量遥测和定义的事务,因此常驻开销保持最低。深层捕获是有意的、有范围限定的,并且留在您的基础设施上。没有任何用户数据被导出到远端后端。',
  'compare.fits.rule': 'jvmguard 适用的场景',
  'compare.fits.1':
    '开发人员无需在主机上建立 SSH 会话,即可通过授权且可审计的访问,从生产 JVM 获取深层捕获。',
  'compare.fits.2':
    'AI 智能体在范围和速率限制内操作生产 JVM,每次操作都写入兼容 SIEM 的审计日志。',
  'compare.fits.3': '轻量的常驻监控在阈值被越过时自动触发捕获。',

  // Download
  'download.meta.title': '下载 - jvmguard',
  'download.meta.description':
    '下载 Windows、macOS 或 Linux 版 jvmguard,或从源码运行当前开发版本。',
  'download.hero.eyebrow': '下载',
  'download.hero.title': '获取 jvmguard',
  'download.hero.lede':
    '将 jvmguard 安装为一个自带 Java 运行时的独立服务器。也可以从源码运行,以跟踪当前开发版本。',
  'download.hero.currentVersion': '当前版本:',
  'download.hero.changelog': '更新日志',
  'download.installer.rule': '安装程序',
  'download.platform.linux.file': '安装程序或归档包',
  'download.btn.download': '下载',
  'download.btn.installerSh': '安装程序(.sh)',
  'download.btn.tar': 'Tar 归档',
  'download.btn.rpm': 'RPM',
  'download.btn.deb': 'DEB',
  'download.installer.pending': '暂无可用版本。',
  'download.installer.note.1': '安装完成后,打开 ',
  'download.installer.note.2': ' 并完成设置向导。然后让 JVM 指向代理端口(默认 ',
  'download.installer.note.3': ')即可开始监控。详见',
  'download.installer.note.link': '安装指南',
  'download.installer.note.4': '。',
  'download.source.rule': '从源码运行',
  'download.source.p1':
    '要在不构建安装程序的情况下运行当前开发版本,克隆仓库并启动服务器即可。构建会自动提供 Java 25 工具链,无需单独安装 Java。',
  'download.source.note.1': '看到 ',
  'download.source.note.2': ' 后,打开 ',
  'download.source.note.3': '。默认 Web 端口为 ',
  'download.source.note.4': ',代理端口为 ',
  'download.source.note.5': '。',
};

const CATALOGS: Record<Locale, Messages> = {en, ko, ja, 'zh-cn': zhCn};

export function getMessages(locale: Locale): Messages {
  return CATALOGS[locale] ?? en;
}

// Structured accessors so components don't build catalog keys dynamically.

export function homeFeatures(m: Messages) {
  return [1, 2, 3, 4, 5, 6, 7].map((i) => ({
    title: m[`home.feature.${i}.title` as MessageKey],
    body: m[`home.feature.${i}.body` as MessageKey],
  }));
}

export function homeSteps(m: Messages) {
  return [1, 2, 3].map((i) => ({
    no: `0${i}`,
    title: m[`home.step.${i}.title` as MessageKey],
    body: m[`home.step.${i}.body` as MessageKey],
  }));
}

export function securityCollect(m: Messages) {
  return [1, 2, 3].map((i) => m[`security.collect.${i}` as MessageKey]);
}

export function securityCapture(m: Messages) {
  return [1, 2, 3, 4].map((i) => m[`security.capture.${i}` as MessageKey]);
}

export function securityFacts(m: Messages) {
  return [1, 2, 3, 4, 5, 6].map((i) => ({
    k: m[`security.fact.${i}.k` as MessageKey],
    v: m[`security.fact.${i}.v` as MessageKey],
  }));
}

export function securityLicense(m: Messages) {
  return [1, 2, 3].map((i) => ({
    strong: m[`security.license.${i}.strong` as MessageKey],
    body: m[`security.license.${i}.body` as MessageKey],
  }));
}

export function compareComparisons(m: Messages) {
  const counts = [3, 2, 4];
  return [1, 2, 3].map((i) => ({
    name: m[`compare.cmp.${i}.name` as MessageKey],
    base: m[`compare.cmp.${i}.base` as MessageKey],
    differs: Array.from({length: counts[i - 1]}, (_, j) => m[`compare.cmp.${i}.d${j + 1}` as MessageKey]),
  }));
}

export function compareFits(m: Messages) {
  return [1, 2, 3].map((i) => m[`compare.fits.${i}` as MessageKey]);
}
