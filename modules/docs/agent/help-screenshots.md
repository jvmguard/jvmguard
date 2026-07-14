# Documentation screenshots

The UI figures in the docs site (`modules/docs/public/images/ui/`) are produced by the Playwright
screenshot tests, not captured by hand. To regenerate them after a UI change:

- The screenshot tests live in `modules/ui/src/test/kotlin/dev/jvmguard/web/e2e/screenshots/`. Each writes a PNG
  named exactly `<imageName>.png` (the docs image name) into `build/.../web/e2e/screenshotsLight/`.
- `:jvmguard:web:screenshots` runs them in the light theme. `:jvmguard:web:darkScreenshots` re-runs the
  same tests with the Playwright color scheme set to dark (the app honors `prefers-color-scheme` via
  `AppShell @ColorScheme(SYSTEM)`), writing `<name>_dark.png` into `build/.../web/e2e/screenshotsDark/`.
  Both are needed: the Starlight site renders the light variant in light mode and the `_dark` variant in
  dark mode (see the `<Figure>` component in `modules/docs/src/components/Figure.astro`).
- `:jvmguard:docs:copyScreenshots` copies both sets into `modules/docs/public/images/ui/`. The committed
  images are what the docs build (and the GitHub Pages deploy) use; re-run it and commit the results
  after a UI change.
- The Astro build does not currently hard-fail on a missing figure (a broken `<Figure src>` renders an
  empty image), so cross-check new `<Figure src="ui/X.png" />` references against the generated files.
