import { toQuery } from "./api";

it("toQuery membuang filter kosong tetapi mempertahankan angka 0", () => {
  expect(
    toQuery({ status: "DELIVERED", q: "", from: undefined, page: 0 }),
  ).toBe("status=DELIVERED&page=0");
});

it("toQuery meng-encode karakter khusus", () => {
  expect(toQuery({ q: "budi+1@mail.com" })).toBe("q=budi%2B1%40mail.com");
});
