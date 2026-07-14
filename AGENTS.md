# JvmGuard — agent guide

JvmGuard is a production JVM-profiling server: a **Spring Boot** application (Spring DI, embedded Tomcat)
that serves the Vaadin 25 web UI and hosts the backend collaborators as Spring beans, reusing the
in-process `Server`/`ServerConnection` from `:backend:connector`. Build is **Gradle (Kotlin DSL)**,
rooted at this directory; modules use the standard **Maven source layout**
(`modules/<m>/src/main/{java,kotlin,resources}`, `src/test/...`) with build files named
`modules/<m>/build.gradle.kts`. Java baseline is **25** (provisioned by the
[foojay toolchain resolver](https://github.com/gradle/foojay-toolchains); the system `java`
may be older). The Gradle build is **fully self-contained**: the build logic lives in the local
`buildSrc` (under the `dev.jvmguard.build.*` packages), versions are declared once in
`gradle/libs.versions.toml`, and module registration is explicit in `settings.gradle.kts`.
**Never commit or push** — the user reviews and commits all changes.

## Backend — `modules/server` (`:server`, the Spring Boot app)

`dev.jvmguard.server.ServerMain` (`@Component`, `SmartLifecycle`) is the `main()` entry (no args); it builds a
`SpringApplication(JvmGuardApplication.class)`, registers a `bootstrap` initializer that applies a pending
`jvmguard.bak` restore before the auto-configured `DataSource` opens the H2 files (and inits agent-side logging),
selects the `integrationTest` profile when applicable, and runs it. `JvmGuardApplication` is `@SpringBootConfiguration
@EnableAutoConfiguration @EnableVaadin("dev.jvmguard.ui") @Import(SpringConfiguration.class)`;
`SpringConfiguration` component-scans the backend packages (`dev.jvmguard.common/data/collector/
database/rest/connector`) and `@Import`s the server-module beans. Backend collaborators are Spring
beans with **constructor injection**; the agent `ConnectionServer`, collector, telemetry, REST, and the
embedded web server are all in the one context. Notes:
- The in-process `Server` is published through `ServerFactory.setLocalServer(...)` (a static holder, so
  the agent-facing `connector` classes can reach it without the Spring context); inside the context,
  collaborators use Spring constructor injection.
- **Config** is standard Spring: the `jvmguard:` section of `application.yaml` binds to
  `dev.jvmguard.common.JvmGuardProperties` (`@ConfigurationProperties("jvmguard")`, `-Djvmguard.*` overrides),
  constructor-injected. Defaults ship in `modules/server/src/main/resources/application.yaml`; installs
  override via `<install>/config/application.yaml`. `JvmGuardEnvironmentPostProcessor` (in
  `META-INF/spring.factories`) runs before refresh: it binds the properties, resolves the install-layout
  directories (`JvmGuardDirectories` + the jar-vs-dev `LoadingDescriptor`, exposed as a bean), and publishes
  the early bootstrap keys (`server.port` from `httpPort`, `logging.config`). Static-only callers
  (`PasswordHelper`, `MailHelper`, `TelemetryDataInterval`) read the bound properties via the
  `JvmGuardConfig` holder. `integrationTest` is the Spring profile, selected in `ServerMain.main()`.
- **HTTPS / reverse-proxy** are applied to Boot's embedded Tomcat by `WebServerCustomizer` (a
  `WebServerFactoryCustomizer<TomcatServletWebServerFactory>`: `factory.setSsl(...)` from the keystore
  resolved by `WebServerSupport`, a `connector.setProperty("server","")` customizer that suppresses the
  Server header, and a `RemoteIpValve` engine valve when `reverseProxy` is set).
- **Security is Spring Security** (`SecurityConfiguration`): an `@Order(0)` stateless HTTP-Basic chain for
  the REST API (`/api/**`, opt-in via `restApiEnabled`, `RestApiKeyAuthenticationProvider`) and an
  `@Order(1)` Vaadin chain (`VaadinSecurityConfigurer` + `loginView`). Web views carry JSR-250
  `@AnonymousAllowed`/`@PermitAll`/`@RolesAllowed(Roles.*)` (`Roles` in `dev.jvmguard.data.user`); the
  principal is `JvmGuardUserDetails` (authorities expand the `AccessLevel` hierarchy). Login flows
  `LoginView` → `SecurityBridge` (a `ServerFactory`-style holder, since views aren't beans) →
  `AuthenticationManager` → `JvmGuardAuthenticationProvider` → `Server.authenticate` then `Server.connect`.
- **Backend authorization is Spring method security** (`@EnableMethodSecurity`): the gated
  `ServerConnectionImpl` / `AbstractServerConnectionImpl` methods carry the meta-annotations
  **`@RequireAdmin` / `@RequireProfiler` / `@RequireViewer`** (in `dev.jvmguard.data.user`, each composing
  `@PreAuthorize("hasRole(...)")` over a `Roles` constant, never hand-written), enforced via an AOP proxy of
  the prototype connection. This is the authoritative access-level boundary; route annotations are UX +
  defense-in-depth. The **REST** resources use the same `@Require*` markers (the chain only authenticates),
  with a 403 handler in `RestExceptionHandler` and `RestInterface` constructor-injected (no static holder).
  REST endpoints produce `text/plain`, so a client must `Accept` it (default is JSON). **Group-scoping** and
  the **per-file log level** stay backend data-scoping, reading the connection's real `User`.
- **Logging** is Spring-Boot-managed **Logback** (`logging.config` → the `logback.xml` shipped in the install root;
  source is `dist-template/logback.xml`); the agent keeps its own `LoggingHandler`. There is no log4j2 on the server
  classpath.
- **Persistence** is a single embedded **H2** database (file `jvmguard` under the data directory), behind a
  **HikariCP** pool, with **Flyway** migrations under `modules/server/src/main/resources/db/migration`. Boot's
  auto-configuration is **not** excluded: the `DataSource` (`spring.datasource.*`, including the HikariCP pool),
  Flyway, the transaction manager and `JdbcClient` are all Boot-managed. `Database` is a `@Component` that wraps the
  auto-configured `HikariDataSource` (for shutdown and the `DB Connections` telemetry) — it is not a manual
  pool/Flyway setup. The only pre-refresh work is the `bootstrap` initializer in `ServerMain.main()` (see above).
  SQL is hand-written (`JdbcClient` for fixed-schema CRUD, raw JDBC for blob/time-series writes); there are no Spring
  Data repositories and no `@Transactional` (writes go through a `DatabaseWriter` thread pool).
- Packaging: install4j launches `dev.jvmguard.server.ServerMain` against the exploded `lib/server`
  classpath (no fat jar).

## Frontend — `modules/ui` (`:ui`)

The UI is a single Vaadin 25 frontend, served by the Spring Boot app's **embedded Tomcat 11
(`jakarta`)** at the root `/`, reusing the in-process backend.

- Vaadin **25.1 (Flow)**, **Aura** theme, free/open-source components only (no commercial Vaadin).
- **Kotlin** — the entire module (production + tests) is Kotlin (`kotlin("jvm")`), built with the
  **Karibu-DSL** (`com.github.mvysny.karibudsl:karibu-dsl`). Follow
  **[modules/docs/agent/kotlin-style.md](./modules/docs/agent/kotlin-style.md)** (general Kotlin + interop) and
  **[modules/docs/agent/web-ui-style.md](./modules/docs/agent/web-ui-style.md)** (the canonical UI style guide: idioms, forms,
  testing, known gotchas).
- **Spring-managed** via **vaadin-spring** (the `SpringInstantiator`); `@Route` views are Spring beans.
  `:ui` is a plain Kotlin **library jar** (no WAR) on the Spring Boot app's classpath — the
  `org.springframework.boot` + `vaadin-spring-boot-starter` wiring lives in `:server`.
- Reuses the backend via the in-process `:backend:connector` (`ServerFactory.lookup()` →
  `Server.login(...)` → `ServerConnection`) and `:backend:data` (POJOs). `connector`, `data`, and
  `vaadin-spring` are **`compileOnly`** in `:ui` (the Boot app provides them at runtime) — no WAR and no
  parent-loading classloader trick.

#### Working on the UI

Theming, components, forms, and testing conventions live in **[modules/docs/agent/web-ui-style.md](./modules/docs/agent/web-ui-style.md)**
(read it before touching UI code, and verify APIs against the Vaadin 25.1 MCP server, not memory).
Orientation:

- **Package layout (extend, don't flatten):** `dev.jvmguard.ui.AppShell` (root `AppShellConfigurator`);
  `shell/` (the app frame, `MainLayout`); `server/` (backend access + session-scoped cross-view concerns:
  `UserSession`, `Sessions`, `LoginService`, `NotificationPoller`); `views/<area>/` (one package per view,
  each its own `@Route`); `components/<component>/` (shared Flow/Lit toolkit, e.g. the sparkline). Views may
  call `ServerConnection` directly via `UserSession.getServerConnection()`; cross-view reuse lives in
  `components/`, not copied between views.
- **Build & run:** `./gradlew :ui:build` produces the Vaadin frontend bundle. To run the dev
  server, launch `dev.jvmguard.server.ServerMain` (IntelliJ run config) — `:server` auto-depends on
  `:ui:vaadinBuildFrontend` from IntelliJ, so the bundle stays fresh. UI at
  `http://localhost:8020/`. Vaadin auto-detects the IDE and runs dev mode (hot-reload); add
  `-Dvaadin.productionMode=true` to serve the pre-built bundle. Append **`?mock`** to log in against the
  canned `MockServerConnectionImpl` data instead of the live backend (auth still needs a real user/password;
  the flag only swaps the connection via `Sessions.captureMock` → `server.login(..., mock=true)`).
- **Testing:** browserless (`JvmGuardBrowserlessTest`, the fast per-change check) plus Playwright e2e
  (`./gradlew :ui:e2eTest`, its own `ServerMain` on 8123/8948). Full API and gotchas in
  web-ui-style.md.

### Live demo data (the demo cluster) — the real-agent alternative to `?mock`

`dev.jvmguard.demo.server.JvmGuardDemoServerStarter` (module **`:demo`**)
is a multi-JVM workload generator: it spawns 10 child JVMs, each launched with
`-javaagent:dist/agent/jvmguard.jar` (dev-mode fallback; `agent/jvmguard.jar` in a real install), each running
one `dev.jvmguard.demo.server.DemoService` role simulating an e-commerce "Online Boutique" fleet
(Storefront, Catalog, Recommendation, Currency, Cart, Checkout, Payment, Shipping, Notification).
Instrumentation is **Declared annotations only** (`@MethodTransaction` operations + nested sub-steps,
`@Telemetry` getters) — no JEE probes, no config required (annotations are auto-detected at class-load).
All "work" is `Thread.sleep` (no CPU load); arrivals are Poisson and a `TrafficProfile`
modulates the rate across multiple timescales (weekly/diurnal/hourly/short sines + daily
jitter) with periodic peaks (flash sales every 3h, a nightly checkout batch, a payment-decline surge),
exposed as a live-tweakable MXBean. Groups: `Demo/Storefront` (pool of 3), `Demo/Browse`
{Catalog, Recommendation, Currency}, `Demo/Purchase` {Cart, Checkout, Payment}, `Demo/Fulfillment`
{Shipping, Notification}. They connect to a jvmguard server (`dev.jvmguard.server.ServerMain`). The starter
`inheritIO`s the children and a JVM shutdown hook `destroy()`s them all on exit, so stopping the starter
tears the whole cluster down. The demo module targets **Java 21** (the installers bundle JRE 21). The
shipped `dist-template/demo/jvmguard_demo.json` is a valid empty `recordingConfig` for optional manual import
— thresholds/triggers are configured in the UI and exported by the user. **Prerequisite:**
`dist/agent/jvmguard.jar` (+ `dist/agent/lib/agent.jar`) must exist (the agent the demo VMs load), and the
starter's working directory must resolve that path.

**The demo VMs do NOT load `dist/agent/lib/agent.jar` directly — they load a copy from `~/.jvmguard/agent/<hash>/<build-version>/agent.jar`.** The bootstrap (`AgentLocation`) extracts the agent there keyed by the manifest **build version**, and a dev rebuild keeps the *same* build number — so it reuses the stale cached copy and silently ignores your rebuild. After rebuilding the agent, **`rm -rf ~/.jvmguard/agent`** (or the demo loads yesterday's bytecode). `-Djvmguard.debugBootstrap=true` makes `AgentLocation` print which jar it actually loaded (the demo starter forwards the flag to the workers).

**Why this matters (not just for demos):** the demo cluster is the only easy way to exercise the
**real agent data path**, which differs from `?mock` in a way that bites the MBean browser. A real
connection serializes MBean attribute values through `dev.jvmguard.mbean.data.MBeanTransfer`
(`writeOpenTypeValues` / `readSimpleValues` → `OpenValueTransfer`), so the web side receives composites as
`CompositeDataWithType` (or a value `Object[]`) and primitive arrays as **boxed `Object[]`** — exactly the
shapes `dev.jvmguard.mbean.common.MBeanHelper` (and `dev.jvmguard.ui.views.data.mbeans.AttributeNode.buildTree` / `OpenTypeHelper` / `ObjectArrayHelper`) expand. The `?mock` path (`MockMBeans`) instead serves
the platform MBean server's **raw** objects (`long[]`, `CompositeDataSupport`, `TabularDataSupport`),
bypassing that serialization. So the mock is a *harsher* input that surfaces formatter edge cases
(primitive-array length, composite/tabular placeholders) which **never occur on a real connection** — handy
for hardening, but verify anything composite/array-shaped against the demo cluster too.

## Backend & agent integration tests — `modules/integration` (`:integration`)

End-to-end tests that launch a real jvmguard server (`dev.jvmguard.server.ServerMain`, in-process) plus one
or more agent-instrumented workload JVMs and assert on collected data against golden `*.xml`/`*.json`. They
are **JUnit 6** tests: a test is a `JvmGuardTest` subclass whose `connect()` drives assertions through a
`TestServerConnection`, and its workload is a matching `*Workload` (`AbstractJvmGuardRun`) subclass (the base
`getRunClassName()` maps a `<Base>Test` driver to its `<Base>Workload`). The harness is a JUnit extension +
fixture (`AgentIntegrationExtension` / `AgentFixture`) that boots the server, launches the workload child
JVM(s), runs `connect()`, and tears down; discovery/grouping/reporting are stock JUnit.

**Layout** (Maven source layout; everything is under package `dev.jvmguard.integration.*`. Golden XML embeds
FQNs/package names, so a code rename must be mirrored in the goldens):
- Drivers + fixture + comparators run in the JUnit JVM (baseline 25): `src/integrationTest/{java,kotlin}/`.
- Golden data: `src/integrationTest/resources/.../<test>/data/*.xml`.
- Workloads run in the agent-instrumented child JVMs and target older JDKs, so they live in separate
  subprojects nested under `workloads` (mirroring `:agent`): `:integration:workloads`
  (release 8), `:workloads:logging` (release 8 + log4j1/log4j2/logback), `:workloads:java21`
  (compiled on and targeting JDK 21). All three feed the
  child-JVM classpath via the `workloadRuntimeClasspath` configuration.

**JDK matrix** — property-driven, defaults to ONE vm (the JDK running the build). `-Pjdks=all` runs the
central `allTestJdks` list (`8,11,17,21,25`); `-Pjdks=21,25` pins a subset. Per-test floors are open-ended
`@MinJdk(n)` annotations (never closed sets, so a new JDK is picked up automatically); a test may also opt
out at runtime via `isRunOnVM(VMConfig)` (reported as skipped). Even the default single-JDK run is on the
order of **2 hours**, and a full `-Pjdks=all` run is several times longer — while iterating, scope to one
test and the current JDK.

**Running** (from the jvmguard root):
- One test, current JDK: `./gradlew :integration:integrationTest --tests "*<TestName>"`
  (pin a JDK with `-Pjdks=25`).
- Re-record golden data: `./gradlew :integration:integrationTest --tests "*<TestName>" -Pjvmguard.record=true`
  — each driver regenerates its golden tree under the test's work dir `output/` instead of comparing. Copy
  the regenerated `*.xml` back over `src/integrationTest/resources/.../data/`, then re-run without the flag
  to confirm green. Re-record freely but spot-check the diffs.
- Whole suite, full matrix: `./gradlew :integration:integrationTest -Pjdks=all`.
- CI subset (the `@Tag("citest")` tests, current JDK): `./gradlew :integration:citest`.

**Output & data** — under **`build/gradle/jvmguard/integration/integration/<TestName>-jdk<N>/`**: `data/` is
the live server's data directory (`db/` embedded H2, `snapshots/`, `log/` incl. `event.log`, `ssl/`),
`output/` holds recorded golden, and `*-console.log` / `JVM*.log` are the child-JVM logs. The fixture wipes
each per-test work dir at the start of every run (a stale H2 tx DB makes the HOUR read sum prior runs).
Tests boot the server on fixed ports (8010/8846), so the task pins `forkEvery=1` + `maxParallelForks=1`
(strictly one server at a time). A crashed `ServerMain` can leave the embedded `db/` locked; if a run can't
start the server, kill stray `jvmguard.testClass` / `jvmguard.vmPort` java processes and delete the work dir.

**Timing:** a single test takes several minutes; `@Telemetry` tests wait on config round-trips and
`getRunCount > 1` tests (e.g. `CapTest`) launch the workload once per run, so they run longer.

## Module map & integration points

Modules are grouped by concern under `modules/`: **`agent/`** is the Java-8 instrumentation domain that
loads into monitored JVMs — `:agent` is a container; its children are **`:agent:core`** (base agent
implementation), `:agent:java11` (Java 11+ additions), `:agent:mbean`, `:agent:api` (instrumentation
annotations), `:agent:bootstrap` (premain launcher), and **`:agent:bundle`** (the aggregate: it
`api`-bundles the others and builds the relocated fat jar). **`backend/`** is the Kotlin/JDK-25 server side
(`:backend:data`/`collector`/`connector`/`rest`). `:ui`, `:server`, `:integration`, `:demo`,
`:installer`, `:docs`, `:website` are top-level. The backend builds *on* the agent (e.g. `:backend:collector`
and `:backend:data` depend on `:agent:bundle`), so the agent modules are a shared foundation, not leaves.
`:agent:bundle` is a leaf (not the container) specifically so IntelliJ imports it cleanly — an
aggregate-that-is-also-a-parent produced a phantom module. Its own default `jar` (empty `bundle.jar`) is
the api-bundle artifact consumers resolve; the relocated fat jar is `distJar` → **`agent-bundle.jar`**,
which `:agent:bootstrap`'s `copyDist` renames to `agent.jar` for `dist/agent/lib/` because the runtime
(`AgentInit.AGENT_JAR`) hardcodes that name. Do **not** consume the fat jar as a compile dependency.

- `modules/server` — the Spring Boot app (`JvmGuardApplication` + `SpringConfiguration`, `ServerMain`,
  the embedded-server customizer for HTTPS/reverse-proxy, the REST gate filter, the `/test` control
  filter). `modules/ui` — the Vaadin UI (Kotlin/Karibu). `modules/backend/rest` — the Spring MVC REST API.
- `modules/backend/connector` / `data` / `collector` — the backend (`Server`/`ServerConnection`,
  POJOs, collector), all Spring beans. `dev.jvmguard.database` (incl. `Database`) lives in
  `:server`; `dev.jvmguard.common` lives in `:backend:data`.
  `modules/agent` — the agent loaded into monitored JVMs (independent of the server's
  Spring context and logging).
- `gradle/libs.versions.toml` (all versions, single source), `settings.gradle.kts` (modules
  registered with explicit `include()`), the `foojay-resolver-convention` plugin provisions the
  JDK toolchains. The product-orchestration tasks (`dist`, `media`, `release`, `beta`, …) live on
  the root project (`:dist` aggregates `:*:dist`).

## Docs

- **[modules/docs/agent/app-mission.md](./modules/docs/agent/app-mission.md)** — what jvmguard is and why: the Automatic Production
  Profiling (APP) mission, principles, and direction. Read this for product intent.
- **[modules/docs/agent/kotlin-style.md](./modules/docs/agent/kotlin-style.md)** — the canonical general Kotlin language + Kotlin ⇄
  Java interop style guide, for **all** Kotlin in the project (backend modules and `modules/ui`).
- **[modules/docs/agent/web-ui-style.md](./modules/docs/agent/web-ui-style.md)** — the UI-specific style guide for `modules/ui`
  (Karibu-DSL, forms, Vaadin testing); extends `kotlin-style.md` (also linked above).
- **[modules/docs/agent/help-screenshots.md](./modules/docs/agent/help-screenshots.md)** — how the documentation site's UI
  figures are generated from the Playwright screenshot tests.
- **[modules/docs/agent/serialization.md](./modules/docs/agent/serialization.md)** — config serialization architecture: the two
  mechanisms (Jackson for server beans, the custom codec for agent beans), the export file format, and
  who consumes what.
- **`modules/docs`** — the product documentation, an Astro Starlight site (Markdown/MDX, light/dark,
  GitHub Pages). The `.mdx` pages under `src/content/docs` and `astro.config.mjs` are **hand-maintained**.
  Tasks: `:docs:copyScreenshots` pulls the Playwright UI shots into
  `public/images/ui/`; `:docs:npmBuild` builds the static site; `:docs:npmDev`/`npmPreview` serve it
  locally. Deployed by `.github/workflows/docs.yml`. When editing the docs, follow `modules/docs/agent/docs-voice.md`
  (voice & style). The website copy follows `modules/docs/agent/website-voice.md`.
