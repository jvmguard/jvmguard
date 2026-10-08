# Changelog


### 0.3

**New features:**

- New "OTel" transaction type: methods annotated with OpenTelemetry's `@WithSpan` or Micrometer's
  `@Observed` are recorded as transactions, named after the span name in the annotation. A
  preconfigured, fully editable OTel transaction definition exists in the transaction recording
  settings, with an optional span name filter.
- Mapped transaction definitions can now filter by annotation attribute values and use annotation
  attribute values as transaction naming elements.
- Captures (heap dumps, thread dumps, JFR snapshots and JProfiler recordings) are now emitted as
  OpenTelemetry log records into the monitored application's OTel pipeline when the OTel API is on
  its classpath. Trigger-fired captures carry the trigger description, the transaction and the
  policy event type that fired the trigger. The emission can be switched off in the trigger
  recording settings of the root group.

**Fixes:**

- When saving a dialog fails because of an invalid value on another tab, the dialog now switches
  to the tab with the invalid field.

### 0.2

**New features:**

- The web UI, documentation and website are now localized into Korean, Japanese and Simplified
  Chinese. The UI language is auto-detected from the browser and can be changed with the language
  selector in the header.
- Added a redaction option for heap dumps and JFR recordings.
- Login security hardening: the password is always validated before the TOTP code, failed logins
  are throttled with exponential backoff, and password hashes are upgraded automatically on login.
- TOTP secrets are now stored AES/GCM-encrypted.
- The REST API now applies the same group scoping as the web UI.

**Fixes:**

- Fixed a wildcard overlap bug for transaction filters (`ab*bc` wrongly matched "abc").
- Fixed duplicate class registration when two threads transform classes with a shared ancestor.
- Fixed serialVersionUID pinning for instrumented classes when a synthetic clinit is added.
- Fixed an LDAP connection leak after authentication.
- The log and VMs views no longer reload when no new data is available.

### 0.1

Initial release

