import { formatDateTime, formatRupiah } from "./format";

it("formatRupiah memakai pemisah ribuan titik", () => {
  expect(formatRupiah(25000)).toMatch(/^Rp\s?25\.000$/); // \s mencakup non-breaking space dari Intl
});

it("formatDateTime menampilkan WIB (UTC+7)", () => {
  expect(formatDateTime("2026-10-19T08:00:00Z")).toMatch(/15[.:]00/);
});
