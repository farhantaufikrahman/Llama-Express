// Zona waktu tetap → hasil test format tanggal sama di laptop (WIB) dan di Jenkins (biasanya UTC)
process.env.TZ = "Asia/Jakarta";

const swcTs = [
  "@swc/jest",
  {
    jsc: {
      parser: { syntax: "typescript", tsx: true },
      transform: { react: { runtime: "automatic" } },
      target: "es2022",
    },
    module: { type: "commonjs" },
  },
];
const swcJs = [
  "@swc/jest",
  {
    jsc: { parser: { syntax: "ecmascript" }, target: "es2022" },
    module: { type: "commonjs" },
  },
];

/**
 * Paket node_modules yang hanya rilis ESM → harus ditransform SWC ke CommonJS.
 * Jika muncul "Must use import to load ES Module: ...node_modules/<nama>/...",
 * cukup tambahkan <nama> ke daftar ini.
 */
const esmPackages = [
  "react-router",
  "@remix-run",
  "msw",
  "@msw",
  "@mswjs",
  "@open-draft",
  "@bundled-es-modules",
  "until-async",
  "rettime",
  "outvariant",
  "strict-event-emitter",
  "headers-polyfill",
  "is-node-process",
  "path-to-regexp",
  "cookie",
  "tough-cookie",
  "tldts",
  "tldts-core",
  "graphql",
];

/** @type {import('jest').Config} */
export default {
  testEnvironment: "jest-fixed-jsdom",
  roots: ["<rootDir>/src"],
  setupFiles: ["<rootDir>/jest.polyfills.ts"],
  setupFilesAfterEnv: ["<rootDir>/jest.setup.ts"],
  transform: {
    "^.+\\.(ts|tsx)$": swcTs,
    "^.+\\.(js|mjs|cjs)$": "<rootDir>/jest.esm-transformer.cjs",
  },
  // Abaikan node_modules KECUALI paket ESM di atas (pola "negative lookahead")
  transformIgnorePatterns: [
    `/node_modules/(?!(${esmPackages.join("|")})/)(?!.*\\.mjs$)`,
  ],
  moduleNameMapper: {
    // import.meta.env tidak ada di luar Vite → ganti config/env dengan stub
    "^(\\.{1,2}/)+config/env$": "<rootDir>/src/test/env.ts",
    "\\.css$": "<rootDir>/src/test/styleStub.ts",
  },
  clearMocks: true,
  collectCoverageFrom: [
    "src/**/*.{ts,tsx}",
    "!src/**/*.d.ts",
    "!src/main.tsx",
    "!src/test/**",
  ],
  coverageReporters: ["text-summary", "lcov"],
  coverageThreshold: {
    "./src/lib/api/client.ts": { lines: 75 },
  },
  reporters: [
    "default",
    [
      "jest-junit",
      { outputDirectory: "reports", outputName: "jest-junit.xml" },
    ],
  ],
};
