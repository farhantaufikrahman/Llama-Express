/**
 * Pesan yang dibawa dari satu halaman ke halaman berikutnya, lalu hilang setelah dibaca sekali.
 * Contoh: "Password berhasil diganti, silakan masuk lagi" — ditulis di halaman Akun, dibaca di halaman Login.
 * Padanan connect-flash di Express atau RedirectAttributes.addFlashAttribute di Spring MVC.
 */
let message: string | null = null;

export const flash = {
  set(text: string) {
    message = text;
  },
  consume(): string | null {
    const current = message;
    message = null;
    return current;
  },
};