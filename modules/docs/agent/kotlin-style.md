# Kotlin style guide

The canonical Kotlin language conventions for **all** Kotlin in jvmguard — the server-side backend
modules and the `modules/ui` UI alike. Write **idiomatic Kotlin, not Java transliterated to Kotlin**.

UI/Vaadin-specific conventions (Karibu-DSL, forms, browserless/E2E testing) live in
[web-ui-style.md](./web-ui-style.md), which extends this doc.

## Kotlin language

- `object` for singletons, `fun interface` for SAMs, `companion object` for statics/constants —
  **placed at the bottom** of the class (Kotlin idiom).
- Prefer expression bodies, `when`, `?.`/`?:`/`let`/`takeIf`, `by lazy`, and stdlib
  (`sortedBy`, `maxOfOrNull`, `mapTo`, `sumOf`, `filterIsInstance`) over manual loops.
- **`.apply { }` to configure an object** with multiple property sets / calls.
- Reach for **extension functions** for reuse. Extract once there are two real call sites.
- Always brace `if`/`for`/`while`. Import rather than use fully-qualified names. Unused lambda params
  are `_`. Descriptive names (`formComponent`, not `fc`).
- No comments unless they carry genuinely non-obvious intent; `// TODO` is the right tool for deferred
  work. No magic strings (FQNs via `X::class.java`; shared keys as `const val`).
- Expose **read-only collections**: public `val xs: List<T>` over a private `mutableListOf<T>()`.
  Exception: beans whose serialization lifecycle requires mutability (Jackson field-based config beans,
  the agent codec beans) keep `var` + a concrete mutable backing type — never `listOf()`/`List.of()`.
- **Reproduce nullability on overrides:** repeat `@NotNull`/`@Nullable` (or use the matching Kotlin
  nullable type) on every override that inherits them.

## Kotlin ⇄ Java interop

The backend modules consume Java that stays Java permanently (the agent via `:jvmguard:api` /
`:jvmguard:mbean`). Kotlin-calling-Java is friction-free; keep it that way.

- **Use property syntax for Java getter+setter pairs** (`x.foo = value`), but **call a side-effecting
  "getter" as a method** for honesty (e.g. `getAndClearModificationTypes()`).
- **Platform types** (unannotated Java returns) flow into non-null positions — don't scatter `!!`.
- **`@Throws` only when a Java caller needs the checked-exception clause** — all-Kotlin code omits it.
- **Watch numeric narrowing when porting**: Java `Math.round(double)` → `Long`, so use `roundToLong()`,
  not `roundToInt()`.
- **Spring beans use primary-constructor injection.** Kotlin classes are `final`, so any module with
  CGLIB-proxied `@Configuration`/`@Bean` classes or AOP (`@PreAuthorize` method security) must apply the
  `kotlin("plugin.spring")` (all-open) plugin, or the proxies fail at runtime (not at compile time).
- Expose a Java-friendly surface (`@JvmStatic`, `@JvmField`, `@JvmOverloads`) **only** where a permanent
  Java consumer needs it — the agent-facing API, or the `:jvmguard:integration` Java workloads. These
  are permanent, deliberate annotations, not temporary TODOs.
