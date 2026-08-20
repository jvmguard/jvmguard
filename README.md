# jvmguard

**[jvmguard.dev](https://jvmguard.dev)** · [Documentation](https://jvmguard.dev/docs/main/introduction) · [Javadoc](https://jvmguard.dev/javadoc) · [Download](https://jvmguard.dev/download)

A JVM monitoring and profiling server with a built-in web UI. Connect JVMs via the
jvmguard agent, track telemetry and transactions, and capture deep profiles, either manually,
on threshold and policy triggers, or with a coding agent via the MCP server.
Every capture is access-controlled and audited.

<picture>
  <source srcset="https://raw.githubusercontent.com/jvmguard/jvmguard/main/modules/website/public/images/ui/profiling_options_dark.png" media="(prefers-color-scheme: dark)">
  <img src="https://raw.githubusercontent.com/jvmguard/jvmguard/main/modules/website/public/images/ui/profiling_options.png" alt="The jvmguard web UI showing a fleet of JVMs with live telemetry and the snapshot recording dialog.">
</picture>

## Get started

Download the installer for your platform from **[jvmguard.dev/download](https://jvmguard.dev/download)**,
or run from source:

```bash
git clone https://github.com/jvmguard/jvmguard.git
cd jvmguard
./gradlew :server:bootRun
```

The build auto-provisions a Java 25 toolchain. Once you see `Server started`, open
**http://localhost:8020/** and complete the setup. Use the "Add VMs" button in the
header of the web UI to start monitoring.

## Localization

The web UI, documentation and website are localized into **Korean**, **Japanese** and
**Simplified Chinese**. 

<picture>
  <source srcset="https://raw.githubusercontent.com/jvmguard/jvmguard/main/modules/website/public/images/ui/method_parameter_config_zh-CN_dark.png" media="(prefers-color-scheme: dark)">
  <img src="https://raw.githubusercontent.com/jvmguard/jvmguard/main/modules/website/public/images/ui/method_parameter_config_zh-CN.png" alt="The jvmguard web UI in Simplified Chinese, configuring transaction naming from a method parameter.">
</picture>

## Documentation

Full documentation, including installation, configuration, triggers, and the REST/MCP APIs,
lives at **[jvmguard.dev/docs](https://jvmguard.dev/docs/main/introduction)**. The API for Declared
transactions and telemetries is documented in the **[Javadoc](https://jvmguard.dev/javadoc)**.

## Technologies

- **Server**: Spring Boot (embedded Tomcat) on Java 25, Spring Security, H2 with HikariCP and Flyway
- **Web UI**: Vaadin 25 (Flow) with Kotlin and Karibu-DSL
- **Agent**: Java agent with ASM bytecode instrumentation, compatible with Java 8+
- **Build**: Gradle with Kotlin DSL, auto-provisioned JDK toolchains
- **Testing**: JUnit, browserless Vaadin tests, Playwright e2e tests
- **Distribution**: install4j installers, Astro Starlight documentation site

## License

[Apache License 2.0](./LICENSE)
