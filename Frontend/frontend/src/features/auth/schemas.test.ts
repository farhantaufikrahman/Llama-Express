import { PHONE_REGEX, PLATE_REGEX } from "./schemas";

describe("PHONE_REGEX", () => {
  it.each(["081234567890", "085712345678"])("menerima %s", (v) =>
    expect(PHONE_REGEX.test(v)).toBe(true),
  );
  it.each(["0812", "abc", "12345678901234567", ""])('menolak "%s"', (v) =>
    expect(PHONE_REGEX.test(v)).toBe(false),
  );
});

describe("PLATE_REGEX", () => {
  it.each(["B 1234 XYZ", "D 4567 AB"])("menerima %s", (v) =>
    expect(PLATE_REGEX.test(v)).toBe(true),
  );
  it.each(["1234", "BBBB", ""])('menolak "%s"', (v) =>
    expect(PLATE_REGEX.test(v)).toBe(false),
  );
});
