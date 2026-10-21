const rupiah = new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 });
const dateTime = new Intl.DateTimeFormat('id-ID', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Jakarta' });

export const formatRupiah = (value: number) => rupiah.format(value);
export const formatDateTime = (iso: string) => dateTime.format(new Date(iso));