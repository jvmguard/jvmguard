import {defineConfig} from 'astro/config';
import sitemap from '@astrojs/sitemap';

import {SITE} from './src/consts';

// Dev-only: redirect /docs requests to the docs dev server (port 4322).
// In production, the CI workflow combines both builds into one artifact
// and no redirect is needed.
function docsRedirect() {
  return {
    name: 'docs-redirect',
    configureServer(server) {
      // Use a catch-all that checks the path, so req.url keeps the full path.
      server.middlewares.use((req, res, next) => {
        if (req.url?.startsWith('/docs')) {
          res.writeHead(302, {Location: `http://localhost:4322${req.url}`});
          res.end();
        } else {
          next();
        }
      });
    },
  };
}

export default defineConfig({
  site: SITE.canonical,
  vite: {
    plugins: [docsRedirect()],
  },
  integrations: [sitemap()],
});
