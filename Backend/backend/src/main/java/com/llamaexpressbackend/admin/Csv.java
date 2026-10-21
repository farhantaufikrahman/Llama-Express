package com.llamaexpressbackend.admin;

/** Escaping sel CSV (RFC 4180) + perlindungan CSV/formula injection */
final class Csv {

    private Csv() {
    }

    /** Untuk teks bebas dari pengguna (alamat, catatan, nama) */
    static String text(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v; // Excel tidak akan menjalankannya sebagai formula
        }
        return quoteIfNeeded(v);
    }

    /** Untuk nilai buatan sistem (kode, enum, angka, tanggal) */
    static String raw(Object value) {
        return (value == null) ? "" : quoteIfNeeded(value.toString());
    }

    private static String quoteIfNeeded(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}