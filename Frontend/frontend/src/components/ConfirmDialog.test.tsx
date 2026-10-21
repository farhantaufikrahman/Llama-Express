import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ConfirmDialog } from "./ConfirmDialog";

function setup(props: Partial<Parameters<typeof ConfirmDialog>[0]> = {}) {
  const onConfirm = jest.fn();
  const onClose = jest.fn();
  render(
    <ConfirmDialog
      open
      title="Batalkan LX-1?"
      withReason
      onConfirm={onConfirm}
      onClose={onClose}
      {...props}
    />,
  );
  return { user: userEvent.setup(), onConfirm, onClose };
}

it("tombol konfirmasi aktif hanya setelah alasan diisi; alasan dikirim sudah di-trim", async () => {
  const { user, onConfirm } = setup();
  const confirm = screen.getByRole("button", { name: "Ya, lanjutkan" });
  expect(confirm).toBeDisabled();

  await user.type(screen.getByLabelText("Alasan"), "  Alamat salah  ");
  expect(confirm).toBeEnabled();
  await user.click(confirm);

  expect(onConfirm).toHaveBeenCalledWith("Alamat salah");
});

it("menampilkan error dari server", () => {
  setup({ error: "Order sudah selesai" });
  expect(screen.getByRole("alert")).toHaveTextContent("Order sudah selesai");
});

it("tombol Batal memanggil onClose", async () => {
  const { user, onClose } = setup();
  await user.click(screen.getByRole("button", { name: "Batal" }));
  expect(onClose).toHaveBeenCalled();
});
