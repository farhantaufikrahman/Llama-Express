/**
 * Dijalankan SEBELUM framework test & SEBELUM file apa pun di-import (setupFiles),
 * supaya global ini sudah ada ketika undici/MSW/React dimuat.
 */
import { clearImmediate, setImmediate } from "node:timers";

// jsdom meniru browser, jadi global khas Node seperti setImmediate dihapus — undici & MSW membutuhkannya
Object.assign(globalThis, { setImmediate, clearImmediate });

/**
 * jest-fixed-jsdom memasang FormData milik Node (undici) agar fetch/MSW bekerja,
 * tetapi FormData Node tidak menerima elemen <form> (tidak ada DOM di server).
 * React 19 memanggil `new FormData(form, submitter)` saat submit → kita ajari FormData Node
 * membaca field form seperti di browser.
 */
const NodeFormData = globalThis.FormData;

class FormDataFromForm extends NodeFormData {
  constructor(form?: HTMLFormElement, submitter?: HTMLElement | null) {
    super();
    if (!form) return;
    for (const element of Array.from(form.elements)) {
      const el = element as HTMLInputElement;
      if (!el.name || el.disabled) continue;
      if (el.type === "file") continue; // File jsdom ≠ File Node; upload diuji lewat api(), bukan submit form
      if ((el.type === "checkbox" || el.type === "radio") && !el.checked)
        continue;
      if (el.type === "submit" || el.type === "button" || el.type === "reset") {
        if (el === submitter) this.append(el.name, el.value); // hanya tombol yang diklik ikut terkirim
        continue;
      }
      if (element instanceof HTMLSelectElement && element.multiple) {
        for (const option of Array.from(element.selectedOptions))
          this.append(el.name, option.value);
        continue;
      }
      this.append(el.name, el.value);
    }
  }
}

globalThis.FormData = FormDataFromForm as typeof FormData;
