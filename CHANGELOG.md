# Changelog


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

