/**
 * Perkecil foto sebelum upload: sisi terpanjang maks 1600 px, JPEG kualitas 80%.
 * Foto HP 4–8 MB → ±200–400 KB. Upload jauh lebih cepat di jaringan seluler & tidak melewati batas 5 MB backend.
 */
export async function compressImage(
  file: File,
  maxSide = 1600,
  quality = 0.8,
): Promise<Blob> {
  if (!file.type.startsWith("image/")) {
    throw new Error("File harus berupa foto");
  }
  // imageOrientation: foto HP sering "miring" kalau orientasi EXIF diabaikan
  const bitmap = await createImageBitmap(file, {
    imageOrientation: "from-image",
  });
  const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext("2d")!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return new Promise((resolve, reject) =>
    canvas.toBlob(
      (blob) =>
        blob ? resolve(blob) : reject(new Error("Gagal memproses foto")),
      "image/jpeg",
      quality,
    ),
  );
}
