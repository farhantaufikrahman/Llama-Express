import { useEffect, useRef, useState } from "react";
import { Button } from "./Button";
import { TextField } from "./TextField";

interface Props {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  danger?: boolean;
  withReason?: boolean; // minta alasan (wajib) — mis. membatalkan order
  busy?: boolean;
  error?: string | null;
  onConfirm: (reason: string) => void;
  onClose: () => void;
}

/**
 * Dialog konfirmasi memakai <dialog> bawaan HTML:
 * showModal() memberi focus trap, tombol Esc, dan latar gelap tanpa library tambahan.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Ya, lanjutkan",
  danger = false,
  withReason = false,
  busy = false,
  error,
  onConfirm,
  onClose,
}: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const [reason, setReason] = useState("");

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      setReason("");
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      className="m-auto w-[min(92vw,440px)] rounded-2xl bg-white p-0 text-ink shadow-xl backdrop:bg-navy-900/50"
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          onConfirm(reason.trim());
        }}
        className="flex flex-col gap-4 p-6"
      >
        <h2 className="text-lg font-extrabold">{title}</h2>
        {description && <p className="text-sm text-ink-muted">{description}</p>}
        {withReason && (
          <TextField
            label="Alasan"
            value={reason}
            maxLength={200}
            autoFocus
            onChange={(e) => setReason(e.target.value)}
          />
        )}
        {error && (
          <p role="alert" className="text-sm font-medium text-danger">
            {error}
          </p>
        )}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>
            Batal
          </Button>
          <Button
            type="submit"
            variant={danger ? "danger" : "primary"}
            disabled={busy || (withReason && !reason.trim())}
          >
            {busy ? "Memproses…" : confirmLabel}
          </Button>
        </div>
      </form>
    </dialog>
  );
}
