/**
 * Transformer untuk file .js/.mjs di node_modules yang berformat ESM.
 * SWC mengubah import/export → require, tetapi `import.meta` tidak punya padanan langsung di CommonJS.
 * Kita ganti dengan padanan CommonJS yang setia:
 *   import.meta.url → file:// URL dari __filename (sama seperti di ESM asli)
 *   import.meta.hot / .env / lainnya → undefined (tidak ada Vite/HMR saat test)
 */
const { createTransformer } = require("@swc/jest");

const swc = createTransformer({
  jsc: { parser: { syntax: "ecmascript" }, target: "es2022" },
  module: { type: "commonjs" },
});

const IMPORT_META_CJS =
  '({ url: require("node:url").pathToFileURL(__filename).href })';
const shimImportMeta = (src) =>
  src.replace(/\bimport\.meta\b/g, IMPORT_META_CJS);

module.exports = {
  canInstrument: swc.canInstrument,
  process: (src, filename, options) =>
    swc.process(shimImportMeta(src), filename, options),
  processAsync: (src, filename, options) =>
    swc.processAsync(shimImportMeta(src), filename, options),
  getCacheKey: (src, filename, options) =>
    swc.getCacheKey(shimImportMeta(src), filename, options),
};
