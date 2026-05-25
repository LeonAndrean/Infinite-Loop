package com.gereja.chatbot.database;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IbadahDatabaseHelper – Mengelola database terpisah ibadah.db
 */
public class IbadahDatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:ibadah.db";
    private static Connection conn;

    public static synchronized Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(DB_URL);
                conn.createStatement().execute("PRAGMA foreign_keys = ON");
                initDatabase(conn);
                System.out.println("[IbadahDB] Koneksi berhasil ke: " + DB_URL);
            }
        } catch (SQLException e) {
            System.err.println("[IbadahDB] Gagal koneksi: " + e.getMessage());
        }
        return conn;
    }

    private static void initDatabase(Connection c) throws SQLException {
        String[] sql = {
                // Tabel Jadwal Ibadah Mingguan
                """
            CREATE TABLE IF NOT EXISTS ibadah_mingguan (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                tanggal  TEXT UNIQUE NOT NULL, -- Format: yyyy-MM-dd
                jam      TEXT NOT NULL,
                tema     TEXT NOT NULL,
                pemimpin TEXT NOT NULL,
                ayat     TEXT NOT NULL,
                renungan TEXT NOT NULL
            )
            """,
                // Tabel Data Pendeta
                """
            CREATE TABLE IF NOT EXISTS pendeta (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                nama              TEXT UNIQUE NOT NULL,
                jabatan           TEXT NOT NULL,
                spesialisasi      TEXT,
                jadwal_konseling  TEXT,
                kontak            TEXT
            )
            """
        };

        try (Statement st = c.createStatement()) {
            for (String q : sql) st.execute(q);
        }

        insertDefaultDataIfEmpty(c);
    }

    private static void insertDefaultDataIfEmpty(Connection c) throws SQLException {
        // Cek tabel pendeta
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM pendeta")) {
            if (rs.getInt(1) == 0) {
                System.out.println("[IbadahDB] Mengisi data default pendeta...");
                String sqlPendeta = "INSERT INTO pendeta(nama, jabatan, spesialisasi, jadwal_konseling, kontak) VALUES(?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sqlPendeta)) {
                    // Pendeta 1
                    ps.setString(1, "Pdt. Samuel Harianto, M.Th.");
                    ps.setString(2, "Pendeta Jemaat (Kepala)");
                    ps.setString(3, "Pastoral & Penggembalaan Jemaat");
                    ps.setString(4, "Selasa & Kamis, pukul 13.00 - 16.00 WIB");
                    ps.setString(5, "+62 811-2800-345");
                    ps.executeUpdate();

                    // Pendeta 2
                    ps.setString(1, "Pdt. Ruth Maria Santoso, M.Div.");
                    ps.setString(2, "Pendeta Jemaat");
                    ps.setString(3, "Pendidikan Kristen, Pemuda & Anak");
                    ps.setString(4, "Rabu & Jumat, pukul 10.00 - 13.00 WIB");
                    ps.setString(5, "+62 811-2800-345");
                    ps.executeUpdate();
                }
            }
        }

        // Cek tabel ibadah mingguan
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ibadah_mingguan")) {
            if (rs.getInt(1) == 0) {
                System.out.println("[IbadahDB] Mengisi data default ibadah mingguan...");
                String sqlIbadah = "INSERT INTO ibadah_mingguan(tanggal, jam, tema, pemimpin, ayat, renungan) VALUES(?,?,?,?,?,?)";
                try (PreparedStatement ps = c.prepareStatement(sqlIbadah)) {
                    // 24 Mei 2026 (Hari Pentakosta)
                    ps.setString(1, "2026-05-24");
                    ps.setString(2, "07:00 (Jawa), 09:30 (Indonesia), 17:00 (Kontemporer)");
                    ps.setString(3, "Roh Kudus Penuntun Hidup Kita");
                    ps.setString(4, "Pdt. Samuel Harianto, M.Th.");
                    ps.setString(5, "Kisah Para Rasul 2:1-13");
                    ps.setString(6, "Hari Pentakosta mengingatkan kita akan pencurahan Roh Kudus. Roh Kudus bukan sekadar kuasa, melainkan Pribadi Allah yang tinggal di dalam kita untuk memimpin hidup kita dalam kebenaran. Mari serahkan hati kita dipimpin oleh-Nya.");
                    ps.executeUpdate();

                    // 31 Mei 2026 (Hari Trinitas / Minggu Ini)
                    ps.setString(1, "2026-05-31");
                    ps.setString(2, "07:00 (Jawa), 09:30 (Indonesia), 17:00 (Kontemporer)");
                    ps.setString(3, "Kasih Karunia Allah Tritunggal");
                    ps.setString(4, "Pdt. Ruth Maria Santoso, M.Div.");
                    ps.setString(5, "2 Korintus 13:13");
                    ps.setString(6, "Kasih karunia Tuhan Yesus Kristus, kasih Allah Bapa, dan persekutuan Roh Kudus menyertai kita sekalian. Sebagai umat pilihan, marilah kita senantiasa hidup dalam keselarasan kasih surgawi.");
                    ps.executeUpdate();

                    // 07 Juni 2026 (Minggu Depan)
                    ps.setString(1, "2026-06-07");
                    ps.setString(2, "07:00 (Jawa), 09:30 (Indonesia), 17:00 (Kontemporer)");
                    ps.setString(3, "Melayani dengan Kesetiaan");
                    ps.setString(4, "Pdt. Samuel Harianto, M.Th.");
                    ps.setString(5, "Matius 25:21");
                    ps.setString(6, "Kesetiaan dalam perkara-perkara kecil adalah fondasi dari kepercayaan besar yang Allah berikan. Mari melayani sesama dengan tulus hati tanpa menuntut pujian manusia.");
                    ps.executeUpdate();

                    // 14 Juni 2026 (Minggu Dua Minggu Lagi)
                    ps.setString(1, "2026-06-14");
                    ps.setString(2, "07:00 (Jawa), 09:30 (Indonesia), 17:00 (Kontemporer)");
                    ps.setString(3, "Kekuatan di Tengah Kelemahan");
                    ps.setString(4, "Pdt. Ruth Maria Santoso, M.Div.");
                    ps.setString(5, "2 Korintus 12:9");
                    ps.setString(6, "Cukuplah kasih karunia-Ku bagimu, sebab di dalam kelemahanlah kuasa-Ku menjadi sempurna. Jangan putus asa saat menghadapi pergumulan hidup yang berat.");
                    ps.executeUpdate();

                    // 21 Juni 2026 (Minggu Tiga Minggu Lagi)
                    ps.setString(1, "2026-06-21");
                    ps.setString(2, "07:00 (Jawa), 09:30 (Indonesia), 17:00 (Kontemporer)");
                    ps.setString(3, "Hidup Baru dalam Kristus");
                    ps.setString(4, "Pdt. Samuel Harianto, M.Th.");
                    ps.setString(5, "2 Korintus 5:17");
                    ps.setString(6, "Jadi siapa yang ada di dalam Kristus, ia adalah ciptaan baru: yang lama sudah berlalu, sesungguhnya yang baru sudah datang. Mari tinggalkan manusia lama kita.");
                    ps.executeUpdate();
                }
            }
        }
    }

    public static Map<String, String> getIbadahByDate(String dateStr) {
        Map<String, String> res = new LinkedHashMap<>();
        String sql = "SELECT * FROM ibadah_mingguan WHERE tanggal = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, dateStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    res.put("tanggal", rs.getString("tanggal"));
                    res.put("jam", rs.getString("jam"));
                    res.put("tema", rs.getString("tema"));
                    res.put("pemimpin", rs.getString("pemimpin"));
                    res.put("ayat", rs.getString("ayat"));
                    res.put("renungan", rs.getString("renungan"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[IbadahDB] Error getIbadahByDate: " + e.getMessage());
        }
        return res;
    }

    public static Map<String, String> getIbadahTerdekat(String baseDateStr) {
        Map<String, String> res = new LinkedHashMap<>();
        String sql = "SELECT * FROM ibadah_mingguan WHERE tanggal >= ? ORDER BY tanggal ASC LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, baseDateStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    res.put("tanggal", rs.getString("tanggal"));
                    res.put("jam", rs.getString("jam"));
                    res.put("tema", rs.getString("tema"));
                    res.put("pemimpin", rs.getString("pemimpin"));
                    res.put("ayat", rs.getString("ayat"));
                    res.put("renungan", rs.getString("renungan"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[IbadahDB] Error getIbadahTerdekat: " + e.getMessage());
        }

        if (res.isEmpty()) {
            sql = "SELECT * FROM ibadah_mingguan ORDER BY tanggal DESC LIMIT 1";
            try (Statement st = getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    res.put("tanggal", rs.getString("tanggal"));
                    res.put("jam", rs.getString("jam"));
                    res.put("tema", rs.getString("tema"));
                    res.put("pemimpin", rs.getString("pemimpin"));
                    res.put("ayat", rs.getString("ayat"));
                    res.put("renungan", rs.getString("renungan"));
                }
            } catch (SQLException e) {
                System.err.println("[IbadahDB] Error fallback getIbadahTerdekat: " + e.getMessage());
            }
        }
        return res;
    }

    public static List<Map<String, String>> getAllPendeta() {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT * FROM pendeta ORDER BY id ASC";
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("nama", rs.getString("nama"));
                m.put("jabatan", rs.getString("jabatan"));
                m.put("spesialisasi", rs.getString("spesialisasi"));
                m.put("jadwal_konseling", rs.getString("jadwal_konseling"));
                m.put("kontak", rs.getString("kontak"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[IbadahDB] Error getAllPendeta: " + e.getMessage());
        }
        return list;
    }
}