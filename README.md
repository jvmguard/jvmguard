# jvmguard

jvmguard is a production JVM-profiling **server**: a single, self-contained application that
continuously collects telemetry, transactions, call trees and live profiling data from the JVMs
you monitor with the jvmguard agent, and presents it through a built-in web UI.

It is licensed under the [Apache License 2.0](./LICENSE).

## Get jvmguard

The easiest way to get jvmguard is to **download the installer** for your platform from the
[download page](https://jvmguard.dev/download) and run it. The installer bundles a
compatible Java runtime, so no separate Java installation is required.

If you prefer to run jvmguard directly from source, see
[Running from source](#running-from-source) below.

## Run after installation

1. Start the **jvmguard service** (the installer creates the launcher; on Linux/macOS start it as a
   service, on Windows use the Services control panel or the installed launcher).
2. Open **http://localhost:8020/** in your browser.
3. On first launch, complete the setup wizard to create your admin login, then start monitoring
   JVMs by pointing them at the jvmguard agent port (default `8847`).

The default web port is `8020` and the agent port is `8847`. Both can be changed — see
[Configuration](#configuration).

## Running from source

To run the current development version without building an installer:

```bash
git clone https://github.com/jvmguard/jvmguard.git jvmguard
cd jvmguard
./gradlew :jvmguard:server:bootRun
```

This builds the UI and launches the server. No manual Java setup is needed — the build
auto-provisions a Java 25 toolchain via the [foojay resolver](https://github.com/gradle/foojay-toolchains).
Once you see `Server started`, open **http://localhost:8020/**.

> The build and test layout, the full development workflow, the demo cluster, the `?mock` data mode
> and all contributor conventions are documented in [AGENTS.md](./AGENTS.md).

## Configuration

The built-in defaults are shipped in
[`application.yaml`](modules/server/src/main/resources/application.yaml). To override values for an
installed instance, place an `application.yaml` in one of these locations (later entries are not read
once an earlier one exists):

- `<install-dir>/config/application.yaml` (recommended), or
- `<install-dir>/application.yaml`, or
- `<install-dir>/jvmguard.properties` (a flat `key=value` format).

Any individual key can also be set with a `-Djvmguard.<key>=<value>` system property, which always
takes precedence.

## Website and documentation

The project website is at **https://jvmguard.github.io**, with full product
documentation at **https://jvmguard.github.io/docs**.

The website lives in [`modules/website`](./modules/website) (an Astro static
site) and the documentation in [`modules/docs`](./modules/docs) (Astro
Starlight). Both are built and deployed to GitHub Pages by
[`.github/workflows/docs.yml`](./.github/workflows/docs.yml).

## License

Apache License 2.0 — see [LICENSE](./LICENSE) and [NOTICE](./NOTICE).
