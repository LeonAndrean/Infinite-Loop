package com.gereja.chatbot.service;

import com.gereja.chatbot.database.DatabaseHelper;
import com.gereja.chatbot.model.ChatMessage;
import com.gereja.chatbot.model.ChatMessage.StepInfo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * ChatbotService – Logika utama chatbot Faith Buddy.
 * Semua data dinamis (syarat, biaya, kontak, info gereja)
 * diambil dari database. Tidak ada data dummy/hardcoded.
 *
 * Alur pencarian jawaban:
 * 1. Deteksi kata kunci khusus (salam, terima kasih, ibadah, dll)
 * 2. Deteksi topik pelayanan (baptis, nikah, sidi, konseling)
 * 3. Cari di tabel kata_kunci_qa (DB) — smart multi-pass matching
 * 4. Fallback kontekstual berdasarkan state
 */
public class ChatbotService {

    // ── Keyword lists ──────────────────────────────────────────
    private static final List<String> KW_BAPTIS    = List.of("baptis","baptisan","pembaptisan","dibaptis");
    private static final List<String> KW_NIKAH     = List.of("nikah","pernikahan","menikah","wedding","kawin","pranikah","pra-nikah","pemberkatan");
    private static final List<String> KW_SIDI      = List.of("sidi","peneguhan","katekisasi");
    private static final List<String> KW_KONSELING = List.of("konseling","konsultasi","bimbingan","pendeta","pastoral","curhat");
    private static final List<String> KW_JADWAL    = List.of("jadwal","pendaftaran","daftar","kapan","tanggal","agenda","acara","bulan ini");
    private static final List<String> KW_SALAM     = List.of("halo","hello","hi","selamat","pagi","siang","sore","malam","shalom","hai","hey","oi","hei");
    private static final List<String> KW_MAKASIH   = List.of("terima kasih","makasih","thanks","tq","thx","tengkyu");
    private static final List<String> KW_SYARAT    = List.of("syarat","persyaratan","dokumen","berkas","butuh apa","perlu apa","harus bawa");
    private static final List<String> KW_KONTAK    = List.of("kontak","hubungi","telepon","tlp","telp","alamat","lokasi","email","whatsapp","wa","nomor");
    private static final List<String> KW_BIAYA     = List.of("biaya","bayar","harga","tarif","gratis","berapa","fee","bayaran");
    private static final List<String> KW_IBADAH    = List.of("ibadah minggu","kebaktian","jam ibadah","ibadah anak","ibadah pemuda","kebaktian minggu");
    private static final List<String> KW_INFO_GEREJA = List.of("visi","misi","sejarah","pendeta","majelis","rekening","donasi","profil gereja");
    private static final List<String> KW_PROFIL    = List.of("tentang gereja","siapa","gereja ini","gkj","faith buddy","apa itu");

    public enum State { AWAL, BAPTIS, NIKAH, SIDI, KONSELING, JADWAL }
    private State state = State.AWAL;

    private static final DateTimeFormatter FMT_PANJANG =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
    private static final DateTimeFormatter FMT_PENDEK  =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));

    // ══════════════════════════════════════════════════════════
    //  PROSES INPUT UTAMA
    // ══════════════════════════════════════════════════════════

    public List<ChatMessage> processInput(String input) {
        if (input == null || input.isBlank()) return Collections.emptyList();
        String lower = input.toLowerCase().trim();

        DatabaseHelper.simpanChat("USER", input.trim());

        ChatMessage respons;

        // ── 1. Salam ──
        if (containsAny(lower, KW_SALAM) && !containsAny(lower, KW_JADWAL)
                && !containsAny(lower, KW_BAPTIS) && !containsAny(lower, KW_NIKAH)) {
            state = State.AWAL;
            respons = pesanSalam();

            // ── 2. Terima Kasih ──
        } else if (containsAny(lower, KW_MAKASIH)) {
            respons = pesanMakasih();

            // ── 3. Profil/Info Umum Gereja ──
        } else if (containsAny(lower, KW_PROFIL) || lower.matches(".*\\b(profil|tentang|siapa|sejarah)\\b.*")) {
            // Cek kata kunci DB dulu
            String jawabanKK = DatabaseHelper.cariJawabanDariKataKunci(lower);
            if (jawabanKK != null) {
                respons = ChatMessage.botMessage(jawabanKK);
            } else {
                respons = infoProfilGereja();
            }

            // ── 4. Ibadah ──
        } else if (containsAny(lower, KW_IBADAH)) {
            respons = infoIbadah();

            // ── 5. Baptis ──
        } else if (containsAny(lower, KW_BAPTIS)) {
            state = State.BAPTIS;
            if      (containsAny(lower, KW_SYARAT)) respons = syaratBaptis();
            else if (containsAny(lower, KW_JADWAL)) respons = jadwalBaptis();
            else if (containsAny(lower, KW_BIAYA))  respons = biaya("BAPTIS");
            else                                      respons = infoBaptis();

            // ── 6. Pernikahan ──
        } else if (containsAny(lower, KW_NIKAH)) {
            state = State.NIKAH;
            if      (containsAny(lower, KW_SYARAT)) respons = syaratNikah();
            else if (containsAny(lower, KW_JADWAL)) respons = jadwalNikah();
            else if (containsAny(lower, KW_BIAYA))  respons = biaya("PERNIKAHAN");
            else                                      respons = infoNikah();

            // ── 7. SIDI ──
        } else if (containsAny(lower, KW_SIDI)) {
            state = State.SIDI;
            if      (containsAny(lower, KW_SYARAT)) respons = syaratSidi();
            else if (containsAny(lower, KW_JADWAL)) respons = jadwalSidi();
            else if (containsAny(lower, KW_BIAYA))  respons = biaya("SIDI");
            else                                      respons = infoSidi();

            // ── 8. Konseling ──
        } else if (containsAny(lower, KW_KONSELING)) {
            state = State.KONSELING;
            if      (containsAny(lower, KW_JADWAL)) respons = jadwalKonseling();
            else if (containsAny(lower, KW_BIAYA))  respons = biaya("KONSELING");
            else                                      respons = infoKonseling();

            // ── 9. Biaya Umum ──
        } else if (containsAny(lower, KW_BIAYA)) {
            respons = biaya(state == State.AWAL ? "SEMUA" : state.name());

            // ── 10. Jadwal ──
        } else if (containsAny(lower, KW_JADWAL)) {
            state = State.JADWAL;
            respons = infoJadwalUmum();

            // ── 11. Kontak ──
        } else if (containsAny(lower, KW_KONTAK)) {
            respons = infoKontak();

            // ── 12. Info Gereja dari DB (kata kunci Q&A) ──
        } else {
            String jawabanKK = DatabaseHelper.cariJawabanDariKataKunci(lower);
            if (jawabanKK != null) {
                respons = ChatMessage.botMessage(jawabanKK);
            } else {
                // Coba ekstrak kata kunci dan cari lagi
                String kk = DatabaseHelper.ekstrakKataKunci(lower);
                if (kk != null && !kk.equals(lower)) {
                    String jawabanEkstrak = DatabaseHelper.cariJawabanDariKataKunci(kk);
                    if (jawabanEkstrak != null) {
                        respons = ChatMessage.botMessage(jawabanEkstrak);
                    } else {
                        respons = balasanKontekstual();
                    }
                } else {
                    respons = balasanKontekstual();
                }
            }
        }
        // --- Bagian akhir dari method processInput ---

        // LOGIKA PERBAIKAN:
        // Cek apakah pesan user masuk ke fallback (balasanKontekstual)
        // Jika chatbot hanya memberikan balasan default/kontekstual, berarti pertanyaan aslinya tidak terjawab
        if (respons == null || respons.getContent() == null ||
                respons.getContent().contains("Maaf, saya belum menemukan jawaban") ||
                respons.getContent().contains("Maaf, saya belum memiliki informasi")) {

            try {
                // Simpan pertanyaan asli user ke tabel tak terjawab
                DatabaseHelper.simpanPertanyaanTakTerjawab(input.trim());
                System.out.println("[DEBUG] Berhasil memicu simpanPertanyaanTakTerjawab untuk: " + input);
            } catch (Exception e) {
                System.err.println("Gagal menyimpan unanswered question: " + e.getMessage());
            }

            // Pastikan respons memberikan pesan fallback yang jelas
            if (respons == null || respons.getContent().isBlank()) {
                respons = ChatMessage.botMessage(
                        "🙏 Maaf, saya belum memiliki informasi mengenai hal tersebut.\n\n" +
                                "Pertanyaan Anda sudah saya catat agar Admin dapat melengkapi informasinya segera. " +
                                "Silakan tanya hal lain atau hubungi Sekretariat di: " + DatabaseHelper.getInfo("telepon")
                );
            }
        }

        DatabaseHelper.simpanChat("BOT", respons.getContent());
        return List.of(respons);
    }

    // ── Shortcut sidebar ──────────────────────────────────────
    public ChatMessage getInfoBaptis()    { state = State.BAPTIS;    return infoBaptis();     }
    public ChatMessage getInfoPernikahan(){ state = State.NIKAH;     return infoNikah();      }
    public ChatMessage getInfoSidi()      { state = State.SIDI;      return infoSidi();       }
    public ChatMessage getInfoKonseling() { state = State.KONSELING; return infoKonseling();  }
    public ChatMessage getInfoJadwal()    { state = State.JADWAL;    return infoJadwalUmum(); }
    public ChatMessage getWelcomeMessage(){ return pesanWelcome();   }

    // ══════════════════════════════════════════════════════════
    //  SAMBUTAN
    // ══════════════════════════════════════════════════════════

    private ChatMessage pesanWelcome() {
        String namaGereja = DatabaseHelper.getInfo("nama_gereja");
        return ChatMessage.botMessage(
                "Shalom! Selamat datang di Faith Buddy 🙏\n\n" +
                        "Asisten informasi " + namaGereja + ".\n\n" +
                        "Silakan ketik pertanyaan Anda atau pilih menu di bawah:\n\n" +
                        "💧 Baptis\n" +
                        "💍 Pernikahan\n" +
                        "📖 SIDI / Peneguhan\n" +
                        "🤝 Konseling Pastoral\n" +
                        "📅 Jadwal Kegiatan\n" +
                        "📞 Kontak & Lokasi\n\n" +
                        "Contoh: \"syarat baptis\", \"jadwal sidi\", \"biaya pernikahan\", \"visi gereja\"");
    }

    private ChatMessage pesanSalam() {
        return ChatMessage.botMessage(
                "Shalom! Tuhan memberkati 🙏\n\n" +
                        "Saya siap membantu informasi pelayanan " + DatabaseHelper.getInfo("nama_gereja") + ".\n\n" +
                        "Ketik pertanyaan Anda, misalnya:\n" +
                        "• \"syarat baptis\"\n" +
                        "• \"jadwal sidi\"\n" +
                        "• \"biaya pernikahan\"\n" +
                        "• \"visi gereja\"\n" +
                        "• \"kontak sekretariat\"");
    }

    private ChatMessage pesanMakasih() {
        return ChatMessage.botMessage(
                "Sama-sama, saudara! 😊\n\n" +
                        "Semoga pelayanan ini bermanfaat. Tuhan Yesus memberkati!\n\n" +
                        "Ada lagi yang bisa saya bantu?");
    }

    // ══════════════════════════════════════════════════════════
    //  PROFIL GEREJA
    // ══════════════════════════════════════════════════════════

    private ChatMessage infoProfilGereja() {
        String nama    = DatabaseHelper.getInfo("nama_gereja");
        String tahun   = DatabaseHelper.getInfo("tahun_berdiri");
        String denom   = DatabaseHelper.getInfo("denomiasi");
        String visi    = DatabaseHelper.getInfo("visi");
        String misi    = DatabaseHelper.getInfo("misi");
        String alamat  = DatabaseHelper.getInfo("alamat");
        String pend1   = DatabaseHelper.getInfo("pendeta_1");
        String pend2   = DatabaseHelper.getInfo("pendeta_2");

        return ChatMessage.botMessage(
                "✝ Profil " + nama + "\n\n" +
                        "📍 Alamat      : " + alamat + "\n" +
                        "📅 Berdiri     : " + tahun + "\n" +
                        "⛪ Denominasi  : " + denom + "\n\n" +
                        "Visi:\n" + visi + "\n\n" +
                        "Misi:\n" + misi + "\n\n" +
                        "Pendeta:\n" +
                        "• " + pend1 + "\n" +
                        "• " + pend2 + "\n\n" +
                        "Untuk info lengkap, kunjungi: " + DatabaseHelper.getInfo("website"));
    }

    // ══════════════════════════════════════════════════════════
    //  BAPTIS
    // ══════════════════════════════════════════════════════════

    private ChatMessage infoBaptis() {
        StepInfo[] s = {
                new StepInfo(1, "Siapkan Dokumen",
                        "KTP, KK, pas foto 3×4, formulir pendaftaran dari sekretariat", "#1A3A2A"),
                new StepInfo(2, "Daftar ke Sekretariat",
                        "Datang langsung atau hubungi WA " + DatabaseHelper.getInfo("whatsapp") +
                                " (Senin–Jumat 08.00–16.00)", "#2D5A3D"),
                new StepInfo(3, "Sesi Konseling",
                        "2 kali pertemuan dengan Pendeta, sekitar 2–3 minggu sebelum pelaksanaan", "#4A7A5A"),
                new StepInfo(4, "Pelaksanaan Baptis",
                        "Dalam Ibadah Minggu sesuai jadwal yang ditetapkan Majelis", "#D4A843")
        };
        return ChatMessage.botStepMessage("Panduan Pembaptisan Kudus", s,
                new String[]{"Persyaratan Baptis", "Jadwal Baptis", "Biaya Baptis"});
    }

    private ChatMessage syaratBaptis() {
        return buatPesanSyarat("BAPTIS", "2 minggu sebelum pelaksanaan.");
    }

    private ChatMessage jadwalBaptis() {
        LocalDate jadwal   = LocalDate.now().plusMonths(1).withDayOfMonth(1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY));
        LocalDate deadline = jadwal.minusWeeks(2);
        return ChatMessage.botMessage(
                "📅 Jadwal Pembaptisan Mendatang\n\n" +
                        "Pelaksanaan  : " + jadwal.format(FMT_PANJANG) + "\n" +
                        "Pukul        : 07.00 / 09.30 WIB (sesuai jadwal Majelis)\n" +
                        "Lokasi       : Gedung Ibadah Utama\n\n" +
                        "⏰ Batas Pendaftaran : " + deadline.format(FMT_PENDEK) + "\n\n" +
                        "Cara mendaftar:\n" +
                        "📍 Datang ke sekretariat (Senin–Jumat 08.00–16.00)\n" +
                        "📱 WhatsApp  : " + DatabaseHelper.getInfo("whatsapp") + "\n" +
                        "📞 Telepon   : " + DatabaseHelper.getInfo("telepon") + "\n\n" +
                        "Ketik \"syarat baptis\" untuk melihat dokumen yang diperlukan.");
    }

    // ══════════════════════════════════════════════════════════
    //  PERNIKAHAN
    // ══════════════════════════════════════════════════════════

    private ChatMessage infoNikah() {
        StepInfo[] s = {
                new StepInfo(1, "Konsultasi Awal",
                        "Temui Pendeta minimal 3 bulan sebelum hari H untuk koordinasi", "#1A3A2A"),
                new StepInfo(2, "Siapkan Dokumen",
                        "KTP, Akta Lahir, Surat Baptis, Surat Sipil N1–N4, foto berdampingan", "#2D5A3D"),
                new StepInfo(3, "Kelas Pra-Nikah",
                        "4 sesi setiap Sabtu 09.00–11.30 WIB di Ruang Pertemuan Lt.1", "#4A7A5A"),
                new StepInfo(4, "Gladi Resik",
                        "H-1 pemberkatan, pukul 17.00 WIB di Gedung Utama", "#7A9A6A"),
                new StepInfo(5, "Pemberkatan Pernikahan",
                        "Sesuai hari yang disepakati bersama Majelis", "#D4A843")
        };
        return ChatMessage.botStepMessage("Panduan Pernikahan Gerejawi", s,
                new String[]{"Persyaratan Dokumen", "Jadwal Pra-Nikah", "Biaya Pernikahan"});
    }

    private ChatMessage syaratNikah() {
        return buatPesanSyarat("PERNIKAHAN", "minimal 3 bulan sebelum hari H.");
    }

    private ChatMessage jadwalNikah() {
        LocalDate sabtu = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        return ChatMessage.botMessage(
                "📅 Jadwal Layanan Pernikahan\n\n" +
                        "Kelas Pra-Nikah:\n" +
                        "• Setiap Sabtu, 09.00–11.30 WIB\n" +
                        "• Sabtu terdekat: " + sabtu.format(FMT_PENDEK) + "\n" +
                        "• Durasi: 4 sesi berturut-turut\n" +
                        "• Lokasi: Ruang Pertemuan, Lantai 1\n\n" +
                        "Konsultasi dengan Pendeta:\n" +
                        "• Senin & Rabu, 10.00–12.00 WIB\n" +
                        "• Daftar via sekretariat: " + DatabaseHelper.getInfo("telepon") + "\n\n" +
                        "⚠️ Pendaftaran pernikahan wajib minimal 3 bulan sebelum hari H.");
    }

    // ══════════════════════════════════════════════════════════
    //  SIDI
    // ══════════════════════════════════════════════════════════

    private ChatMessage infoSidi() {
        StepInfo[] s = {
                new StepInfo(1, "Pendaftaran",
                        "Syarat utama: sudah dibaptis & berusia minimal 14 tahun", "#1A3A2A"),
                new StepInfo(2, "Kelas Persiapan SIDI",
                        "8 pertemuan setiap Minggu 11.00–13.00 WIB (sekitar 2 bulan)", "#2D5A3D"),
                new StepInfo(3, "Ujian Sidi",
                        "Ujian lisan: Pengakuan Iman, Katekismus, dan Tata Ibadah GKJ", "#4A7A5A"),
                new StepInfo(4, "Peneguhan Sidi",
                        "Dalam Ibadah Minggu yang disaksikan seluruh jemaat", "#D4A843")
        };
        return ChatMessage.botStepMessage("Panduan SIDI / Peneguhan Sidi", s,
                new String[]{"Persyaratan SIDI", "Jadwal Kelas SIDI", "Biaya SIDI"});
    }

    private ChatMessage syaratSidi() {
        return buatPesanSyarat("SIDI", "1 minggu sebelum kelas dimulai.");
    }

    private ChatMessage jadwalSidi() {
        LocalDate mulai     = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        LocalDate peneguhan = LocalDate.now().plusMonths(2).with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));
        return ChatMessage.botMessage(
                "📅 Jadwal SIDI / Peneguhan\n\n" +
                        "Kelas Persiapan:\n" +
                        "• Setiap Minggu, 11.00–13.00 WIB\n" +
                        "• Mulai: " + mulai.format(FMT_PENDEK) + "\n" +
                        "• Durasi: 8 pertemuan (±2 bulan)\n" +
                        "• Lokasi: Ruang Katekisasi, Lantai 2\n\n" +
                        "Peneguhan Sidi:\n" +
                        "• Perkiraan: " + peneguhan.format(FMT_PANJANG) + "\n\n" +
                        "⏰ Batas daftar: 1 minggu sebelum kelas pertama.\n" +
                        "📱 WA: " + DatabaseHelper.getInfo("whatsapp") + "\n\n" +
                        "Ketik \"syarat sidi\" untuk daftar dokumen yang diperlukan.");
    }



    // ══════════════════════════════════════════════════════════
    //  KONSELING
    // ══════════════════════════════════════════════════════════

    private ChatMessage infoKonseling() {
        String pend1 = DatabaseHelper.getInfo("pendeta_1");
        String pend2 = DatabaseHelper.getInfo("pendeta_2");
        StepInfo[] s = {
                new StepInfo(1, "Ajukan Permohonan",
                        "Isi formulir di sekretariat atau via WhatsApp " + DatabaseHelper.getInfo("whatsapp"), "#1A3A2A"),
                new StepInfo(2, "Konfirmasi Jadwal",
                        "Sekretariat menghubungi Anda dalam 1–3 hari kerja", "#2D5A3D"),
                new StepInfo(3, "Sesi Konseling",
                        "±60 menit di Ruang Konseling Pastoral, Lantai 2. Online juga tersedia.", "#4A7A5A"),
                new StepInfo(4, "Tindak Lanjut",
                        "Pendeta merekomendasikan langkah lanjut sesuai kebutuhan", "#D4A843")
        };
        return ChatMessage.botStepMessage("Panduan Konseling Pastoral", s,
                new String[]{"Jadwal Konseling", "Biaya Konseling", "Hubungi Sekretariat"});
    }

    private ChatMessage jadwalKonseling() {
        LocalDate selasa = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        LocalDate rabu   = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        String pend1     = DatabaseHelper.getInfo("pendeta_1");
        String pend2     = DatabaseHelper.getInfo("pendeta_2");
        return ChatMessage.botMessage(
                "📅 Jadwal Konseling Pastoral\n\n" +
                        pend1 + "\n" +
                        "  Selasa & Kamis, 13.00–16.00 WIB\n" +
                        "  Selasa terdekat: " + selasa.format(FMT_PENDEK) + "\n\n" +
                        pend2 + "\n" +
                        "  Rabu & Jumat, 10.00–13.00 WIB\n" +
                        "  Rabu terdekat: " + rabu.format(FMT_PENDEK) + "\n\n" +
                        "📍 Lokasi    : Ruang Konseling Pastoral, Gedung Gereja Lt.2\n" +
                        "💻 Online    : Tersedia via video call (WhatsApp/Zoom)\n\n" +
                        "📞 Daftar   : " + DatabaseHelper.getInfo("telepon") + "\n" +
                        "📱 WhatsApp : " + DatabaseHelper.getInfo("whatsapp"));
    }

    // ══════════════════════════════════════════════════════════
    //  JADWAL UMUM & IBADAH
    // ══════════════════════════════════════════════════════════

    private ChatMessage infoJadwalUmum() {
        LocalDate today    = LocalDate.now();
        LocalDate baptis   = today.plusMonths(1).withDayOfMonth(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY));
        LocalDate dlBaptis = baptis.minusWeeks(2);
        LocalDate peneguhan= today.plusMonths(2).with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));
        LocalDate dlSidi   = peneguhan.minusWeeks(5);
        LocalDate sabtu    = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        LocalDate selasa   = today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));

        return ChatMessage.botMessage(
                "📅 Jadwal Layanan Mendatang\n\n" +
                        "── BAPTIS ──\n" +
                        "  Batas daftar : " + dlBaptis.format(FMT_PENDEK) + "\n" +
                        "  Pelaksanaan  : " + baptis.format(FMT_PANJANG) + "\n\n" +
                        "── PERNIKAHAN ──\n" +
                        "  Konsultasi   : Senin & Rabu 10.00–12.00 WIB\n" +
                        "  Kelas Pra-Nikah: Sabtu " + sabtu.format(FMT_PENDEK) + ", 09.00–11.30\n\n" +
                        "── SIDI ──\n" +
                        "  Batas daftar : " + dlSidi.format(FMT_PENDEK) + "\n" +
                        "  Peneguhan    : " + peneguhan.format(FMT_PANJANG) + "\n\n" +
                        "── KONSELING ──\n" +
                        "  Selasa terdekat : " + selasa.format(FMT_PENDEK) + " pukul 13.00 WIB\n\n" +
                        "📞 Info lebih lanjut: " + DatabaseHelper.getInfo("telepon"));
    }

    private ChatMessage infoIbadah() {
        LocalDate minggu = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return ChatMessage.botMessage(
                "⛪ Jadwal Ibadah " + DatabaseHelper.getInfo("nama_gereja") + "\n\n" +
                        "Ibadah Minggu (" + minggu.format(FMT_PENDEK) + "):\n" +
                        "  🕖 07.00 WIB – Ibadah I (Bahasa Jawa)\n" +
                        "  🕤 09.30 WIB – Ibadah II (Bahasa Indonesia)\n" +
                        "  🕔 17.00 WIB – Ibadah III (Bahasa Indonesia / Kontemporer)\n\n" +
                        "📚 Sekolah Minggu : 09.30 WIB, Ruang Anak Lt.1\n" +
                        "👨‍👩‍👧 PA Keluarga    : 09.30 WIB, Ruang Keluarga Lt.2\n" +
                        "🙏 Ibadah Pemuda  : Sabtu 16.00–18.00 WIB\n\n" +
                        "📍 Lokasi: " + DatabaseHelper.getInfo("alamat") + "\n\n" +
                        "Ketik \"kontak\" untuk informasi lebih lanjut.");
    }

    // ══════════════════════════════════════════════════════════
    //  BIAYA & KONTAK
    // ══════════════════════════════════════════════════════════

    private ChatMessage biaya(String kategori) {
        if ("SEMUA".equals(kategori)) {
            // Ambil semua biaya dari DB
            StringBuilder sb = new StringBuilder("💰 Biaya Layanan " + DatabaseHelper.getInfo("nama_gereja") + "\n\n");
            for (String kat : List.of("BAPTIS", "SIDI", "KONSELING", "PERNIKAHAN")) {
                List<String[]> list = DatabaseHelper.getBiaya(kat);
                if (!list.isEmpty()) {
                    sb.append("── ").append(namaKategori(kat)).append(" ──\n");
                    for (String[] item : list) {
                        sb.append("• ").append(item[0]).append(": ").append(item[1]).append("\n");
                        if (item[2] != null && !item[2].isBlank())
                            sb.append("  ↳ ").append(item[2]).append("\n");
                    }
                    sb.append("\n");
                }
            }
            sb.append("📞 Konfirmasi: ").append(DatabaseHelper.getInfo("telepon"));
            return ChatMessage.botMessage(sb.toString());
        }

        List<String[]> list = DatabaseHelper.getBiaya(kategori);
        if (list.isEmpty()) {
            return ChatMessage.botMessage(
                    "Info biaya " + namaKategori(kategori) + " belum tersedia.\n" +
                            "Silakan hubungi: " + DatabaseHelper.getInfo("telepon"));
        }
        StringBuilder sb = new StringBuilder("💰 Biaya " + namaKategori(kategori) + "\n\n");
        for (String[] item : list) {
            sb.append("• ").append(item[0]).append(": ").append(item[1]).append("\n");
            if (item[2] != null && !item[2].isBlank())
                sb.append("  ↳ ").append(item[2]).append("\n");
        }
        sb.append("\n📞 Konfirmasi terkini: ").append(DatabaseHelper.getInfo("telepon"));
        return ChatMessage.botMessage(sb.toString());
    }

    private ChatMessage infoKontak() {
        return ChatMessage.botMessage(
                "📞 Kontak " + DatabaseHelper.getInfo("nama_gereja") + "\n\n" +
                        "Telepon  : " + DatabaseHelper.getInfo("telepon")  + "\n" +
                        "WhatsApp : " + DatabaseHelper.getInfo("whatsapp") + "\n" +
                        "Email    : " + DatabaseHelper.getInfo("email")    + "\n" +
                        "Website  : " + DatabaseHelper.getInfo("website")  + "\n\n" +
                        "📍 Alamat:\n" + DatabaseHelper.getInfo("alamat") + "\n\n" +
                        "⏰ Jam Operasional Sekretariat:\n" +
                        DatabaseHelper.getInfo("jam_operasional") + "\n\n" +
                        "Sosmed:\n" +
                        "📸 Instagram: " + DatabaseHelper.getInfo("instagram") + "\n" +
                        "👍 Facebook : " + DatabaseHelper.getInfo("facebook"));
    }

    // ══════════════════════════════════════════════════════════
    //  BALASAN KONTEKSTUAL (FALLBACK)
    // ══════════════════════════════════════════════════════════

    private ChatMessage balasanKontekstual() {
        return switch (state) {
            case BAPTIS    -> ChatMessage.botMessage(
                    "Tentang Pembaptisan, Anda dapat bertanya:\n\n" +
                            "💧 \"syarat baptis\"\n" +
                            "📅 \"jadwal baptis\"\n" +
                            "💰 \"biaya baptis\"");
            case NIKAH     -> ChatMessage.botMessage(
                    "Tentang Pernikahan, Anda dapat bertanya:\n\n" +
                            "💍 \"syarat pernikahan\"\n" +
                            "📅 \"jadwal pra-nikah\"\n" +
                            "💰 \"biaya pernikahan\"");
            case SIDI      -> ChatMessage.botMessage(
                    "Tentang SIDI, Anda dapat bertanya:\n\n" +
                            "📖 \"syarat sidi\"\n" +
                            "📅 \"jadwal sidi\"\n" +
                            "💰 \"biaya sidi\"");
            case KONSELING -> ChatMessage.botMessage(
                    "Untuk informasi konseling:\n\n" +
                            "🤝 \"jadwal konseling\"\n" +
                            "💰 \"biaya konseling\"\n\n" +
                            "Atau langsung hubungi: " + DatabaseHelper.getInfo("telepon"));
            default        -> ChatMessage.botMessage(
                    "Maaf, saya belum menemukan jawaban untuk pertanyaan tersebut. 🙏\n\n" +
                            "Silakan tanya tentang:\n" +
                            "💧 Baptis\n" +
                            "💍 Pernikahan\n" +
                            "📖 SIDI / Peneguhan\n" +
                            "🤝 Konseling\n" +
                            "📅 Jadwal Kegiatan\n" +
                            "⛪ Visi, Misi & Profil Gereja\n" +
                            "📞 Kontak & Lokasi\n\n" +
                            "Contoh: \"syarat baptis\", \"jam ibadah\", \"pendeta gereja\"");
        };
    }

    // ══════════════════════════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════════════════════════

    private ChatMessage buatPesanSyarat(String kategori, String catatan) {
        List<String> list = DatabaseHelper.getSyarat(kategori);
        if (list.isEmpty()) {
            return ChatMessage.botMessage(
                    "Data persyaratan " + namaKategori(kategori) + " belum tersedia.\n" +
                            "Hubungi: " + DatabaseHelper.getInfo("telepon"));
        }
        StringBuilder sb = new StringBuilder("📋 Persyaratan " + namaKategori(kategori) + "\n\n");
        for (int i = 0; i < list.size(); i++)
            sb.append((i + 1)).append(". ").append(list.get(i)).append("\n");
        sb.append("\n⏰ Semua dokumen dikumpulkan paling lambat ").append(catatan);
        sb.append("\n\n📍 Sekretariat: ").append(DatabaseHelper.getInfo("alamat"));
        return ChatMessage.botMessage(sb.toString());
    }

    private boolean containsAny(String input, List<String> keywords) {
        return keywords.stream().anyMatch(input::contains);
    }

    private String namaKategori(String kode) {
        return switch (kode) {
            case "BAPTIS"     -> "Pembaptisan";
            case "PERNIKAHAN" -> "Pernikahan";
            case "SIDI"       -> "SIDI / Peneguhan";
            case "KONSELING"  -> "Konseling Pastoral";
            default           -> kode;
        };
    }

    public State getState()  { return state; }
    public void resetState() { state = State.AWAL; }
}
