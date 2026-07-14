# web — UI style guide

Conventions for `modules/web` (Vaadin 25.1 / Aura + Karibu-DSL). **Extends
[kotlin-style.md](./kotlin-style.md) — read that first** for the general Kotlin language and
Kotlin ⇄ Java interop rules. This doc covers only the UI-specific layer.

## Verify against Vaadin 25.1, don't recall

Training data predates 25.1, so from memory you will reproduce Vaadin 8 / old-Flow patterns and miss new
features. Before using any component, API, theming token, or binding pattern, check the **Vaadin MCP
server** (`https://mcp.vaadin.com/docs`). Prefer 25.1 features (Signals, the current free component set, Aura).

- **Load Aura via `settings.addLink(Aura.STYLESHEET, …)`** in `AppShellConfigurator.configurePage()`.
  `@Theme` is gone. Use `addLink` rather than `@StyleSheet` (which runs a server-side content-hash calling
  `ServletContext.getResource`; `addLink` emits the same `href` resolved against the base URL).
- **Light/dark** via `com.vaadin.flow.component.page.ColorScheme` (annotation or `Page.setColorScheme`).

## Vaadin interop

- **Use property syntax for Vaadin getter+setter pairs** (`testId = …`, `minWidth = …`,
  `isPadding = false`). The general interop rules (platform types, `@Throws`, numeric narrowing,
  side-effecting getters) are in [kotlin-style.md](./kotlin-style.md).

## UI with Karibu-DSL

- Build component trees with Karibu builders (`verticalLayout { }`, `span/textField/select/icon(…) { }`,
  `contextMenu { item(…) { } }`, `appLayout { navbar { } }`, …). For a component with no builder,
  construct + `.apply { }`; Karibu builders still work inside `layout.apply { … }`.
- **Aura, not Lumo**, for every `ThemeVariant`-derived enum (`ButtonVariant`, `GridVariant`,
  `NotificationVariant`, …). Avoid `LUMO_*` and other Lumo-only values (for `ButtonVariant`: `CONTRAST`,
  `TERTIARY_INLINE`, `ICON`) — they won't render under Aura. `ButtonVariant`'s Aura set is `PRIMARY`,
  `TERTIARY`, `SUCCESS`, `ERROR`, `WARNING`, `SMALL`, `LARGE`. Check the styling doc's "Supported by:
  Aura" column before using any variant or utility class.
- **Axis-named alignment** (`setDefaultVerticalComponentAlignment` on a `HorizontalLayout`, etc.);
  avoid `setAlignItems`.
- **Dialogs extend `components.JvmGuardDialog`** (resizable + draggable + keepInViewport defaults). For
  the common `Cancel` + primary-confirm footer, call `confirmFooter(confirmText, confirmTestId) { … }`
  rather than hand-rolling the two buttons; it returns the confirm button (bind its enabled state if
  needed) and its primary theme also wires it to the Ctrl/Cmd+Enter shortcut.
- **A `Component` that is also a `server.ModificationListener`** registers via
  `registerModificationListener(session)` in `onAttach` (auto-unregisters on detach); do not pair
  `session.addModificationListener` / `removeModificationListener` by hand.
- **Static assets** (app stylesheet, logos, favicon) live in
  `modules/web/src/main/resources/META-INF/resources/` (served from the classpath), loaded via
  `settings.addLink("styles.css", …)` / `addFavIcon` in `AppShell` — NOT `frontend/` (that is for
  Vite-bundled Lit/TS via `@CssImport`/`@JsModule`). Global CSS reaches shadow internals via `::part(…)`
  and lives in `styles.css`.
- **Responsiveness**: wrapping toolbars (`setWidthFull()` + `isWrap = true`), flex-grow to fill rather
  than `setSizeFull` everywhere.
- **Keyboard accessibility**: every interactive element focusable + Enter/Space-activatable + visible
  focus. Use `components.textLink` for clickable text (a bare span + click is mouse-only).

## Forms & layout

Forms split cleanly — **`FormLayout` for layout, `Binder` for data** — and the discipline below keeps
it that way. **Standardize; never hand-roll a per-view form or a god-form class.**

- **Layout with `FormLayout`** (responsive columns) and the shared section/row helpers
  (`settingsSection(title, …)`, `cellRow`, …) — not ad-hoc `VerticalLayout` plumbing per view.
- **`Binder<Bean>` is the single binding / validation / dirty mechanism.** `readBean` to populate,
  `writeBeanIfValid` to commit, field validators + `binder.validate().isOk` for validity, and
  `binder.addStatusChangeListener { … binder.hasChanges() }` to drive Save-enable. **Do not** hand-wire
  populate/apply/dirty — `hasChanges()` auto-resets against the `readBean` baseline.
- **Bind with lambdas, not method references** (`{ it.fixedTransactionDays }`, `{ c, v -> … }`) — Kotlin
  Java-bean getter/`isX` pairs make `Bean::getX` references ambiguous.
- **Extract common helpers / base layers as patterns repeat.** A lifecycle base class
  (`AbstractSettingsSectionView`: login/role gate, load-on-attach, write-on-detach, Binder-driven dirty)
  and small builders (`settingsSection`, `EnumSelect`, `menuButton`) keep new editable views thin. When a
  layout/widget pattern appears 2–3×, promote it into `components/` or a base class.

## Testing (all web tests are Kotlin)

**Browserless** (the fast per-change check, no browser/servlet container) via Vaadin's built-in browserless
testing (`com.vaadin:browserless-test-junit6`). Extend **`JvmGuardBrowserlessTest`** (our base in
`modules/web/src/test/kotlin`: `BrowserlessTest` + `Locators` + `@ViewPackages("dev.jvmguard.ui")` + reified
`find<T>()`). The base creates and tears down the Vaadin env per test, so `@BeforeEach` only does
`Sessions.setCurrent(...)` (no `MockVaadin.setup`/`tearDown`).

- Query with `find<T>().single()` / `.all()` / `.exists()`; assert navigation with
  `assertInstanceOf(SomeView::class.java, getCurrentView())`; drive components through `use(component)`:
  `.click()`, `.setValue(v)`, `.select(idx)` (`Tabs`/`TabSheet`), `.selectItem(label)` (`Select`/`ComboBox`),
  `.getCellComponent(row, key)` / `.size()` (`Grid`). `navigate(View::class.java)` returns the view.
- **Simulate the user (`fromClient=true`); don't poke values.** `use(field).setValue(v)`,
  `use(button).click()`, `use(select).selectItem(label)` fire `fromClient=true` — what views gating on
  `event.isFromClient` need (`EnumSelect`, the recording/settings drafts). A plain `component.value = v` is
  `fromClient=false`: use it only for a widget the tester rejects (not attached to a UI) or a view that does
  not gate on `fromClient`. `MultiSelectComboBox.selectItem` sets the value server-side (`fromClient=false`),
  so to trip a `fromClient`-gated listener fire it yourself:
  `ComponentUtil.fireEvent(combo, AbstractField.ComponentValueChangeEvent(combo, combo, old, true))`.
- **Generic component types need a type argument:** `find<Grid<*>>()`, `find<SaveSetDialog<*, *>>()`; keep
  the `as Grid<Row>` cast (`@Suppress("UNCHECKED_CAST")`) where the element type matters.
- **Match test ids with `getTestId()`** (e.g. `find<Button>().all().first { it.testId == "…" }` or
  `findButton().withTestId("…")`), not the `data-testid` element attribute (not serialized in this mode). A
  component inside a Grid component-column is not reachable via `find<T>()`; use
  `use(grid).getCellComponent(row, key)` (give the column a `setKey`) and search the returned cell.
- Run `./gradlew :jvmguard:web:test` (the task forces `vaadin.productionMode=true`: browserless has no dev
  server, and Vaadin's dev-mode detection fails because the build file is `web.gradle.kts`, not
  `build.gradle.kts`). JUnit 6 comes from the shared `addJunit6()` helper.

**E2E** (real browser) via Playwright: `./gradlew :jvmguard:web:e2eTest` starts its **own** `ServerMain`
(from `:jvmguard:server`, integration mode) on isolated ports (8123/8948), runs Playwright, and stops it
(excluded from the normal `test`/`build`). Extend `PlaywrightE2ETest`, run each body in `onPage { … }` (a
`Page` receiver).

- **Locate widgets by test id, never display text**: `Component.setTestId(ID)` ↔ `page.getByTestId(ID)`,
  where the id is a `const val ID_*` on the production class (never a literal duplicated in the test).
  Reserve `getByText` for asserting rendered *data*. For test data, **reuse `MockServerConnectionImpl`** (via
  the `MockConnections` factory) — the same canned-data connection `server.login(..., mock=true)` returns.
  Do not write a fake `ServerConnection`.
- **Unit-test pure logic** — the browser/e2e layer can't see it. Verify stub/placeholder actions
  **structurally** (a card appears, a URL changes), not by message wording.
- Use `internal` (not `private`) for production members a test must reach.
