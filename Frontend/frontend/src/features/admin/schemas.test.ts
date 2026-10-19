import { localInputToIso, tariffSchema } from "./schemas";

const valid = {
  serviceType: "INSTANT",
  vehicleType: "MOTOR",
  baseFare: "8000",
  perKmFare: "2500",
  minFare: "10000",
  activeFrom: "",
};

describe("tariffSchema", () => {
  it("menerima tarif valid", () => {
    expect(tariffSchema.safeParse(valid).success).toBe(true);
  });

  it("menolak minimum < tarif dasar, error ditempel di field minFare", () => {
    const r = tariffSchema.safeParse({ ...valid, minFare: "5000" });
    expect(r.success).toBe(false);
    expect(r.error?.issues[0].path).toEqual(["minFare"]);
  });

  it.each(["8.000", "8,000", "-1", "abc", ""])(
    'menolak format rupiah "%s"',
    (baseFare) => {
      expect(tariffSchema.safeParse({ ...valid, baseFare }).success).toBe(
        false,
      );
    },
  );
});

describe("localInputToIso", () => {
  it("00:00 WIB = 17:00 UTC hari sebelumnya", () => {
    expect(localInputToIso("2026-11-01T00:00")).toBe(
      "2026-10-31T17:00:00.000Z",
    );
  });
  it('kosong → undefined (backend memakai "sekarang")', () => {
    expect(localInputToIso("")).toBeUndefined();
  });
});
