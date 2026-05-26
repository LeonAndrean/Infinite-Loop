package com.gereja.chatbot.database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:data.db";
    private static Connection conn;

    // ══════════════════════════════════════════════════════════
    //  KONEKSI
    // ══════════════════════════════════════════════════════════

    public static Connection getConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(DB_URL);
                conn.createStatement().execute("PRAGMA foreign_keys = ON");
                initDatabase(conn);
                System.out.println("[DB] Koneksi berhasil ke: " + DB_URL);
            }
        } catch (SQLException e) {
            System.out.println("[DB] Gagal koneksi: " + e.getMessage());
        }
        return conn;
    }

    // ══════════════════════════════════════════════════════════
    //  INISIALISASI TABEL
    // ══════════════════════════════════════════════════════════

    private static void initDatabase(Connection c) throws SQLException {
        String[] sql = {
                // Jadwal kegiatan
                """
            CREATE TABLE IF NOT EXISTS pertanyaan_tak_terjawab (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                pertanyaan TEXT NOT NULL,
                waktu      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
                """,
                """
            CREATE TABLE IF NOT EXISTS jadwal (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_kegiatan TEXT    NOT NULL,
                kategori      TEXT    NOT NULL,
                tanggal       TEXT    NOT NULL,
                jam_mulai     TEXT,
                jam_selesai   TEXT,
                lokasi        TEXT,
                deskripsi     TEXT,
                warna_aksen   TEXT    DEFAULT '#1A3A2A'
            )
            """,
                // Deadline pendaftaran
                """
            CREATE TABLE IF NOT EXISTS deadline (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_kegiatan    TEXT NOT NULL,
                tanggal_deadline TEXT NOT NULL,
                kategori         TEXT NOT NULL,
                keterangan       TEXT
            )
            """,
                // Info gereja (key-value)
                """
            CREATE TABLE IF NOT EXISTS info_gereja (
                id    INTEGER PRIMARY KEY AUTOINCREMENT,
                kunci TEXT UNIQUE NOT NULL,
                nilai TEXT NOT NULL
            )
            """,
                // Riwayat chat
                """
            CREATE TABLE IF NOT EXISTS riwayat_chat (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                pengirim TEXT NOT NULL,
                pesan    TEXT NOT NULL,
                waktu    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """,
                // Syarat dokumen per layanan
                """
            CREATE TABLE IF NOT EXISTS syarat_dokumen (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                kategori   TEXT NOT NULL,
                nomor_urut INTEGER NOT NULL,
                dokumen    TEXT NOT NULL
            )
            """,
                // Biaya layanan
                """
            CREATE TABLE IF NOT EXISTS biaya_layanan (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                kategori   TEXT NOT NULL,
                nama_item  TEXT NOT NULL,
                biaya      TEXT NOT NULL,
                keterangan TEXT
            )
            """,
                // Admin users
                """
            CREATE TABLE IF NOT EXISTS admin_users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                nama_lengkap  TEXT NOT NULL,
                created_at    TEXT DEFAULT (datetime('now','localtime'))
            )
            """,
                // Kata kunci Q&A
                """
            CREATE TABLE IF NOT EXISTS kata_kunci_qa (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                kata_kunci      TEXT UNIQUE NOT NULL,
                pertanyaan_asli TEXT,
                jawaban         TEXT NOT NULL,
                kategori        TEXT DEFAULT 'UMUM',
                hit_count       INTEGER DEFAULT 0,
                created_at      TEXT DEFAULT (datetime('now','localtime')),
                updated_at      TEXT DEFAULT (datetime('now','localtime'))
            )
            """,
                // Ayat harian (CRUD admin)
                """
            CREATE TABLE IF NOT EXISTS ayat_harian (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                tanggal    TEXT UNIQUE,
                referensi  TEXT NOT NULL,
                ayat       TEXT NOT NULL,
                keterangan TEXT,
                aktif      INTEGER DEFAULT 1,
                created_at TEXT DEFAULT (datetime('now','localtime'))
            )
            """
        };

        try (Statement st = c.createStatement()) {
            for (String q : sql) st.execute(q);
        }

        insertDefaultDataIfEmpty(c);
    }

    // ══════════════════════════════════════════════════════════
//  PERTANYAAN TAK TERJAWAB
// ══════════════════════════════════════════════════════════

    public static void simpanPertanyaanTakTerjawab(String pertanyaan) {
        if (pertanyaan == null || pertanyaan.isBlank()) return;

        String sql = "INSERT INTO pertanyaan_tak_terjawab (pertanyaan) VALUES (?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, pertanyaan);
            ps.executeUpdate();
            System.out.println("[DB] Pertanyaan tak terjawab disimpan: " + pertanyaan);
        } catch (SQLException e) {
            System.out.println("[DB] Gagal simpanPertanyaanTakTerjawab: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  DATA AWAL – REAL CHURCH DATA
    // ══════════════════════════════════════════════════════════


    private static void insertDefaultDataIfEmpty(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM info_gereja")) {
            if (rs.getInt(1) > 0) return;
        }

        System.out.println("[DB] Mengisi data awal gereja...");

        // ── Info Gereja ──────────────────────────────────────
        String[][] info = {
                {"nama_gereja",      "GKJ Ngupasan Yogyakarta"},
                {"telepon",          "(0274) 512-345"},
                {"whatsapp",         "+62 811-2800-345"},
                {"email",            "sekretariat@gkjngupasan.org"},
                {"alamat",           "Jl. Ngupasan No. 1, Gondomanan, Yogyakarta 55122"},
                {"website",          "www.gkjngupasan.org"},
                {"instagram",        "@gkjngupasan"},
                {"facebook",         "GKJ Ngupasan Yogyakarta"},
                {"jam_operasional",  "Senin–Jumat: 08.00–16.00 | Sabtu: 09.00–12.00 | Minggu: 07.00–13.00"},
                {"pendeta_1",        "Pdt. Samuel Harianto, M.Th."},
                {"pendeta_2",        "Pdt. Ruth Maria Santoso, M.Div."},
                {"majelis_ketua",    "Bpk. Andreas Prijatno"},
                {"rekening_bank",    "BRI – 0123-01-012345-30-6 a.n. GKJ Ngupasan"},
                {"tahun_berdiri",    "1857"},
                {"denomiasi",        "Gereja Kristen Jawa (GKJ)"},
                {"visi",             "Menjadi komunitas iman yang hidup dalam kasih Kristus, melayani sesama dan dunia."},
                {"misi",             "Memberitakan Injil, mendewasakan iman, dan menghadirkan keadilan Allah di tengah masyarakat."}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO info_gereja(kunci,nilai) VALUES(?,?)")) {
            for (String[] row : info) {
                ps.setString(1, row[0]); ps.setString(2, row[1]); ps.executeUpdate();
            }
        }

        // ── Syarat Dokumen ───────────────────────────────────
        Object[][] syarat = {
                {"BAPTIS", 1, "Fotokopi KTP pemohon (atau orang tua/wali untuk anak)"},
                {"BAPTIS", 2, "Fotokopi Kartu Keluarga (KK)"},
                {"BAPTIS", 3, "Pas foto 3×4 sebanyak 2 lembar (latar merah)"},
                {"BAPTIS", 4, "Formulir pendaftaran baptis (tersedia di sekretariat)"},
                {"BAPTIS", 5, "Surat pernyataan orang tua/wali (untuk baptis anak di bawah 12 tahun)"},
                {"BAPTIS", 6, "Akta Kelahiran asli (untuk baptis anak)"},
                {"BAPTIS", 7, "Surat Keterangan dari gereja asal (jika pindah gereja)"},
                {"PERNIKAHAN", 1, "Fotokopi KTP masing-masing mempelai"},
                {"PERNIKAHAN", 2, "Fotokopi Akta Kelahiran kedua mempelai"},
                {"PERNIKAHAN", 3, "Surat Baptis asli dari gereja masing-masing"},
                {"PERNIKAHAN", 4, "Pas foto berdampingan 4×6 sebanyak 4 lembar"},
                {"PERNIKAHAN", 5, "Surat N1 – Surat Keterangan untuk menikah (dari Kelurahan)"},
                {"PERNIKAHAN", 6, "Surat N2 – Surat Keterangan asal-usul (dari Kelurahan)"},
                {"PERNIKAHAN", 7, "Surat N4 – Surat Keterangan tentang orang tua (dari Kelurahan)"},
                {"PERNIKAHAN", 8, "Surat Izin Orang Tua (jika salah satu mempelai belum berusia 21 tahun)"},
                {"PERNIKAHAN", 9, "Surat Keputusan Pengadilan (jika pernah bercerai)"},
                {"SIDI", 1, "Sudah menerima Baptisan Kudus (wajib)"},
                {"SIDI", 2, "Berusia minimal 14 tahun pada saat peneguhan"},
                {"SIDI", 3, "Fotokopi Surat Baptis"},
                {"SIDI", 4, "Fotokopi KTP atau Kartu Pelajar"},
                {"SIDI", 5, "Pas foto 3×4 sebanyak 2 lembar"},
                {"SIDI", 6, "Formulir pendaftaran SIDI dari sekretariat"}
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO syarat_dokumen(kategori,nomor_urut,dokumen) VALUES(?,?,?)")) {
            for (Object[] row : syarat) {
                ps.setString(1, (String) row[0]);
                ps.setInt(2, (Integer) row[1]);
                ps.setString(3, (String) row[2]);
                ps.executeUpdate();
            }
        }

        // ── Biaya Layanan ────────────────────────────────────
        Object[][] biaya = {
                {"BAPTIS",      "Biaya Administrasi",          "Gratis",         "Persembahan sukarela diperbolehkan"},
                {"SIDI",        "Kelas Persiapan & Peneguhan", "Gratis",         "Modul dan bahan ajar disediakan oleh gereja"},
                {"KONSELING",   "Konseling Pastoral",          "Gratis",         "Persembahan sukarela diperbolehkan"},
                {"PERNIKAHAN",  "Biaya Administrasi Gereja",   "Rp 750.000",     "Dibayar paling lambat 1 bulan sebelum pemberkatan"},
                {"PERNIKAHAN",  "Sewa Gedung Serbaguna",       "Rp 2.000.000",   "Untuk resepsi / kapasitas 200 orang"},
                {"PERNIKAHAN",  "Dekorasi Standar Gereja",     "Rp 500.000",     "Bunga altar dan kain pita (opsional)"},
                {"PERNIKAHAN",  "Dokumentasi Foto & Video",    "Rp 1.500.000",   "Oleh tim dokumentasi gereja (opsional)"},
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO biaya_layanan(kategori,nama_item,biaya,keterangan) VALUES(?,?,?,?)")) {
            for (Object[] row : biaya) {
                ps.setString(1, (String) row[0]); ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, row[3] != null ? (String) row[3] : "");
                ps.executeUpdate();
            }
        }

        // ── Admin Default ─────────────────────────────────────
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM admin_users")) {
            if (rs.getInt(1) == 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO admin_users(username,password_hash,nama_lengkap) VALUES(?,?,?)")) {
                    ps.setString(1, "admin");
                    ps.setString(2, sha256("admin123"));
                    ps.setString(3, "Administrator GKJ Ngupasan");
                    ps.executeUpdate();
                }
            }
        }

        // ── Ayat Harian Default ───────────────────────────────
        insertDefaultAyat(c);

        // ── Kata Kunci Q&A Default ────────────────────────────
        insertDefaultKataKunci(c);

        // ── Jadwal Default (untuk Kalender) ──────────────────
        insertDefaultJadwal(c);

        System.out.println("[DB] Data awal berhasil diisi.");
    }

    private static void insertDefaultAyat(Connection c) throws SQLException {
        Object[][] ayat = {
                {null, "Yohanes 3:16",
                        "Karena begitu besar kasih Allah akan dunia ini, sehingga Ia telah mengaruniakan Anak-Nya yang tunggal, supaya setiap orang yang percaya kepada-Nya tidak binasa, melainkan beroleh hidup yang kekal.",
                        "Kasih Allah"},
                {null, "Filipi 4:13",
                        "Segala perkara dapat kutanggung di dalam Dia yang memberi kekuatan kepadaku.",
                        "Kekuatan dalam Kristus"},
                {null, "Yeremia 29:11",
                        "Sebab Aku ini mengetahui rancangan-rancangan apa yang ada pada-Ku mengenai kamu, demikianlah firman TUHAN, yaitu rancangan damai sejahtera dan bukan rancangan kecelakaan, untuk memberikan kepadamu hari depan yang penuh harapan.",
                        "Harapan dari Allah"},
                {null, "Mazmur 23:1",
                        "TUHAN adalah gembalaku, takkan kekurangan aku.",
                        "Pemeliharaan Allah"},
                {null, "Roma 8:28",
                        "Kita tahu sekarang, bahwa Allah turut bekerja dalam segala sesuatu untuk mendatangkan kebaikan bagi mereka yang mengasihi Dia, yaitu bagi mereka yang terpanggil sesuai dengan rencana Allah.",
                        "Rencana Allah"},
                {null, "Amsal 3:5-6",
                        "Percayalah kepada TUHAN dengan segenap hatimu, dan janganlah bersandar kepada pengertianmu sendiri. Akuilah Dia dalam segala lakumu, maka Ia akan meluruskan jalanmu.",
                        "Kepercayaan kepada Tuhan"},
                {null, "Matius 6:33",
                        "Tetapi carilah dahulu Kerajaan Allah dan kebenarannya, maka semuanya itu akan ditambahkan kepadamu.",
                        "Prioritas Kerajaan Allah"},
                {null, "Ibrani 11:1",
                        "Iman adalah dasar dari segala sesuatu yang kita harapkan dan bukti dari segala sesuatu yang tidak kita lihat.",
                        "Tentang Iman"},
                {null, "1 Korintus 13:4-5",
                        "Kasih itu sabar; kasih itu murah hati; ia tidak cemburu. Ia tidak memegahkan diri dan tidak sombong. Ia tidak melakukan yang tidak sopan dan tidak mencari keuntungan diri sendiri.",
                        "Tentang Kasih"},
                {null, "Galatia 5:22-23",
                        "Tetapi buah Roh ialah: kasih, sukacita, damai sejahtera, kesabaran, kemurahan, kebaikan, kesetiaan, kelemahlembutan, penguasaan diri.",
                        "Buah Roh"},
                {null, "Efesus 2:8-9",
                        "Sebab karena kasih karunia kamu diselamatkan oleh iman; itu bukan hasil usahamu, tetapi pemberian Allah, itu bukan hasil pekerjaanmu: jangan ada orang yang memegahkan diri.",
                        "Keselamatan oleh Kasih Karunia"},
                {null, "Mazmur 46:2",
                        "Allah itu bagi kita tempat perlindungan dan kekuatan, sebagai penolong dalam kesesakan sangat terbukti.",
                        "Perlindungan Allah"},
                {null, "Yosua 1:9",
                        "Kuatkan dan teguhkanlah hatimu! Janganlah kecut dan tawar hati, sebab TUHAN, Allahmu, menyertai engkau, ke mana pun engkau pergi.",
                        "Keberanian dalam Iman"},
                {null, "Mazmur 119:105",
                        "Firman-Mu itu pelita bagi kakiku dan terang bagi jalanku.",
                        "Firman Allah sebagai Pandu"},
                {null, "Roma 12:12",
                        "Bersukacitalah dalam pengharapan, sabarlah dalam kesesakan, dan bertekunlah dalam doa!",
                        "Hidup Kristen"},
                {null, "Yohanes 14:6",
                        "Akulah jalan dan kebenaran dan hidup. Tidak ada seorang pun yang datang kepada Bapa, kalau tidak melalui Aku.",
                        "Yesus Sang Jalan"},
                {null, "Mazmur 37:4",
                        "Bergembiralah karena TUHAN; maka Ia akan memberikan kepadamu apa yang diinginkan hatimu.",
                        "Sukacita dalam Tuhan"},
                {null, "Kolose 3:23",
                        "Apapun juga yang kamu perbuat, perbuatlah dengan segenap hatimu seperti untuk Tuhan dan bukan untuk manusia.",
                        "Bekerja untuk Tuhan"},
                {null, "Yakobus 1:17",
                        "Setiap pemberian yang baik dan setiap anugerah yang sempurna, datangnya dari atas, diturunkan dari Bapa segala terang.",
                        "Berkat dari Allah"},
                {null, "Matius 11:28",
                        "Marilah kepada-Ku, semua yang letih lesu dan berbeban berat, Aku akan memberi kelegaan kepadamu.",
                        "Istirahat dalam Kristus"},
                {null, "2 Timotius 1:7",
                        "Sebab Allah memberikan kepada kita bukan roh ketakutan, melainkan roh yang membangkitkan kekuatan, kasih dan ketertiban.",
                        "Roh Kekuatan"},
                {null, "Filipi 4:6-7",
                        "Janganlah hendaknya kamu kuatir tentang apapun juga, tetapi nyatakanlah dalam segala hal keinginanmu kepada Allah dalam doa dan permohonan dengan ucapan syukur. Damai sejahtera Allah yang melampaui segala akal manusia akan memelihara hati dan pikiranmu dalam Kristus Yesus.",
                        "Damai Sejahtera Allah"},
                {null, "Yohanes 11:25",
                        "Akulah kebangkitan dan hidup; barangsiapa percaya kepada-Ku, ia akan hidup walaupun ia sudah mati.",
                        "Kebangkitan dan Hidup"},
                {null, "Mazmur 91:1",
                        "Orang yang duduk dalam lindungan Yang Mahatinggi dan bermalam dalam naungan Yang Mahakuasa akan berkata kepada TUHAN: Tempat perlindunganku dan kubu pertahananku, Allahku, yang kupercayai.",
                        "Perlindungan Yang Mahatinggi"},
                {null, "Wahyu 21:4",
                        "Dan Ia akan menghapus segala air mata dari mata mereka, dan maut tidak akan ada lagi; tidak akan ada lagi perkabungan, atau ratap tangis, atau dukacita, sebab segala sesuatu yang lama itu telah berlalu.",
                        "Janji Surga"},
                {null, "Lukas 1:37",
                        "Sebab bagi Allah tidak ada yang mustahil.",
                        "Kemahakuasaan Allah"},
                {null, "Amsal 22:6",
                        "Didiklah orang muda menurut jalan yang patut baginya, maka pada masa tuanya pun ia tidak akan menyimpang dari pada jalan itu.",
                        "Mendidik Generasi"},
                {null, "Roma 5:8",
                        "Akan tetapi Allah menunjukkan kasih-Nya kepada kita, oleh karena Kristus telah mati untuk kita, ketika kita masih berdosa.",
                        "Kasih Allah dalam Kristus"},
                {null, "Mazmur 27:1",
                        "TUHAN adalah terangku dan keselamatanku, kepada siapakah aku harus takut? TUHAN adalah benteng hidupku, terhadap siapakah aku harus gemetar?",
                        "Keberanian Iman"},
                {null, "1 Yohanes 4:19",
                        "Kita mengasihi, karena Allah lebih dahulu mengasihi kita.",
                        "Kasih Berbalas"},
                {null, "Kisah Para Rasul 2:38",
                        "Bertobatlah dan hendaklah kamu masing-masing memberi dirimu untuk dibaptis dalam nama Yesus Kristus untuk pengampunan dosamu, maka kamu akan menerima karunia Roh Kudus.",
                        "Panggilan untuk Baptisan"},
        };

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO ayat_harian(tanggal,referensi,ayat,keterangan,aktif) VALUES(?,?,?,?,1)")) {
            for (Object[] row : ayat) {
                ps.setString(1, (String) row[0]); // null = ayat rotasi
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, (String) row[3]);
                ps.executeUpdate();
            }
        }
    }

    private static void insertDefaultKataKunci(Connection c) throws SQLException {
        Object[][] kk = {
                {"visi gereja",           "Apa visi GKJ Ngupasan?",
                        "Visi GKJ Ngupasan Yogyakarta:\n\n\"Menjadi komunitas iman yang hidup dalam kasih Kristus, melayani sesama dan dunia.\"\n\nVisi ini menjadi arah pelayanan seluruh jemaat dan majelis dalam setiap kegiatan gereja.", "UMUM"},

                {"misi gereja",           "Apa misi GKJ Ngupasan?",
                        "Misi GKJ Ngupasan Yogyakarta:\n\n• Memberitakan Injil kepada semua orang\n• Mendewasakan iman jemaat melalui Firman, Sakramen, dan Doa\n• Menghadirkan keadilan dan damai Allah di tengah masyarakat\n• Melayani sesama tanpa memandang perbedaan", "UMUM"},

                {"sejarah gereja",        "Bagaimana sejarah GKJ Ngupasan?",
                        "GKJ Ngupasan Yogyakarta berdiri sejak tahun 1857 dan merupakan salah satu gereja tertua di Yogyakarta.\n\nGereja ini lahir dari pelayanan penginjilan di kawasan Gondomanan. Selama lebih dari satu setengah abad, GKJ Ngupasan telah melayani ribuan jemaat dan mengalami pertumbuhan iman yang berkelanjutan.", "UMUM"},

                {"jam ibadah minggu",     "Kapan jam ibadah Minggu?",
                        "Jadwal Ibadah Minggu GKJ Ngupasan:\n\n🕖 Ibadah I   : 07.00 WIB – Gedung Utama\n🕤 Ibadah II  : 09.30 WIB – Gedung Utama\n🕔 Ibadah III : 17.00 WIB – Gedung Utama\n\n📚 Sekolah Minggu : 09.30 WIB (Ruang Anak, Lantai 1)\n👨‍👩‍👧 PA Keluarga     : 09.30 WIB (Ruang Keluarga, Lantai 2)\n\nSilakan hadir sesuai kapasitas tempat duduk.", "UMUM"},

                {"ibadah pemuda",         "Kapan ibadah pemuda?",
                        "Ibadah Pemuda GKJ Ngupasan:\n\n📅 Setiap Sabtu, pukul 16.00–18.00 WIB\n📍 Lokasi: Ruang Pemuda, Lantai 2\n\nKegiatan ibadah pemuda meliputi pujian & penyembahan, firman Tuhan, persekutuan, serta kegiatan sosial bulanan.\n\nInfo lebih lanjut hubungi: +62 811-2800-345", "UMUM"},

                {"pendeta",               "Siapa pendeta GKJ Ngupasan?",
                        "Pendeta GKJ Ngupasan Yogyakarta:\n\n👨‍⚕️ Pdt. Samuel Harianto, M.Th.\n   - Spesialisasi: Pastoral & Penggembalaan\n   - Konseling: Selasa & Kamis 13.00–16.00 WIB\n\n👩‍⚕️ Pdt. Ruth Maria Santoso, M.Div.\n   - Spesialisasi: Pendidikan Jemaat & Pemuda\n   - Konseling: Rabu & Jumat 10.00–13.00 WIB\n\nUntuk penjadwalan, hubungi sekretariat di (0274) 512-345.", "UMUM"},

                {"majelis",               "Siapa majelis gereja?",
                        "Majelis GKJ Ngupasan Yogyakarta:\n\n• Ketua Majelis : Bpk. Andreas Prijatno\n• Sekretaris     : Ibu Theresia Wahyu\n• Bendahara      : Bpk. Petrus Santoso\n\nRapat Majelis berlangsung setiap Minggu pertama pukul 19.30 WIB.\n\nUntuk keperluan surat menyurat silakan melalui sekretariat.", "UMUM"},

                {"rekening gereja",       "Apa rekening bank gereja?",
                        "Rekening Gereja GKJ Ngupasan:\n\n🏦 BRI – 0123-01-012345-30-6\n   a.n. GKJ Ngupasan Yogyakarta\n\nRekening ini digunakan untuk:\n• Persembahan online\n• Pembayaran administrasi pernikahan\n• Donasi pembangunan gedung\n\nSertakan berita transfer (nama + keperluan) agar dapat dicatat dengan baik.", "UMUM"},

                {"parking parkir",        "Apakah ada tempat parkir?",
                        "Fasilitas Parkir GKJ Ngupasan:\n\n🚗 Parkir Mobil : Halaman belakang gereja (kapasitas 30 kendaraan)\n🏍️ Parkir Motor : Sisi kiri gedung (kapasitas 100 kendaraan)\n\nUntuk ibadah Minggu pagi, disarankan hadir lebih awal karena kapasitas parkir terbatas. Jemaat dapat menggunakan parkir umum di sekitar Gondomanan.", "UMUM"},

                {"perpustakaan gereja",   "Apakah ada perpustakaan?",
                        "Perpustakaan GKJ Ngupasan:\n\n📚 Koleksi: 1.200+ judul buku teologi, Alkitab, rohani, dan umum\n⏰ Jam buka: Minggu 08.00–12.00 WIB & Rabu 09.00–15.00 WIB\n📍 Lokasi: Lantai 1, sayap kanan gedung utama\n\nAnggota jemaat dapat meminjam maksimal 2 buku selama 2 minggu. Pendaftaran kartu anggota melalui sekretariat.", "UMUM"},

                {"kelas alkitab",         "Apakah ada kelas Alkitab?",
                        "Kelas Alkitab & PA GKJ Ngupasan:\n\n📖 PA Jemaat Umum  : Rabu, 18.30–20.00 WIB (Ruang Serbaguna Lt.1)\n📖 PA Kaum Bapak   : Jumat, 18.30–20.00 WIB (Aula Gedung Lama)\n📖 PA Kaum Ibu     : Kamis, 09.00–11.00 WIB (Ruang Pertemuan Lt.2)\n📖 Pendalaman PL/PB: Sabtu, 08.00–09.30 WIB\n\nSemua kegiatan PA terbuka untuk umum dan tidak dipungut biaya.", "UMUM"},

                {"diakonia",              "Apa itu diakonia?",
                        "Diakonia GKJ Ngupasan:\n\nDiakonia adalah pelayanan sosial gereja yang mencakup:\n\n🤝 Bantuan Sosial Jemaat (BSJ) – bagi jemaat yang membutuhkan\n🏥 Kunjungan Orang Sakit – setiap Selasa & Kamis\n🎓 Beasiswa Pendidikan  – untuk pelajar dari keluarga tidak mampu\n🏘️ Pelayanan Masyarakat – bakti sosial rutin 3 bulanan\n\nInfokan kebutuhan melalui: (0274) 512-345 atau langsung ke Majelis setempat.", "UMUM"},
        };

        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO kata_kunci_qa(kata_kunci,pertanyaan_asli,jawaban,kategori) VALUES(?,?,?,?)")) {
            for (Object[] row : kk) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, (String) row[3]);
                ps.executeUpdate();
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PASSWORD HASH
    // ══════════════════════════════════════════════════════════

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  AYAT HARIAN
    // ══════════════════════════════════════════════════════════

    /**
     * Ambil ayat untuk hari ini.
     * Prioritas: ayat dengan tanggal = hari ini → rotasi berdasarkan hari dalam tahun.
     */
    public static String[] getAyatHarian() {
        String today = LocalDate.now().toString(); // yyyy-MM-dd
        // Cari ayat khusus tanggal hari ini
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT referensi, ayat, keterangan FROM ayat_harian WHERE tanggal=? AND aktif=1 LIMIT 1")) {
            ps.setString(1, today);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new String[]{ rs.getString("referensi"), rs.getString("ayat"), rs.getString("keterangan") };
            }
        } catch (SQLException e) { System.out.println("[DB] getAyatHarian error: " + e.getMessage()); }

        // Rotasi: pilih berdasarkan dayOfYear mod jumlah ayat
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ayat_harian WHERE tanggal IS NULL AND aktif=1")) {
            int total = rs.getInt(1);
            if (total == 0) return new String[]{"Filipi 4:13", "Segala perkara dapat kutanggung di dalam Dia yang memberi kekuatan kepadaku.", "Kekuatan dalam Kristus"};
            int dayOfYear = LocalDate.now().getDayOfYear();
            int offset = (dayOfYear - 1) % total;
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "SELECT referensi, ayat, keterangan FROM ayat_harian WHERE tanggal IS NULL AND aktif=1 ORDER BY id LIMIT 1 OFFSET ?")) {
                ps.setInt(1, offset);
                ResultSet rs2 = ps.executeQuery();
                if (rs2.next()) {
                    return new String[]{ rs2.getString("referensi"), rs2.getString("ayat"), rs2.getString("keterangan") };
                }
            }
        } catch (SQLException e) { System.out.println("[DB] getAyatHarian rotasi error: " + e.getMessage()); }
        return new String[]{"Yohanes 3:16", "Karena begitu besar kasih Allah akan dunia ini...", "Kasih Allah"};
    }

    public static List<Map<String, String>> getAllAyatHarian() {
        List<Map<String, String>> list = new ArrayList<>();
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, tanggal, referensi, ayat, keterangan, aktif FROM ayat_harian ORDER BY id")) {
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id",         String.valueOf(rs.getInt("id")));
                m.put("tanggal",    rs.getString("tanggal") != null ? rs.getString("tanggal") : "(rotasi)");
                m.put("referensi",  rs.getString("referensi"));
                m.put("ayat",       rs.getString("ayat"));
                m.put("keterangan", rs.getString("keterangan") != null ? rs.getString("keterangan") : "");
                m.put("aktif",      rs.getInt("aktif") == 1 ? "Ya" : "Tidak");
                list.add(m);
            }
        } catch (SQLException e) { System.out.println("[DB] getAllAyatHarian error: " + e.getMessage()); }
        return list;
    }

    public static void insertAyatHarian(String tanggal, String referensi, String ayat, String keterangan) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO ayat_harian(tanggal,referensi,ayat,keterangan,aktif) VALUES(?,?,?,?,1)")) {
            ps.setString(1, tanggal == null || tanggal.isBlank() ? null : tanggal);
            ps.setString(2, referensi); ps.setString(3, ayat); ps.setString(4, keterangan);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] insertAyatHarian error: " + e.getMessage()); }
    }

    public static void updateAyatHarian(int id, String tanggal, String referensi, String ayat, String keterangan, boolean aktif) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE ayat_harian SET tanggal=?,referensi=?,ayat=?,keterangan=?,aktif=? WHERE id=?")) {
            ps.setString(1, tanggal == null || tanggal.isBlank() ? null : tanggal);
            ps.setString(2, referensi); ps.setString(3, ayat); ps.setString(4, keterangan);
            ps.setInt(5, aktif ? 1 : 0); ps.setInt(6, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] updateAyatHarian error: " + e.getMessage()); }
    }

    public static void deleteAyatHarian(int id) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM ayat_harian WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] deleteAyatHarian error: " + e.getMessage()); }
    }

    public static int countAyatHarian() {
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ayat_harian")) {
            return rs.getInt(1);
        } catch (SQLException e) { return 0; }
    }

    // ══════════════════════════════════════════════════════════
    //  ADMIN USERS
    // ══════════════════════════════════════════════════════════

    public static String validateAdmin(String username, String password) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT nama_lengkap FROM admin_users WHERE username=? AND password_hash=?")) {
            ps.setString(1, username); ps.setString(2, sha256(password));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nama_lengkap");
        } catch (SQLException e) { System.out.println("[DB] validateAdmin error: " + e.getMessage()); }
        return null;
    }

    public static boolean resetPassword(String username, String newPassword) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE admin_users SET password_hash=? WHERE username=?")) {
            ps.setString(1, sha256(newPassword)); ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.out.println("[DB] resetPassword error: " + e.getMessage()); }
        return false;
    }

    public static boolean isUsernameExists(String username) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM admin_users WHERE username=?")) {
            ps.setString(1, username);
            return ps.executeQuery().getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    public static void insertAdmin(String username, String password, String namaLengkap) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO admin_users(username,password_hash,nama_lengkap) VALUES(?,?,?)")) {
            ps.setString(1, username); ps.setString(2, sha256(password));
            ps.setString(3, namaLengkap); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] insertAdmin error: " + e.getMessage()); }
    }

    public static void updateAdmin(int id, String username, String newPassword, String namaLengkap) {
        try {
            if (newPassword != null && !newPassword.isEmpty()) {
                try (PreparedStatement ps = getConnection().prepareStatement(
                        "UPDATE admin_users SET username=?,password_hash=?,nama_lengkap=? WHERE id=?")) {
                    ps.setString(1, username); ps.setString(2, sha256(newPassword));
                    ps.setString(3, namaLengkap); ps.setInt(4, id); ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = getConnection().prepareStatement(
                        "UPDATE admin_users SET username=?,nama_lengkap=? WHERE id=?")) {
                    ps.setString(1, username); ps.setString(2, namaLengkap);
                    ps.setInt(3, id); ps.executeUpdate();
                }
            }
        } catch (SQLException e) { System.out.println("[DB] updateAdmin error: " + e.getMessage()); }
    }

    public static void deleteAdmin(int id) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM admin_users WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] deleteAdmin error: " + e.getMessage()); }
    }

    public static List<Map<String, String>> getAllAdmin() {
        List<Map<String, String>> list = new ArrayList<>();
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id,username,nama_lengkap,created_at FROM admin_users ORDER BY id")) {
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id",           String.valueOf(rs.getInt("id")));
                m.put("username",     rs.getString("username"));
                m.put("nama_lengkap", rs.getString("nama_lengkap"));
                m.put("created_at",   rs.getString("created_at"));
                list.add(m);
            }
        } catch (SQLException e) { System.out.println("[DB] getAllAdmin error: " + e.getMessage()); }
        return list;
    }

    // ══════════════════════════════════════════════════════════
    //  INFO GEREJA
    // ══════════════════════════════════════════════════════════

    public static String getInfo(String kunci) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT nilai FROM info_gereja WHERE kunci=?")) {
            ps.setString(1, kunci);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nilai");
        } catch (SQLException e) { System.out.println("[DB] getInfo error: " + e.getMessage()); }
        return "-";
    }

    public static List<Map<String, String>> getAllInfoGereja() {
        List<Map<String, String>> list = new ArrayList<>();
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT id,kunci,nilai FROM info_gereja ORDER BY id")) {
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id",    String.valueOf(rs.getInt("id")));
                m.put("kunci", rs.getString("kunci"));
                m.put("nilai", rs.getString("nilai"));
                list.add(m);
            }
        } catch (SQLException e) { System.out.println("[DB] getAllInfoGereja error: " + e.getMessage()); }
        return list;
    }

    public static void insertInfoGereja(String kunci, String nilai) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT OR REPLACE INTO info_gereja(kunci,nilai) VALUES(?,?)")) {
            ps.setString(1, kunci); ps.setString(2, nilai); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] insertInfoGereja error: " + e.getMessage()); }
    }

    public static void updateInfoGereja(int id, String kunci, String nilai) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE info_gereja SET kunci=?,nilai=? WHERE id=?")) {
            ps.setString(1, kunci); ps.setString(2, nilai); ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] updateInfoGereja error: " + e.getMessage()); }
    }

    public static void deleteInfoGereja(int id) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM info_gereja WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] deleteInfoGereja error: " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  PELAYANAN (syarat_dokumen + biaya_layanan)
    // ══════════════════════════════════════════════════════════

    public static List<Map<String, String>> getAllPelayanan() {
        List<Map<String, String>> list = new ArrayList<>();
        list.addAll(querySyarat(null));
        list.addAll(queryBiaya(null));
        return list;
    }

    public static List<Map<String, String>> getPelayananByKategori(String kategori) {
        List<Map<String, String>> list = new ArrayList<>();
        list.addAll(querySyarat(kategori));
        list.addAll(queryBiaya(kategori));
        return list;
    }

    private static List<Map<String, String>> querySyarat(String kategori) {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = kategori != null
                ? "SELECT id,kategori,dokumen FROM syarat_dokumen WHERE kategori=? ORDER BY kategori,nomor_urut"
                : "SELECT id,kategori,dokumen FROM syarat_dokumen ORDER BY kategori,nomor_urut";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            if (kategori != null) ps.setString(1, kategori);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id",         String.valueOf(rs.getInt("id")));
                m.put("kategori",   rs.getString("kategori"));
                m.put("tipe",       "SYARAT_DOKUMEN");
                m.put("nama",       rs.getString("dokumen"));
                m.put("biaya",      "-");
                m.put("keterangan", "");
                list.add(m);
            }
        } catch (SQLException e) { System.out.println("[DB] querySyarat error: " + e.getMessage()); }
        return list;
    }

    private static List<Map<String, String>> queryBiaya(String kategori) {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = kategori != null
                ? "SELECT id,kategori,nama_item,biaya,keterangan FROM biaya_layanan WHERE kategori=? ORDER BY kategori"
                : "SELECT id,kategori,nama_item,biaya,keterangan FROM biaya_layanan ORDER BY kategori";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            if (kategori != null) ps.setString(1, kategori);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id",         String.valueOf(rs.getInt("id")));
                m.put("kategori",   rs.getString("kategori"));
                m.put("tipe",       "BIAYA_LAYANAN");
                m.put("nama",       rs.getString("nama_item"));
                m.put("biaya",      rs.getString("biaya"));
                m.put("keterangan", rs.getString("keterangan") != null ? rs.getString("keterangan") : "");
                list.add(m);
            }
        } catch (SQLException e) { System.out.println("[DB] queryBiaya error: " + e.getMessage()); }
        return list;
    }

    public static void insertPelayanan(String tipe, String kategori, String nama, String biaya, String ket) {
        if ("SYARAT_DOKUMEN".equals(tipe)) {
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO syarat_dokumen(kategori,nomor_urut,dokumen) VALUES(?,?,?)")) {
                int max = maxNomorUrut(kategori);
                ps.setString(1, kategori); ps.setInt(2, max + 1); ps.setString(3, nama);
                ps.executeUpdate();
            } catch (SQLException e) { System.out.println("[DB] insertPelayanan error: " + e.getMessage()); }
        } else {
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "INSERT INTO biaya_layanan(kategori,nama_item,biaya,keterangan) VALUES(?,?,?,?)")) {
                ps.setString(1, kategori); ps.setString(2, nama);
                ps.setString(3, biaya != null ? biaya : "Gratis");
                ps.setString(4, ket != null ? ket : "");
                ps.executeUpdate();
            } catch (SQLException e) { System.out.println("[DB] insertBiaya error: " + e.getMessage()); }
        }
    }

    private static int maxNomorUrut(String kategori) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT COALESCE(MAX(nomor_urut),0) FROM syarat_dokumen WHERE kategori=?")) {
            ps.setString(1, kategori);
            return ps.executeQuery().getInt(1);
        } catch (SQLException e) { return 0; }
    }

    public static void updatePelayanan(int id, String tipe, String kategori, String nama, String biaya, String ket) {
        if ("SYARAT_DOKUMEN".equals(tipe)) {
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "UPDATE syarat_dokumen SET kategori=?,dokumen=? WHERE id=?")) {
                ps.setString(1, kategori); ps.setString(2, nama); ps.setInt(3, id);
                ps.executeUpdate();
            } catch (SQLException e) { System.out.println("[DB] updateSyarat error: " + e.getMessage()); }
        } else {
            try (PreparedStatement ps = getConnection().prepareStatement(
                    "UPDATE biaya_layanan SET kategori=?,nama_item=?,biaya=?,keterangan=? WHERE id=?")) {
                ps.setString(1, kategori); ps.setString(2, nama);
                ps.setString(3, biaya != null ? biaya : "Gratis");
                ps.setString(4, ket != null ? ket : ""); ps.setInt(5, id);
                ps.executeUpdate();
            } catch (SQLException e) { System.out.println("[DB] updateBiaya error: " + e.getMessage()); }
        }
    }

    public static void deletePelayanan(int id, String tipe) {
        String tbl = "SYARAT_DOKUMEN".equals(tipe) ? "syarat_dokumen" : "biaya_layanan";
        try (PreparedStatement ps = getConnection().prepareStatement("DELETE FROM " + tbl + " WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] deletePelayanan error: " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  KATA KUNCI Q&A
    // ══════════════════════════════════════════════════════════

    public static void insertKataKunci(String kataKunci, String pertanyaan, String jawaban, String kategori) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT OR IGNORE INTO kata_kunci_qa(kata_kunci,pertanyaan_asli,jawaban,kategori) VALUES(?,?,?,?)")) {
            ps.setString(1, kataKunci.toLowerCase().trim());
            ps.setString(2, pertanyaan); ps.setString(3, jawaban);
            ps.setString(4, kategori != null ? kategori : "UMUM");
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] insertKataKunci error: " + e.getMessage()); }
    }

    public static void updateKataKunci(int id, String kataKunci, String pertanyaan, String jawaban, String kategori) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE kata_kunci_qa SET kata_kunci=?,pertanyaan_asli=?,jawaban=?,kategori=?," +
                        "updated_at=datetime('now','localtime') WHERE id=?")) {
            ps.setString(1, kataKunci.toLowerCase().trim()); ps.setString(2, pertanyaan);
            ps.setString(3, jawaban); ps.setString(4, kategori != null ? kategori : "UMUM");
            ps.setInt(5, id); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] updateKataKunci error: " + e.getMessage()); }
    }

    public static void deleteKataKunci(int id) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM kata_kunci_qa WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] deleteKataKunci error: " + e.getMessage()); }
    }

    public static boolean isKataKunciExists(String kataKunci) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM kata_kunci_qa WHERE kata_kunci=?")) {
            ps.setString(1, kataKunci.toLowerCase().trim());
            return ps.executeQuery().getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    public static List<Map<String, String>> searchKataKunci(String kategori, String cari) {
        List<Map<String, String>> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id,kata_kunci,pertanyaan_asli,jawaban,kategori,hit_count,created_at " +
                        "FROM kata_kunci_qa WHERE 1=1");
        if (kategori != null) sql.append(" AND kategori=?");
        if (cari     != null) sql.append(" AND (kata_kunci LIKE ? OR pertanyaan_asli LIKE ?)");
        sql.append(" ORDER BY hit_count DESC, id DESC");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            int idx = 1;
            if (kategori != null) ps.setString(idx++, kategori);
            if (cari     != null) {
                String like = "%" + cari + "%";
                ps.setString(idx++, like); ps.setString(idx, like);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("id",              String.valueOf(rs.getInt("id")));
                m.put("kata_kunci",      rs.getString("kata_kunci"));
                m.put("pertanyaan_asli", rs.getString("pertanyaan_asli") != null ? rs.getString("pertanyaan_asli") : "");
                m.put("jawaban",         rs.getString("jawaban"));
                m.put("kategori",        rs.getString("kategori"));
                m.put("hit_count",       String.valueOf(rs.getInt("hit_count")));
                list.add(m);
            }
        } catch (SQLException e) { System.out.println("[DB] searchKataKunci error: " + e.getMessage()); }
        return list;
    }

    /**
     * Pencarian kata kunci CERDAS:
     * 1. Cari exact match
     * 2. Cari input mengandung kata kunci (atau sebaliknya)
     * 3. Cari kata per kata dari input yang cocok dengan kata kunci
     */
    public static String cariJawabanDariKataKunci(String inputUser) {

        if (inputUser == null || inputUser.isBlank())
            return null;

        String lower = inputUser.toLowerCase().trim();

        String bestAnswer = null;
        int bestScore = 0;

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id, kata_kunci, jawaban FROM kata_kunci_qa")) {

            while (rs.next()) {

                String kk = rs.getString("kata_kunci")
                        .toLowerCase()
                        .trim();

                String jawaban = rs.getString("jawaban");

                int score = 0;

                // EXACT MATCH
                if (lower.equals(kk)) {
                    score = 100;
                }

                // PHRASE MATCH
                else if (lower.contains(kk) && kk.length() >= 5) {
                    score = 80;
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestAnswer = jawaban;
                }
            }

        } catch (SQLException e) {
            System.out.println("[DB] cariJawaban error: " + e.getMessage());
        }

        return bestScore >= 80 ? bestAnswer : null;
    }

    private static void incrementHitCount(int id) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE kata_kunci_qa SET hit_count=hit_count+1 WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] incrementHitCount error: " + e.getMessage()); }
    }

    public static int countKataKunci() {
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM kata_kunci_qa")) {
            return rs.getInt(1);
        } catch (SQLException e) { return 0; }
    }

    // ══════════════════════════════════════════════════════════
    //  EKSTRAKSI & DETEKSI KATEGORI
    // ══════════════════════════════════════════════════════════

    public static String ekstrakKataKunci(String kalimat) {
        if (kalimat == null || kalimat.isBlank()) return null;
        Set<String> stopWords = Set.of(
                "apa", "apakah", "bagaimana", "berapa", "kapan", "dimana", "siapa",
                "saya", "aku", "kita", "kamu", "anda", "dia", "mereka",
                "adalah", "ada", "tidak", "bisa", "mau", "ingin", "tolong",
                "yang", "di", "ke", "dari", "dengan", "untuk", "dan", "atau", "tapi",
                "ini", "itu", "nih", "dong", "ya", "yuk", "gimana", "gak", "nggak",
                "tentang", "mengenai", "soal", "info", "informasi", "tanya", "mau tanya",
                "halo", "hi", "hello", "shalom", "selamat", "boleh", "mohon", "minta"
        );
        String lower = kalimat.toLowerCase()
                .replaceAll("[?!.,;:]", "")
                .replaceAll("\\s+", " ")
                .trim();
        String[] words = lower.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!stopWords.contains(w) && w.length() > 1) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(w);
            }
        }
        String kk = sb.toString().trim();
        return kk.isEmpty() ? null : kk;
    }

    public static String deteksiKategori(String kalimat) {
        if (kalimat == null) return "UMUM";
        String lower = kalimat.toLowerCase();
        if (lower.matches(".*\\b(baptis|baptisan|pembaptisan|dibaptis)\\b.*")) return "BAPTIS";
        if (lower.matches(".*\\b(nikah|pernikahan|menikah|kawin|pemberkatan|wedding)\\b.*")) return "PERNIKAHAN";
        if (lower.matches(".*\\b(sidi|peneguhan|katekisasi)\\b.*")) return "SIDI";
        if (lower.matches(".*\\b(konseling|konsultasi|bimbingan|pastoral)\\b.*")) return "KONSELING";
        return "UMUM";
    }

    // ══════════════════════════════════════════════════════════
    //  SYARAT & BIAYA (akses langsung)
    // ══════════════════════════════════════════════════════════

    public static List<String> getSyarat(String kategori) {
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT dokumen FROM syarat_dokumen WHERE kategori=? ORDER BY nomor_urut")) {
            ps.setString(1, kategori.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString("dokumen"));
        } catch (SQLException e) { System.out.println("[DB] getSyarat error: " + e.getMessage()); }
        return list;
    }

    public static List<String[]> getBiaya(String kategori) {
        List<String[]> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT nama_item,biaya,keterangan FROM biaya_layanan WHERE kategori=?")) {
            ps.setString(1, kategori.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new String[]{rs.getString("nama_item"), rs.getString("biaya"), rs.getString("keterangan")});
        } catch (SQLException e) { System.out.println("[DB] getBiaya error: " + e.getMessage()); }
        return list;
    }

    // ══════════════════════════════════════════════════════════
    //  RIWAYAT CHAT
    // ══════════════════════════════════════════════════════════

    public static void simpanChat(String pengirim, String pesan) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO riwayat_chat(pengirim,pesan) VALUES(?,?)")) {
            ps.setString(1, pengirim); ps.setString(2, pesan); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] simpanChat error: " + e.getMessage()); }
    }

    public static List<String[]> getRiwayatChat(int limit) {
        List<String[]> list = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(
                "SELECT pengirim,pesan,waktu FROM riwayat_chat ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new String[]{rs.getString("pengirim"), rs.getString("pesan"), rs.getString("waktu")});
        } catch (SQLException e) { System.out.println("[DB] getRiwayatChat error: " + e.getMessage()); }
        return list;
    }

    public static int countRiwayatChat() {
        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM riwayat_chat")) {
            return rs.getInt(1);
        } catch (SQLException e) { return 0; }
    }

    public static void hapusRiwayat() {
        try (Statement st = getConnection().createStatement()) {
            st.execute("DELETE FROM riwayat_chat");
        } catch (SQLException e) { System.out.println("[DB] hapusRiwayat error: " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  JADWAL
    // ══════════════════════════════════════════════════════════

    public static void tambahJadwal(String nama, String kategori, String tanggal,
                                    String jamMulai, String jamSelesai, String lokasi) {
        try (PreparedStatement ps = getConnection().prepareStatement(
                "INSERT INTO jadwal(nama_kegiatan,kategori,tanggal,jam_mulai,jam_selesai,lokasi) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, nama); ps.setString(2, kategori); ps.setString(3, tanggal);
            ps.setString(4, jamMulai); ps.setString(5, jamSelesai); ps.setString(6, lokasi);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("[DB] tambahJadwal error: " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  JADWAL DEFAULT (data contoh untuk Kalender)
    // ══════════════════════════════════════════════════════════

    private static void insertDefaultJadwal(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM jadwal")) {
            if (rs.getInt(1) > 0) return;
        }

        LocalDate today = LocalDate.now();
        int year  = today.getYear();
        int month = today.getMonthValue();

        java.util.function.BiFunction<Integer, Integer, String> d =
                (m, day) -> String.format("%04d-%02d-%02d", year, m, day);

        // Pastikan hari tidak melebihi jumlah hari di bulan
        int maxDay = java.time.YearMonth.of(year, month).lengthOfMonth();

        Object[][] jadwal = {
                {"Ibadah Minggu Pagi",           "IBADAH",     d.apply(month, Math.min(1, maxDay)),  "07:00", "09:00", "Gedung Gereja Utama"},
                {"Ibadah Minggu Siang",          "IBADAH",     d.apply(month, Math.min(1, maxDay)),  "10:30", "12:30", "Gedung Gereja Utama"},
                {"Persekutuan Doa Pemuda",       "PEMUDA",     d.apply(month, Math.min(3, maxDay)),  "18:30", "20:00", "Ruang Serba Guna"},
                {"Rapat Majelis Bulanan",        "INTERNAL",   d.apply(month, Math.min(5, maxDay)),  "09:00", "11:00", "Ruang Rapat Gereja"},
                {"Kelas Katekisasi Dewasa",      "SIDI",       d.apply(month, Math.min(6, maxDay)),  "16:00", "18:00", "Ruang Kelas A"},
                {"Ibadah Minggu Pagi",           "IBADAH",     d.apply(month, Math.min(8, maxDay)),  "07:00", "09:00", "Gedung Gereja Utama"},
                {"Ibadah Minggu Siang",          "IBADAH",     d.apply(month, Math.min(8, maxDay)),  "10:30", "12:30", "Gedung Gereja Utama"},
                {"Pelatihan Paduan Suara",       "MUSIK",      d.apply(month, Math.min(9, maxDay)),  "17:00", "19:00", "Ruang Musik"},
                {"Ibadah Keluarga",              "IBADAH",     d.apply(month, Math.min(10, maxDay)), "18:30", "20:00", "Gedung Gereja Utama"},
                {"Persekutuan Ibu-Ibu",          "PERSEKUTUAN",d.apply(month, Math.min(11, maxDay)), "09:00", "11:00", "Aula Gereja"},
                {"Pendaftaran Baptis Anak",      "BAPTIS",     d.apply(month, Math.min(12, maxDay)), "09:00", "12:00", "Sekretariat"},
                {"Kelas Katekisasi Dewasa",      "SIDI",       d.apply(month, Math.min(13, maxDay)), "16:00", "18:00", "Ruang Kelas A"},
                {"Ibadah Minggu Pagi",           "IBADAH",     d.apply(month, Math.min(15, maxDay)), "07:00", "09:00", "Gedung Gereja Utama"},
                {"Ibadah Minggu Siang",          "IBADAH",     d.apply(month, Math.min(15, maxDay)), "10:30", "12:30", "Gedung Gereja Utama"},
                {"Seminar Keluarga Kristen",     "SEMINAR",    d.apply(month, Math.min(16, maxDay)), "09:00", "15:00", "Aula Gereja"},
                {"Persekutuan Doa Pemuda",       "PEMUDA",     d.apply(month, Math.min(17, maxDay)), "18:30", "20:00", "Ruang Serba Guna"},
                {"Latihan Drama",                "MUSIK",      d.apply(month, Math.min(18, maxDay)), "15:00", "18:00", "Aula Gereja"},
                {"Pelayanan Masyarakat",         "SOSIAL",     d.apply(month, Math.min(20, maxDay)), "08:00", "13:00", "Panti Asuhan Kasih"},
                {"Kelas Katekisasi Pemuda",      "SIDI",       d.apply(month, Math.min(20, maxDay)), "16:00", "18:00", "Ruang Kelas B"},
                {"Ibadah Minggu Pagi",           "IBADAH",     d.apply(month, Math.min(22, maxDay)), "07:00", "09:00", "Gedung Gereja Utama"},
                {"Ibadah Minggu Siang",          "IBADAH",     d.apply(month, Math.min(22, maxDay)), "10:30", "12:30", "Gedung Gereja Utama"},
                {"Konseling Pastoral",           "KONSELING",  d.apply(month, Math.min(23, maxDay)), "09:00", "16:00", "Ruang Konseling"},
                {"Rapat Panitia",                "INTERNAL",   d.apply(month, Math.min(24, maxDay)), "18:00", "20:00", "Ruang Rapat Gereja"},
                {"Pemberkatan Pernikahan",       "PERNIKAHAN", d.apply(month, Math.min(25, maxDay)), "10:00", "12:00", "Gedung Gereja Utama"},
                {"Ibadah Padang Pemuda",         "PEMUDA",     d.apply(month, Math.min(26, maxDay)), "07:00", "15:00", "Kaliurang"},
                {"Pendalaman Alkitab Sel",       "IBADAH",     d.apply(month, Math.min(27, maxDay)), "19:00", "20:30", "Rumah Jemaat"},
                {"Ibadah Minggu Pagi",           "IBADAH",     d.apply(month, Math.min(29, maxDay)), "07:00", "09:00", "Gedung Gereja Utama"},
                {"Pelantikan Pengurus Baru",     "INTERNAL",   d.apply(month, Math.min(29, maxDay)), "10:00", "12:00", "Aula Gereja"},
        };

        // Bulan depan
        int nm = month == 12 ? 1  : month + 1;
        int ny = month == 12 ? year + 1 : year;
        int maxDayNext = java.time.YearMonth.of(ny, nm).lengthOfMonth();
        java.util.function.BiFunction<Integer, Integer, String> dn =
                (m, day) -> String.format("%04d-%02d-%02d", ny, m, day);

        Object[][] jadwalNext = {
                {"Ibadah Awal Bulan",        "IBADAH",  dn.apply(nm, Math.min(1, maxDayNext)),  "07:00", "09:00", "Gedung Gereja Utama"},
                {"Pendaftaran SIDI Baru",    "SIDI",    dn.apply(nm, Math.min(3, maxDayNext)),  "09:00", "12:00", "Sekretariat"},
                {"Ibadah Minggu Pagi",       "IBADAH",  dn.apply(nm, Math.min(5, maxDayNext)),  "07:00", "09:00", "Gedung Gereja Utama"},
                {"Retret Keluarga Gereja",   "SEMINAR", dn.apply(nm, Math.min(10, maxDayNext)), "08:00", "17:00", "Hotel Merbabu"},
                {"Pendaftaran Baptis Dewasa","BAPTIS",  dn.apply(nm, Math.min(15, maxDayNext)), "09:00", "12:00", "Sekretariat"},
        };

        String sql = "INSERT INTO jadwal(nama_kegiatan,kategori,tanggal,jam_mulai,jam_selesai,lokasi) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[][] arr : new Object[][][] { jadwal, jadwalNext }) {
                for (Object[] row : arr) {
                    ps.setString(1, (String) row[0]); ps.setString(2, (String) row[1]);
                    ps.setString(3, (String) row[2]); ps.setString(4, (String) row[3]);
                    ps.setString(5, (String) row[4]); ps.setString(6, (String) row[5]);
                    ps.executeUpdate();
                }
            }
        }
        System.out.println("[DB] Data jadwal default berhasil diisi.");
    }

    // ══════════════════════════════════════════════════════════
    //  GET JADWAL PER BULAN (digunakan oleh CalendarViewController)
    // ══════════════════════════════════════════════════════════

    /**
     * Mengembalikan semua jadwal dalam rentang tanggal (format yyyy-MM-dd).
     * Return: Map< "yyyy-MM-dd", List<Map<field, value>> >
     */
    public static Map<String, List<Map<String, String>>> getJadwalBulan(
            String startDate, String endDate) {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        String sql = "SELECT nama_kegiatan, kategori, tanggal, jam_mulai, jam_selesai, lokasi " +
                "FROM jadwal WHERE tanggal >= ? AND tanggal <= ? ORDER BY tanggal, jam_mulai";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String tgl = rs.getString("tanggal");
                Map<String, String> ev = new LinkedHashMap<>();
                ev.put("nama_kegiatan", rs.getString("nama_kegiatan"));
                ev.put("kategori",      rs.getString("kategori"));
                ev.put("tanggal",       tgl);
                ev.put("jam_mulai",     rs.getString("jam_mulai")   != null ? rs.getString("jam_mulai")   : "");
                ev.put("jam_selesai",   rs.getString("jam_selesai") != null ? rs.getString("jam_selesai") : "");
                ev.put("lokasi",        rs.getString("lokasi")      != null ? rs.getString("lokasi")      : "");
                result.computeIfAbsent(tgl, k -> new ArrayList<>()).add(ev);
            }
        } catch (SQLException e) {
            System.out.println("[DB] getJadwalBulan error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Hapus semua jadwal (untuk admin reset data).
     */
    public static void hapusSemuaJadwal() {
        try (Statement st = getConnection().createStatement()) {
            st.execute("DELETE FROM jadwal");
        } catch (SQLException e) { System.out.println("[DB] hapusSemuaJadwal error: " + e.getMessage()); }
    }
    public static void tambahVariasiIbadah() {

        String jawaban = """
        Jadwal Ibadah Minggu:

        07.00 WIB
        09.30 WIB
        17.00 WIB
        """;

        String[] keywords = {
                "jadwal ibadah",
                "ibadah minggu",
                "kebaktian minggu",
                "jadwal gereja",
                "ibadah hari minggu",
                "jadwal minggu depan"
        };

        for (String k : keywords) {
            insertKataKunci(k, k, jawaban, "UMUM");
        }
    }
}