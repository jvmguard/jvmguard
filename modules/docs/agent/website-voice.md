# jvmguard website — voice & style guide

This guide is **normative** for every piece of public marketing copy on the jvmguard website: the
landing page (`/`), `/security`, `/download`, `/compare`, and any future top-level pages. It is the
website counterpart to [docs-voice.md](./docs-voice.md), which stays normative for the help documentation under
`src/content/docs/**.mdx`.

It exists because the website speaks in a different register from the help docs, and an unconstrained
edit will drift in one of two directions: toward the docs' deliberately narrow operational voice (which
undersells the product on a marketing page), or toward adjective inflation (which undercuts the
enterprise-trust story). Every page must read as if one author wrote it, and must pass the
[self-check](#pre-finish-self-check) at the end of this file.

These rules draw on the internal mission (`modules/docs/agent/app-mission.md`). Where a rule quotes a line, it is
quoted for *voice*.

---

## How to use this guide

- Read §1 first. It sets the single defining difference between website copy and help copy.
- Read §7 before writing any feature sentence. It is the content-correctness gate: the capabilities
  that are not part of jvmguard and must never appear on the site.
- When you change prose, match the sentence length (±) and connector style of the surrounding copy.
- Run the [self-check](#pre-finish-self-check) before considering any page done.

---

## 1. The key rule — inverted register boundary

The help docs describe **what jvmguard is, concretely, and how to use it**, and deliberately refuse to
argue category, mission, or roadmap (see docs-voice.md §5). The **website does the opposite**: its job is to
position jvmguard, state the category it invents, name what it is not, and explain where it is heading.

- **Mission, category, competitive, and roadmap language is allowed and expected on the website.** This
  is the single register the docs forbid that the site requires. The mission vocabulary in §2 exists to
  be used here.
- **The contrast, made explicit.** This is the register the website *does* use:
  > jvmguard watches your production JVMs with low-overhead telemetries and the transactions you
  > define. It takes a deep capture only when a trigger you configured fires.

  This is the register the website shares with the docs (plain, concrete, no inflation):
  > The jvmguard agent instruments selected methods through class loading. It loads no native libraries
  > unless you explicitly enable the profiling cross-over.

- **Relationship to `modules/docs/agent/app-mission.md`.** The mission file is the agent-internal source-of-truth for
  positioning. Website copy is its **public rewrite**: every positioning claim on the site must be
  consistent with the mission, but rewritten for a reader who is evaluating whether to deploy jvmguard,
  not for an internal reader tracking strategy. **Never paste mission sections onto the site.** The
  mission's numbered principles, its "Direction" roadmap notes, and its "Competitive landscape" are
  source material to rewrite, not blocks to copy.
- **One register the website still refuses: inflated marketing.** Positioning without proof reads as
  sales copy and destroys the enterprise-trust goal (see §9). State the claim, then the mechanism.

---

## 2. Approved mission vocabulary

These terms are **banned in the help docs** (docs-voice.md §5) but **expected on the website**. Use them
where they carry weight, not on every line:

- **Automatic Production Profiling / APP** — the category jvmguard invents. Use in the hero and on
  `/compare`.
- **capture-and-control** (plane) — what jvmguard is, as distinct from an analyzer or an APM.
- **consent-first, minimal by default** — the data posture. Pairs with the `/security` page.
- **profiles, not telemetry** — what is captured versus what an APM ships.
- **production-safe** — the overhead and blast-radius stance.
- **control, trust, and audit** — the role of the UI (configure what may be captured, review what was
  captured, make the consent story legible).
- Category and competitive framing: "not an APM", "a different category from Application Performance
  Monitoring", named comparisons to Cryostat, continuous profilers, and the JFR+jcmd baseline. Factual,
  never defensible (see §8).
- Roadmap framing: "planned", "direction", "intended differentiator". Always labeled as not-yet-built
  when that is the case (the agentic / MCP direction is planned for 1.0).

---

## 3. Diction

Concrete, technical, plain. The vocabulary of someone who runs JVMs in production.

- **Do** use the domain terms naturally and unannounced: agent, JVMTI, class loading, instrumentation,
  JFR, heap dump, thread dump, MBean, trigger, threshold, transaction, call tree, hot spots, backtraces,
  VM group, retention, HPROF, JMX.
- **Do** keep the product name **lowercase** everywhere, including at the start of a sentence and in
  headings:
  > jvmguard is designed to monitor multiple VMs at the same time.
  > ## Cross-over to profiling

  Capitalize only proper nouns and acronyms: JProfiler, JVM, JFR, JVMTI, HPROF, MBean, MXBean, REST,
  HTTPS, HTTP, LDAP, TCP, H2, Tomcat, JMX, JNDI, Apache.
- **Do** give the number or the mechanism when a claim depends on it. "Less than 1% overhead" and "no
  outbound connections by default" are website voice. "Near-zero overhead" is accurate and allowed for
  the steady state (it matches the mission); "extremely low overhead" as a free-floating intensifier is not.
- **Do not** use marketing or positioning filler. Blocked words include, non-exhaustively: powerful,
  seamless, robust, comprehensive, cutting-edge, modern (as a free-floating adjective), leverage,
  utilize, ecosystem, paradigm, world-class, intelligent, next-generation, empower, streamline. See §4
  for more adjectives to avoid.

---

## 4. Adjective inflation — what to avoid

A confident, benefit-led voice is right for the site (see §6). Adjective inflation is not. Cut inflated
phrasing and replace it with the concrete claim:

- ~~powerful JVM monitoring solution~~ → say what it does (watches JVMs, captures on trigger).
- ~~maximum insight with minimum overhead~~ → state the overhead or the mechanism.
- ~~ultra-flexible business transactions~~ → name the configuration options.
- ~~first-class citizens~~ / ~~top-notch~~ / ~~premier tool~~ / ~~industry's most advanced~~ → drop the
  rank and state the capability.
- ~~excellent support for Spring and JEE~~ → name what is supported and how.
- Filler openers such as ~~"Defense in depth is a strategy that applies to many situations in life"~~ →
  delete. Start with the feature.

The test: if you remove the adjective and the sentence still says something true and specific, the
adjective was inflation. If the sentence then says nothing, the claim was the adjective, and you need to
find the mechanism underneath before publishing.

---

## 5. Inherited hard rules

These are inherited verbatim from docs-voice.md for brand consistency across the site and the docs. Every
page must obey them.

- **No em dashes (`—`).** Use a comma, a colon, a period, or parentheses. (Author house style.)
- **No semicolons.** Split into two sentences.
- **No exclamation marks. No emoji.** A rhetorical question is allowed, used sparingly as a hero
  hook (the help docs forbid rhetorical questions; the website does not).
- **Headings** are short noun phrases in sentence case, not Title Case:
  > ## Cross-over to profiling
  > ## What the agent does and does not collect

- **Bold (`**…**`)** for a key term or named sub-concept on first introduction or when enumerating.
- **Inline code** for file and directory names, property and option names, commands and VM parameters
  (`application.yaml`, `-javaagent`, `@Telemetry`), and literal values.
- **Double quotes** for UI labels as the user sees them: the "Add VMs" button, the "Save set" button.
- Cross-links to the docs are absolute `/docs/...` paths. Cross-links between site pages are relative.

---

## 6. Sentence shape, tone, and the headline pattern

Use a **benefit-led headline followed by a short, concrete explanation**, with no inflation underneath
it (§4).

- **Headline:** a short noun phrase naming the benefit or the capability, sentence case.
- **Blurb:** one or two sentences. Benefit first, mechanism second. No rank adjectives.

  Positive:
  > **React to off-nominal conditions.** When a configured condition is met, jvmguard fires a trigger and
  > runs its actions: record JFR, take a heap dump, take a thread dump, write to the inbox, send an
  > email, call a webhook. You compose the response in the UI.

  > **Cross-over to profiling.** When a trigger fires, jvmguard can load the JProfiler agent and produce a
  > snapshot for deep analysis. The capture lands in your inbox. You open it in JProfiler.

  > **No JMX port required.** The agent reads MBeans in-process. You inspect and operate on MBeans
  > without exposing a JMX connector server to the network.

- **Person.** Second person for the reader ("you can monitor", "you compose the response"). The product
  as subject for what jvmguard does ("jvmguard watches", "jvmguard fires a trigger", "the agent reads
  MBeans"). First person plural (`we`) only for an explicit recommendation, used sparingly.
- **Tone.** Confident, matter-of-fact, plain. State limits and risks directly. The enterprise reader
  should finish a paragraph knowing exactly what the agent will and will not do in their JVM.
- **Preferred connectors:** `so`, `because`, `since`, `however`, `also`, `in that case`, `for example`,
  `note that`, `unlike`, `while`, `rather than`.

---

## 7. Feature scope — content correctness

This is the highest-risk section. Describe only what jvmguard does. The capabilities below are **not part
of jvmguard** and must never appear on the site, even obliquely.

**Not part of jvmguard. Do not mention:**
- A **dashboard** or dashboard views.
- **Dashboard alerts** or an alert-timeline view. (Trigger notifications go out via email, webhook, the
  inbox, and the event log. That is the story to tell.)
- **End-user experience monitoring (EUEM)**, browser page-load measurement, or JavaScript injection into
  HTML pages.
- **Probes**: JDBC, JPA/Hibernate, MongoDB, Cassandra, HBase, JMS, JNDI probes, probe hot spots, and
  probe-based telemetries. Do not use "database performance and bottlenecks" or "database operations"
  framing that implies probes.
- A **method-sampling** subsystem (sampling data views or sampling configuration).
- Steady-state **memory monitoring** or a memory (HPROF) data view.
- A **call graph**: cross-VM remote calls, remote origins, or a transaction graph.
- **Historical comparisons / diff mode.**
- A **threshold-violations data view.** (Thresholds are trigger inputs, not a data view.)
- **Auto-detected Web / EJB / Spring / RMI transactions.**

**In scope, with a nuance you must get right:**
- **Transactions** are **POJO, DevOps, and custom-annotation** types only. The site may describe
  transactions, the call tree, and hot spots. It must **not** claim jvmguard "automatically detects"
  servlet, EJB, Spring, RMI, or web-service transactions. Transactions are explicitly configured.
- **Heap dumps** are a trigger action (`HEAP_DUMP`), a capture-on-trigger. Steady-state memory views are
  not in scope.
- **The JProfiler JVMTI cross-over** is a capture that produces a JProfiler snapshot for handoff. Frame
  it as a capture-and-handoff, never as analysis inside jvmguard.
- **Trigger actions the UI produces:** `RECORD_DATA`, `RECORD_JFR`, `THREAD_DUMP`, `HEAP_DUMP`, `EMAIL`,
  `WEBHOOK`, `LOG`, `INBOX`.
- **Telemetries** come from VM, MBean, and DevOps sources.

Keep every sentence on what jvmguard does. Do not describe a capability it does not have.

---

## 8. The funnel framing — JProfiler handoff

The OSS site is also the top of the JProfiler funnel, and the framing must be **openness first** so it
reads as a feature, not as lock-in.

- **Captures are standard formats.** JFR recordings and HPROF heap dumps are open. State that they open
  in JProfiler, JMC, and Eclipse MAT. Lead with the openness, then note JProfiler as the deepest
  analyzer.
- **JProfiler is the complement, not the requirement.** "Analyze in JProfiler, JMC, or MAT" is the
  website voice. "Requires JProfiler" is not.
- **The agentic / MCP direction is planned for 1.0.** Label it as a 1.0 feature, not as "not shipped".
  Do not describe it as a current capability. State the loop (detect, capture, analyze, recommend) as
  the 1.0 direction with guardrails and auditing, and stop. Overclaiming a roadmap item destroys
  enterprise trust faster than the gap itself.
- **Comparisons are factual, never defensible.** On `/compare`, one concrete paragraph each for Cryostat,
  the JFR+jcmd baseline, and continuous profilers. Name the real difference (K8s-only versus not,
  JFR-only versus heap/thread/MBean, target-matched versus threshold-driven, continuous collection
  versus capture-on-trigger). No superlatives about either side.

---

## 9. Audience calibration

The reader is an **enterprise backend, SRE, platform, or security-adjacent engineer** deciding whether
jvmguard can go into production and survive a security review. Calibrate to that reader.

- **The /security page is the procurement-bypass asset.** It must answer: what does the agent do in my
  JVM, what does it collect in the steady state (only the lightweight telemetries and transactions you
  enabled), what does it send where (agent to server, no phone home, no cloud egress), where does data rest (a single H2 file on your host), how is access controlled
  (Spring Security, LDAP, three-tier RBAC), how is transport secured (TLS agent to server, HTTPS UI),
  and under what license (Apache 2.0, source-available, self-hosted, air-gapped-friendly). Concrete and
  auditable beats adjective-heavy every time.
- **Enterprise, not indie.** The visual and verbal register signals a serious instrument made by the
  JProfiler team, not a hobbyist tool. Avoid slang, meme references, and the generic dev-tool SaaS
  template (centered hero with gradient text, three-card outline-icon feature grid, "trusted by" logo
  strip, fake dashboard in a browser frame).
- **Lead with what it does not do.** For this reader, "gathers no application or end-user data" and
  "loads no native libraries unless you opt in" are the strongest opening claims. They are also true
  and verifiable. Do not claim jvmguard collects nothing: the steady state records the telemetries and
  transactions you configured. The accurate distinction is lightweight always-on data versus deep
  artifacts captured only on a trigger.

---

## 10. Exemplars (positive) and anti-patterns (negative)

### Positive — keep this voice

- Benefit headline plus concrete mechanism:
  > **Pure Java agent by default.** The jvmguard agent instruments selected methods through class loading.
  > It loads no native libraries unless you explicitly enable the profiling cross-over.

- Mission positioning, website register (this is allowed here, banned in the docs):
  > jvmguard is a production profiling server. It watches your JVMs with low-overhead telemetries and the
  > transactions you define, and captures a deep profile only when a trigger fires.

- Openness-first funnel framing:
  > Captures are standard JFR and HPROF files. Open them in JProfiler for the deepest analysis, or in
  > JMC and Eclipse MAT.

- Matter-of-fact limit:
  > Thresholds do not fire actions directly. They feed triggers, and the triggers run the actions.

### Negative — do not write like this

- **Adjective inflation**:
  > ~~jvmguard is a powerful JVM monitoring solution that gives you maximum insight with minimum
  > overhead.~~

- **Pasting the internal mission manifesto** onto a public page:
  > ~~1. Consent-first, minimal by default. 2. Profiles, not telemetry. 3. Automatic and targeted.~~

  Rewrite the principle for the reader instead (see the second positive example above).

- **A feature jvmguard does not have, described as live**:
  > ~~jvmguard includes a dashboard that shows you if everything is OK.~~
  > ~~Database operations like JDBC statements are first-class citizens in jvmguard.~~

- **Em dashes and semicolons** — not used. Use a comma, colon, period, or parentheses.

- **Overclaiming the roadmap**:
  > ~~jvmguard drives the full detect-capture-analyze loop with AI agents.~~

  State it as planned instead.

---

## Pre-finish self-check

Run this before marking any page done. Every box must hold.

- [ ] "jvmguard" is lowercase everywhere, including sentence-initial and in headings.
- [ ] No em dashes, no semicolons, no exclamation marks, no emoji. A rhetorical question, if any,
      is a single hero hook, not used in body copy.
- [ ] No marketing filler (powerful, seamless, robust, comprehensive, modern-as-adjective, leverage,
      utilize, ultra-flexible, top-notch, premier, industry's most advanced, first-class, maximum/
      minimum-as-intensifiers, filler openers).
- [ ] Mission vocabulary (APP, capture-and-control, consent-first, profiles-not-telemetry,
      production-safe) is used where it carries weight, not pasted as a numbered manifesto.
- [ ] No out-of-scope capability appears: dashboard, dashboard alerts, EUEM, any probe, method sampling,
      memory monitoring / memory views, call graph / cross-VM, historical comparisons, threshold-
      violations view, auto-detected web/EJB/Spring/RMI transactions.
- [ ] Transactions are described as POJO / DevOps / custom-annotation only; heap dumps as a trigger
      action only; the JVMTI cross-over as a capture-and-handoff only.
- [ ] Trigger actions referenced are only `RECORD_DATA`, `RECORD_JFR`, `THREAD_DUMP`, `HEAP_DUMP`,
      `EMAIL`, `WEBHOOK`, `LOG`, `INBOX`.
- [ ] The steady state is described accurately: lightweight telemetries and the transactions you
      configured, with low overhead. Never claim jvmguard "collects nothing". Deep artifacts are the
      exception, captured only on a trigger.
- [ ] The JProfiler handoff is openness-first (JFR/HPROF open in JProfiler, JMC, MAT); MCP/agentic is
      labeled as a 1.0 feature, never as "not shipped".
- [ ] Any comparison (Cryostat, JFR+jcmd, continuous profilers) is factual, with no superlatives on
      either side.
- [ ] Sentences are subject-first where natural; the product is the subject for what jvmguard does;
      benefit headlines are followed by a concrete mechanism.
- [ ] Headings are short noun phrases in sentence case; UI labels are in double quotes; files,
      properties, commands, and code are in inline code.
- [ ] Positioning claims are consistent with `modules/docs/agent/app-mission.md`, but rewritten for a deploying reader
      rather than copied verbatim.
