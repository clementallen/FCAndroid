import { defineConfig } from 'vite';
import { fileURLToPath } from 'node:url';

// The TeaVM build writes here. Aliasing it keeps the import in source code
// readable and means only this file knows where Gradle puts things.
const engine = fileURLToPath(
  new URL('../web/build/generated/teavm/js/js/flightclub.js', import.meta.url),
);

export default defineConfig({
  resolve: {
    alias: { '@engine': engine },
  },
  build: {
    outDir: '../dist',
    emptyOutDir: true,
    // esbuild's minifier mangles the labelled statements TeaVM generates for
    // its control flow - the bundle loads and immediately throws "Undefined
    // label". terser handles them correctly, for the same output size.
    minify: 'terser',
    // The engine bundle is large and already optimised by TeaVM; there is
    // nothing useful to inline and plenty to keep cacheable on its own.
    chunkSizeWarningLimit: 4096,
  },
  server: { fs: { allow: ['..'] } },
});
