# jvmguard documentation — voice & style guide

This guide is **normative** for every edit to `modules/docs/src/content/docs/**.mdx` and to
`astro.config.mjs`. It exists because the help docs have a distinct, consistent human voice, and an
unconstrained edit will drift away from it. Every section must hold that voice and pass the
[self-check](#pre-finish-self-check) at the end of this file.

Each rule is anchored to a **concrete exemplar**. A quoted line is quoted for its *voice*, not as an
endorsement of its exact wording.

The single most important rule is the [register boundary](#5-register-boundary--the-key-rule): the help
docs are operational reference, never product positioning.

---

## How to use this guide

- Read §5 first. It sets the key rule: the help docs are operational reference, never product positioning.
- Keep prose tight. Match the sentence length (±) and connector style of the surrounding copy.
- Run the [self-check](#pre-finish-self-check) before considering any section done.

---

## 1. Diction

Concrete, technical, plain. The vocabulary of someone who works on JVMs.

- **Do** use the domain terms naturally and unannounced: instrument, retransform, class loading, JVMTI,
  MBean, telemetry, transaction, call tree, hot spots, backtraces, reentry, threshold, trigger, keystore,
  heap, thread dump.
- **Do** keep the product name **lowercase**, including at the start of a sentence and in headings:
  > jvmguard consists of two main parts: the server and the agent.
  > jvmguard includes an MBean browser that shows all registered MBeans in a selected VM.
  > jvmguard cannot know what your business processes are, so configuring transactions is an important
  > part in setting up an application for monitoring.

  The only things that stay capitalized are proper nouns and acronyms: JProfiler, JVM, JFR, JVMTI, HPROF,
  MBean, MXBean, REST, HTTPS, HTTP, LDAP, SMTP, TCP, H2, Tomcat, JMX, JNDI, PKCS12.
- **Do not** use marketing or positioning vocabulary. Blocked words include, non-exhaustively:
  powerful, seamless, robust, comprehensive, cutting-edge, modern, leverage, utilize, ecosystem,
  paradigm, world-class, intelligent, next-generation, empower, streamline.
- **Do not** use intensifiers or superlatives. The docs never say "extremely low overhead" as a selling
  point — when overhead matters, they give the number or the mechanism ("less than 1%", "optimized for
  minimum overhead").

## 2. Sentence shape

Subject-first, moderate length, joined by a small set of causal connectors. The product is very often
the grammatical subject.

- **Preferred connectors:** `so`, `because`, `since`, `however`, `also`, `in that case`, `for example`,
  `note that`, `unlike`, `while`, `rather than`. Open paragraphs and asides with them freely:
  > Note that the transaction matching in jvmguard takes the first match, so more generic filters should
  > be lower in the list of transaction definitions.
  > While the `jvmguard.jar` file with the bootstrapping code is never updated, it performs a limited
  > function at startup that does not impact the monitoring functionality itself.
  > Threshold violations are not directly coupled to alerts or other actions, they just increase an
  > associated counter.

- **Explain why, briefly.** A statement is usually followed by the reason or the consequence, not left
  bare:
  > Each name can only be used by one VM at the same time. A second VM that requests to be monitored with
  > the same name will be rejected by the jvmguard server.

- **Punctuation constraints (hard rules):**
  - **No em dashes (`—`).** Use a comma, a colon, a period, or parentheses. The existing docs contain
    none. (This is also an author house style.)
  - **No semicolons.** Split into two sentences.
  - **No exclamation marks.** No emoji. No rhetorical questions.
  - Colons are fine and common, to introduce a list or an elaboration:
    > jvmguard consists of two main parts: the server and the agent.

## 3. Person and tone

- **Second person** for the reader: "you can monitor", "you have to give them names", "you will probably
  not want all URL invocations to become transactions".
- **The product as subject** for what jvmguard does: "jvmguard intercepts", "jvmguard builds a call tree",
  "jvmguard observes scalar values".
- **First person plural (`we`) only for an explicit recommendation**, used sparingly:
  > While you can use JMC to view JFR snapshots, we recommend using JProfiler, just like for CPU
  > snapshots.

- **Tone:** matter-of-fact, instructional, mildly formal but plain. No hand-holding, no cheerfulness, no
  apology. State limits and risks directly:
  > Once the JVMTI has been turned on, it cannot be turned off again.
  > Note that only the last offending VM will be profiled, not all of them.

## 4. Structure and formatting

- **Headings** are short noun phrases in sentence case, not Title Case:
  > ## Call tree and hot spots
  > ## Trigger types
  > ## Basic mechanism
  > ## Recording CPU snapshots

  One `##` per major section, `###` for subsections. The front-matter `title:` matches the heading
  convention.

- **Bold (`**…**`)** for a key term or a named sub-concept on first introduction or when enumerating:
  > Policies create the following anomalous transaction states:
  > * **Slow**: The threshold after which a transaction is classified as slow …
  > * **Very slow**: Another time-based threshold like "Slow" …

- **Double quotes** for UI labels as the user sees them: the "Add VMs" button, the "Discard" flag, the
  "Save set" button, the "Custom description" check box. (Do not bold UI labels.)

- **Inline code** for: file and directory names (`application.yaml`, `ssl/agent.ks`), property and option
  names (`vmPort`, `vmUseSsl`), commands and VM parameters (`configure -c`, `-javaagent`), class and
  annotation names (`@Telemetry`, `java.lang.Error`), bytecode/JNI signatures, and literal values. Paths
  use the platform form: `$HOME/.jvmguard/log` on Linux/Unix, `%USERPROFILE%\.jvmguard\log` on Windows.

- **`<kbd>`** for keyboard keys: <kbd>CTRL</kbd>.

- **Numbered lists** for ordered steps or ranked items, each often with a bold lead-in (see the policies
  example above). **Bulleted lists** for unordered enumerations of types, options, or notes. Keep list
  items parallel in grammar.

- **Tables** (GitHub pipe syntax) for parameter references, interval/retention specs, and format tables.
  Header row followed by `|---|---|`.

- **Figures:** place a `<Figure src="…"/>` *after* the paragraph that introduces it, never before, and
  never describe the figure redundantly in prose. UI screenshots are `ui/*.png` (a light plus a
  `_dark.png` pair); diagrams are top-level `*.svg`. Example:
  ```
  <Figure src="ui/calltree.png" />
  ```

- **Cross-links** are relative `/docs/...` paths inline in prose, link text describing the target:
  > For more information on this topic, see the chapter on [monitoring JVMs](/docs/main/monitoring).

  Never leave a link pointing at a page that does not exist.

## 5. Register boundary — the key rule

The help documentation describes **what jvmguard is, concretely, and how to use it**. It does **not**
argue what category jvmguard belongs to, what it stands for, how it compares to other products, or where
it is heading. That material belongs to `modules/docs/agent/app-mission.md` (an internal compass) and never to user
help.

- **Mission alignment happens by choosing which facts to state and how to frame a feature** — not by
  importing mission vocabulary. You correct "jvmguard is an APM that monitors your business processes" by
  restating, operationally, what jvmguard actually does. You do **not** replace it with "jvmguard is an
  automatic production profiling server" or "a capture-and-control plane".

- **Banned in help prose** (these are mission-positioning terms, not operational descriptions):
  - Self-categorization: "APM", "application performance management", "monitoring tool for business
    processes", "business transactions" used as a positioning claim (the word "transaction" as a jvmguard
    data type is fine), "dashboard".
  - Mission slogans: "automatic production profiling", "consent-first", "profiles not telemetry",
    "capture-and-control plane", "control, trust, and audit plane", "production-safe by design".
  - Strategy and roadmap: "open source and free", "success is measured in …", "the next step is …",
    "planned", "direction", "differentiator".
  - Competitive or category framing: "closest analog", "unlike Cryostat/Datadog/…", "the nearest thing
    to", "competitive landscape".
  - Numbered "principles" or manifestos.

- **The contrast, made explicit.** This is the register we are keeping:

  > jvmguard observes scalar values from four different types of sources:
  > …
  > Many of these telemetries are produced by probes.

  This is the register we are **not** introducing:

  > jvmguard is an open-source server for automatic production profiling: it captures deep, JVM-level
  > profiling artifacts from running production applications automatically, only when something warrants
  > it …

  The first describes a mechanism the reader will operate. The second argues a thesis. Help docs do only
  the first.

- **State each feature accurately and operationally**, then stop. Do not argue category, mission, or
  comparison in help prose (that is the boundary above).

## 6. Editorial discipline

Write tight, factual prose and let the voice above carry it.

- **Say it once, plainly.** State the fact and, briefly, the reason or the consequence. Do not pad a
  12-word point into a 30-word one.
- **Do not editorialize.** No flourish, no positioning, no restating for "flow" or "clarity" something
  that is already clear. If a sentence is accurate and on-voice, leave it.
- **Every sentence must be verifiable against the product.** Document only what jvmguard actually does.
  Do not describe a capability, screen, or option that does not exist.
- **When you trim a page, trim it cleanly.** Drop the sentences and the figure, remove the heading if a
  whole subsection goes, and fix any inbound cross-links. Do not leave dangling references or a one-line
  placeholder.

---

## 7. Exemplars (positive) and anti-patterns (negative)

### Positive — keep this voice

- Subject-first, product-as-subject, causal:
  > jvmguard builds a call tree from all recorded transactions. A call tree is a cumulated data structure
  > that captures all the different sequences in which transactions are nested.

- Matter-of-fact limit:
  > Once the JVMTI has been turned on, it cannot be turned off again. As long as the JVM is running, it
  > will remain in this state. Typically, the overhead of the JVMTI without any data recording is less
  > than 1%, though.

- Settings behavior, plain:
  > As long as you do not change anything, you can simply leave a settings view again. If you made a
  > change, Apply and Discard buttons appear. No changes to the configuration have any effect before you
  > apply them.

- "Note that" aside:
  > Note that the transaction matching in jvmguard takes the first match, so more generic filters should
  > be lower in the list of transaction definitions.

### Negative — do not write like this

These are drawn from `modules/docs/agent/app-mission.md`. Each is wrong for a help page, not because it is badly
written, but because it is the wrong *kind* of writing.

- **Colon-stacked abstract positioning** — do not introduce jvmguard this way:
  > ~~jvmguard is an open-source server for automatic production profiling: it captures deep, JVM-level
  > profiling artifacts from running production applications automatically …~~

- **Numbered principles / manifesto** — never in help:
  > ~~1. Consent-first, minimal by default. … 2. Profiles, not telemetry. …~~

- **Category and competitive framing** — never in help:
  > ~~Automatic Production Profiling is a different category from Application Performance Monitoring.~~
  > ~~Closest analog: Cryostat (Red Hat, OSS).~~

- **Strategic / roadmap register** — never in help:
  > ~~Success is measured in adoption, trust, and ecosystem, not revenue.~~
  > ~~The next step is to make jvmguard drivable by AI agents …~~

- **Em dashes and semicolons** — not used in this corpus. Use a comma, colon, period, or parentheses.

---

## Pre-finish self-check

Run this before marking any section done. Every box must hold.

- [ ] "jvmguard" is lowercase everywhere, including sentence-initial and in headings.
- [ ] No em dashes, no semicolons, no exclamation marks, no emoji, no rhetorical questions.
- [ ] No marketing/positioning vocabulary (powerful, seamless, leverage, robust, comprehensive, …).
- [ ] No mission/positioning register (APM-as-self-description, "automatic production profiling",
      "consent-first", "capture-and-control plane", category/competitive/roadmap language).
- [ ] The docs describe only real jvmguard features. A dashboard, probes, method sampling, memory
      snapshots, license keys, and EJB/Spring/RMI/web transaction types are not part of jvmguard and are
      not documented. "business transaction" (as positioning) and "APM" (as self-description) do not appear.
- [ ] Sentences are subject-first where natural; the product is the subject for what jvmguard does.
- [ ] UI labels are in double quotes; key terms are bold on first use; files, properties, commands, and
      code are in inline code; keys are in `<kbd>`.
- [ ] Every correct original sentence was kept verbatim; only stale, wrong, or misframed clauses were
      changed, and each changed sentence matches the original length and connector style.
- [ ] Headings are short noun phrases in sentence case.
- [ ] Figures sit after their introducing paragraph, and no figure describes a screen that does not exist.
- [ ] Cross-links are relative `/docs/...` and point only at pages that exist.
