# Documentation screenshots

The UI figures in the docs site (`modules/docs/public/images/ui/`) are produced by the Playwright
screenshot tests, not captured by hand. To regenerate them after a UI change:

- The screenshot tests live in `modules/ui/src/test/kotlin/dev/jvmguard/ui/e2e/screenshots/`. Each writes a PNG
  named exactly `<imageName>.png` (the docs image name) into `build/gradle/ui/e2e/screenshotsLight/<locale>/`.
- `:ui:screenshots` runs them in the light theme. `:ui:darkScreenshots` re-runs the
  same tests with the Playwright color scheme set to dark (the app honors `prefers-color-scheme` via
  `AppShell @ColorScheme(SYSTEM)`), writing `<name>_dark.png` into `build/gradle/ui/e2e/screenshotsDark/<locale>/`.
  Both are needed: the Starlight site renders the light variant in light mode and the `_dark` variant in
  dark mode (see the `<Figure>` component in `modules/docs/src/components/Figure.astro`).
- **Locales.** `-Pjvmguard.screenshots.locale=<ko|ja|zh-CN>` captures the UI in that language (the browser
  context locale drives the UI's auto-detection; default is `en`). UI-label locators in the tests resolve
  through `l("<bundle key>")` (`ScreenshotTest`), so the same tests drive every locale; data names
  (MBeans, VM groups, hosts) stay English literals. When you add a screenshot test, never locate by a
  hardcoded UI label.
- `:docs:copyScreenshots` assembles `public/images/ui/`: the English output goes to the flat directory
  (committed — this is the review baseline; re-run the English tasks and this copy after a UI change,
  review the image diff in IDEA, and commit), the localized output goes to
  `public/images/ui/generated/<locale>/` (gitignored, never committed). The website gets its two figures
  the same way via `:website:copyScreenshots`. Locale directory names are BCP-47 (`zh-CN`, not the
  Starlight config key `zh-cn`); `Figure.astro` resolves them through `starlightRoute.lang`.
- **The deploy workflow regenerates ALL locales (including English) in CI** (the `screenshots` matrix job
  in `docs-deploy.yml`), so the published figures are always fresh; the committed English images are only
  the local review baseline and what local docs builds use. The runner images ship no CJK fonts, so that
  job installs `fonts-noto-cjk` first — without it the ko/ja/zh-CN captures render tofu.
- The Astro build does not currently hard-fail on a missing figure (a broken `<Figure src>` renders an
  empty image), so cross-check new `<Figure src="ui/X.png" />` references against the generated files.
