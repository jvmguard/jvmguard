# Automatic Production Profiling

## Mission

jvmguard is an open-source server for **automatic production profiling**: it captures deep, JVM-level
profiling artifacts from running production applications automatically, only when something warrants
it, and without collecting application data by default.

## The category

Automatic Production Profiling is a different category from Application Performance Monitoring.

- **APM** continuously instruments frameworks and ships spans, metrics, and traces to a vendor. The
  steady state is "always collecting."
- **Automatic production profiling** collects no application data in the steady state. It watches lightweight JVM-level signals,
  and when an explicitly configured condition is met it takes a single targeted deep capture: a JFR
  recording, a heap dump, or a thread dump. The capture is a *profile*, not your business or trace
  data.

jvmguard does not try to be an APM and does not compete with full APM tools. It occupies the space
those tools leave empty: getting profiler-grade detail out of production, safely and on demand.

## Principles

1. **Consent-first, minimal by default.** jvmguard does not gather application or end-user data and
   does not phone home. Deep profiling artifacts are captured only when a trigger the operator
   configured fires.
2. **Profiles, not telemetry.** What we capture is JVM-level profiling data: CPU and allocation via
   JFR, heap dumps, thread dumps, MBean state, and call-tree/transaction data from explicitly defined
   transaction patterns. We do not auto-instrument frameworks.
3. **Automatic and targeted.** Value comes from trigger / threshold / policy-driven captures at the
   moment of trouble, not from continuous dashboards. jvmguard has no dashboard by design: with no
   default collection, there is nothing to chart.
4. **Production-safe.** Capture is the exception, not the norm. Overhead and blast radius stay bounded,
   and what may be captured (and when, and at what cost) is explicit and inspectable.
5. **Open source and free.** Success is measured in adoption, trust, and ecosystem, not revenue. A
   small, legible codebase and a privacy-respecting default are strategic assets.
6. **Captures feed deep analysis.** The artifacts jvmguard produces (for example JProfiler-openable
   snapshots) are meant to be analyzed in dedicated tools. jvmguard is the capture-and-control layer,
   not the analyzer.

## The role of the UI

The web UI is a **control, trust, and audit plane**, not an analysis tool. It exists to configure what
may be captured and under which conditions, to review what was captured (the inbox and the event log),
and to make the consent and overhead story legible. Deep analysis happens elsewhere.

## Direction: agentic integration

jvmguard is drivable by AI agents through an **MCP server** that exposes its read and capture
operations: discover the fleet, read telemetries, transactions, and MBeans, and start a JFR
recording, a heap dump, or a thread dump. The MCP server builds on the **REST API** and ships now. It
makes the full loop possible:

> detect a problem -> capture the right profile -> analyze it -> recommend or take the next action

with a human supervising rather than operating it. The remaining 1.0 work is the **guardrails** (scope
opt-in, rate and overhead limits) and **auditing** of every operation an agent performs that make
agent-driven capture production-safe.

## What jvmguard is not

- Not an APM, tracing, or metrics pipeline.
- Not a framework or transaction-as-trace tracker.
- Not a dashboard product.
- Not a data collector: it does not aggregate or transmit application data by default.
