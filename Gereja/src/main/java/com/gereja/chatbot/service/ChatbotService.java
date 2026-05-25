package com.gereja.chatbot.service;

import com.gereja.chatbot.database.DatabaseHelper;
import com.gereja.chatbot.database.IbadahDatabaseHelper;
import com.gereja.chatbot.model.ChatMessage;
import com.gereja.chatbot.model.ChatMessage.StepInfo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * ChatbotService – Logika utama chatbot Faith Buddy dengan dukungan MULTI-INTENT
 */
public class ChatbotService {

    private static final List<String> KW_BAPTIS    = List.of("baptis","baptisan","pembaptisan","dibaptis");
    private static final List<String> KW_NIKAH     = List.of("nikah","pernikahan","menikah","wedding","kawin","pranikah","pra-nikah","pemberkatan");
    private static final List<String> KW_SIDI      = List.of("sidi","peneguhan","katekisasi");
    private static final List<String> KW_KONSELING = List.of("konseling","konsultasi","bimbingan","pastoral","curhat");
    private static final List<String> KW_JADWAL    = List.of("jadwal","pendaftaran","daftar","kapan","tanggal","agenda","acara","bulan ini");
    private static final List<String> KW_SALAM     = List.of("halo","hello","hi","selamat","pagi","siang","sore","malam","shalom","hai","hey","oi","hei");
    private static final List<String> KW_MAKASIH   = List.of("terima kasih","makasih","thanks","tq","thx","tengkyu");
    private static final List<String> KW_SYARAT    = List.of("syarat","persyaratan","dokumen","berkas","butuh apa","perlu apa","harus bawa");
    private static final List<String> KW_KONTAK    = List.of("kontak","hubungi","telepon","tlp","telp","alamat","lokasi","email","whatsapp","wa","nomor");
    private static final List<String> KW_BIAYA     = List.of("biaya","bayar","harga","tarif","gratis","berapa","fee","bayaran");
    private static final List<String> KW_IBADAH    = List.of("ibadah minggu","kebaktian","jam ibadah","ibadah anak","ibadah pemuda","kebaktian minggu");
    private static final List<String> KW_PROFIL    =  List.of("tentang gereja","gereja ini","gkj","faith buddy","apa itu");
    private static final List<String> KW_TEMA =
            List.of("tema", "tema ibadah", "topik ibadah");

    private static final List<String> KW_AYAT =
            List.of("ayat", "ayat alkitab", "firman tuhan");

    private static final List<String> KW_RENUNGAN =
            List.of("renungan", "renungan minggu");

    private static final List<String> KW_WAKTU_IBADAH =
            List.of("jadwal ibadah", "jam ibadah", "waktu ibadah");
    private static final List<String> KW_PENDETA   = List.of("pendeta", "siapa pendeta", "pelayan firman", "pdt");

    public enum State { AWAL, BAPTIS, NIKAH, SIDI, KONSELING, JADWAL }
    private State state = State.AWAL;

    private LocalDate lastRequestedIbadahDate = null;
    private Map<String, String> lastIbadahData = null;

    private static final DateTimeFormatter FMT_PANJANG =
            DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
    private static final DateTimeFormatter FMT_PENDEK  =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"));

    public List<ChatMessage> processInput(String input) {
        if (input == null || input.isBlank()) return Collections.emptyList();
        String lower = input.toLowerCase().trim();

        DatabaseHelper.simpanChat("USER", input.trim());

        // ── 1. DETEKSI MULTI-INTENT ──
        List<String> matchedParts = new ArrayList<>();

        if (containsAny(lower, KW_PENDETA)) {
            matchedParts.add(infoDetailPendeta());
        }

        if (containsAny(lower, List.of("ibadah minggu", "ibadah mingguan", "kebaktian minggu", "renungan minggu", "tema ibadah", "renungan ibadah"))) {
            matchedParts.add(handleIbadahMingguanQuery(lower));
        }

        if (containsAny(lower, KW_BAPTIS)) {
            if (containsAny(lower, KW_SYARAT)) {
                matchedParts.add(syaratBaptis().getContent());
            } else if (containsAny(lower, KW_JADWAL)) {
                matchedParts.add(jadwalBaptis().getContent());
            } else if (containsAny(lower, KW_BIAYA)) {
                matchedParts.add(biaya("BAPTIS").getContent());
            } else {
                matchedParts.add(formatStepMessageAsText(infoBaptis()));
            }
        }

        if (containsAny(lower, KW_NIKAH)) {
            if (containsAny(lower, KW_SYARAT)) {
                matchedParts.add(syaratNikah().getContent());
            } else if (containsAny(lower, KW_JADWAL)) {
                matchedParts.add(jadwalNikah().getContent());
            } else if (containsAny(lower, KW_BIAYA)) {
                matchedParts.add(biaya("PERNIKAHAN").getContent());
            } else {
                matchedParts.add(formatStepMessageAsText(infoNikah()));
            }
        }

        if (containsAny(lower, KW_SIDI)) {
            if (containsAny(lower, KW_SYARAT)) {
                matchedParts.add(syaratSidi().getContent());
            } else if (containsAny(lower, KW_JADWAL)) {
                matchedParts.add(jadwalSidi().getContent());
            } else if (containsAny(lower, KW_BIAYA)) {
                matchedParts.add(biaya("SIDI").getContent());
            } else {
                matchedParts.add(formatStepMessageAsText(infoSidi()));
            }
        }

        if (containsAny(lower, KW_KONSELING)) {
            if (containsAny(lower, KW_JADWAL)) {
                matchedParts.add(jadwalKonseling().getContent());
            } else if (containsAny(lower, KW_BIAYA)) {
                matchedParts.add(biaya("KONSELING").getContent());
            } else {
                matchedParts.add(formatStepMessageAsText(infoKonseling()));
            }
        }

        if (containsAny(lower, KW_KONTAK) && !containsAny(lower, KW_BAPTIS) && !containsAny(lower, KW_NIKAH) && !containsAny(lower, KW_SIDI)) {
            matchedParts.add(infoKontak().getContent());
        }

        if ((containsAny(lower, KW_PROFIL) || lower.matches(".*\\b(profil|tentang|siapa|sejarah)\\b.*"))
                && !containsAny(lower, KW_BAPTIS) && !containsAny(lower, KW_NIKAH) && !containsAny(lower, KW_SIDI) && !containsAny(lower, KW_PENDETA)) {
            String jawabanKK = DatabaseHelper.cariJawabanDariKataKunci(lower);
            if (jawabanKK != null) {
                matchedParts.add(jawabanKK);
            } else {
                matchedParts.add(infoProfilGereja().getContent());
            }
        }

        // Jika terdeteksi MULTI-INTENT (2 atau lebih pertanyaan sekaligus)
        if (matchedParts.size() >= 2) {
            StringBuilder combined = new StringBuilder();
            for (int i = 0; i < matchedParts.size(); i++) {
                combined.append(matchedParts.get(i));
                if (i < matchedParts.size() - 1) {
//                    combined.append("\n\n═════════════════════════════\n\n");
                }
            }
//            ChatMessage respons = ChatMessage.botMessage(combined.toString());
//            DatabaseHelper.simpanChat("BOT", respons.getContent());
//            return List.of(respons);
        }

        // ── 2. LOGIKA SINGLE-INTENT STANDARD (FALLBACK) ──
        ChatMessage respons;

        if (containsAny(lower, KW_SALAM) && !containsAny(lower, KW_JADWAL)
                && !containsAny(lower, KW_BAPTIS) && !containsAny(lower, KW_NIKAH) && !containsAny(lower, KW_PENDETA) && !containsAny(lower, KW_IBADAH)) {
            state = State.AWAL;
            respons = pesanSalam();

        } else if (containsAny(lower, KW_MAKASIH)) {
            respons = pesanMakasih();

        } else if (containsAny(lower, KW_PENDETA)) {
            respons = ChatMessage.botMessage(infoDetailPendeta());

        } else if (containsAny(lower, List.of("ibadah minggu", "ibadah mingguan", "kebaktian minggu", "renungan minggu", "tema ibadah", "renungan ibadah"))) {
            respons = ChatMessage.botMessage(handleIbadahMingguanQuery(lower));

        } else if (containsAny(lower, KW_PROFIL) || lower.matches(".*\\b(profil|tentang|siapa|sejarah)\\b.*")) {
            String jawabanKK = DatabaseHelper.cariJawabanDariKataKunci(lower);
            if (jawabanKK != null) {
                respons = ChatMessage.botMessage(jawabanKK);
            } else {
                respons = infoProfilGereja();
            }
            // ===== CONTEXT FOLLOW-UP IBADAH =====

            if (lastIbadahData != null) {

                // siapa pemimpin ibadah?
                if (lower.contains("pimpin")
                        || lower.contains("pemimpin")
                        || lower.contains("pelayan firman")) {

                    return List.of(ChatMessage.botMessage(
                            "👤 Pelayan Firman Ibadah:\n"
                                    + lastIbadahData.get("pemimpin")
                    ));
                }

                // ayat renungan
                if (lower.contains("ayat")) {

                    return List.of(ChatMessage.botMessage(
                            "📖 Ayat Alkitab:\n"
                                    + lastIbadahData.get("ayat")
                    ));
                }

                // tema ibadah
                if (lower.contains("tema")) {

                    return List.of(ChatMessage.botMessage(
                            "🌟 Tema Ibadah:\n"
                                    + lastIbadahData.get("tema")
                    ));
                }

                // renungan
                if (lower.contains("renungan")) {

                    return List.of(ChatMessage.botMessage(
                            "✍️ Renungan:\n"
                                    + lastIbadahData.get("renungan")
                    ));
                }

                // jadwal
                if (lower.contains("jam")
                        || lower.contains("jadwal")
                        || lower.contains("tanggal")) {

                    return List.of(ChatMessage.botMessage(
                            "📅 Jadwal Ibadah:\n"
                                    + lastRequestedIbadahDate.format(FMT_PANJANG)
                                    + "\n🕖 "
                                    + lastIbadahData.get("jam")
                    ));
                }
            }

        } else if (containsAny(lower, KW_IBADAH)) {
            respons = infoIbadah();

        } else if (containsAny(lower, KW_BAPTIS)) {
            state = State.BAPTIS;
            if      (containsAny(lower, KW_SYARAT)) respons = syaratBaptis();
            else if (containsAny(lower, KW_JADWAL)) respons = jadwalBaptis();
            else if (containsAny(lower, KW_BIAYA))  respons = biaya("BAPTIS");
            else                                      respons = infoBaptis();

        } else if (containsAny(lower, KW_NIKAH)) {
            state = State.NIKAH;
            if      (containsAny(lower, KW_SYARAT)) respons = syaratNikah();
            else if (containsAny(lower, KW_JADWAL)) respons = jadwalNikah();
            else if (containsAny(lower, KW_BIAYA))  respons = biaya("PERNIKAHAN");
            else                                      respons = infoNikah();

        } else if (containsAny(lower, KW_SIDI)) {
            state = State.SIDI;
            if      (containsAny(lower, KW_SYARAT)) respons = syaratSidi();
            else if (containsAny(lower, KW_JADWAL)) respons = jadwalSidi();
            else if (containsAny(lower, KW_BIAYA))  respons = biaya("SIDI");
            else                                      respons = infoSidi();

        } else if (containsAny(lower, KW_KONSELING)) {
            state = State.KONSELING;
            if      (containsAny(lower, KW_JADWAL)) respons = jadwalKonseling();
            else if (containsAny(lower, KW_BIAYA))  respons = biaya("KONSELING");
            else                                      respons = infoKonseling();

        } else if (containsAny(lower, KW_BIAYA)) {
            respons = biaya(state == State.AWAL ? "SEMUA" : state.name());

        } else if (containsAny(lower, KW_JADWAL)) {
            state = State.JADWAL;
            respons = infoJadwalUmum();

        } else if (containsAny(lower, KW_KONTAK)) {
            respons = infoKontak();

        } else {
            String jawabanKK = DatabaseHelper.cariJawabanDariKataKunci(lower);
            if (jawabanKK != null) {
                respons = ChatMessage.botMessage(jawabanKK);
            } else {
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

        // ── 3. ERROR HANDLING / FALLBACK JIKA PESAN TIDAK TERJAWAB ──
        if (respons == null || respons.getContent() == null ||
                respons.getContent().contains("Maaf, saya belum menemukan jawaban") ||
                respons.getContent().contains("Maaf, saya belum memiliki informasi")) {

            try {
                DatabaseHelper.simpanPertanyaanTakTerjawab(input.trim());
                System.out.println("[DEBUG] Pertanyaan tidak terjawab disimpan: " + input);
            } catch (Exception e) {
                System.err.println("Gagal menyimpan pertanyaan tak terjawab: " + e.getMessage());
            }

            if (respons == null || respons.getContent().isBlank()) {
                respons = ChatMessage.botMessage(
                        "🙏 Maaf, saya belum memiliki informasi lengkap mengenai hal tersebut.\n\n" +
                                "Pertanyaan Anda sudah saya catat agar Admin dapat melengkapi jawabannya segera. " +
                                "Silakan tanyakan hal lain atau hubungi Sekretariat di: " + DatabaseHelper.getInfo("telepon")
                );
            }
        }

        DatabaseHelper.simpanChat("BOT", respons.getContent());
        return List.of(respons);
    }

    public ChatMessage getInfoBaptis()    { state = State.BAPTIS;    return infoBaptis();     }
    public ChatMessage getInfoPernikahan(){ state = State.NIKAH;     return infoNikah();      }
    public ChatMessage getInfoSidi()      { state = State.SIDI;      return infoSidi();       }
    public ChatMessage getInfoKonseling() { state = State.KONSELING; return infoKonseling();  }
    public ChatMessage getInfoJadwal()    { state = State.JADWAL;    return infoJadwalUmum(); }
    public ChatMessage getWelcomeMessage(){ return pesanWelcome();   }

    // ══════════════════════════════════════════════════════════
    //  LOGIKA PENGHITUNGAN HARI & DETAIL IBADAH MINGGUAN
    // ══════════════════════════════════════════════════════════

    private String handleIbadahMingguanQuery(String lowerInput) {

        LocalDate today = LocalDate.now();
        LocalDate targetSunday;
        String label = "Minggu Ini";

        if (lowerInput.contains("minggu depan")) {
            label = "Minggu Depan";

            LocalDate upcomingSunday =
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
                targetSunday = today.plusWeeks(1);
            } else {
                targetSunday = upcomingSunday.plusWeeks(1);
            }

        } else {
            targetSunday =
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }

        Map<String, String> ibadah =
                IbadahDatabaseHelper.getIbadahByDate(targetSunday.toString());
        lastRequestedIbadahDate = targetSunday;
        lastIbadahData = ibadah;

        if (ibadah.isEmpty()) {
            return "Maaf, jadwal ibadah belum tersedia.";
        }

        String formattedDate = targetSunday.format(FMT_PANJANG);

        boolean askTema =
                containsAny(lowerInput, KW_TEMA);

        boolean askAyat =
                containsAny(lowerInput, KW_AYAT);

        boolean askRenungan =
                containsAny(lowerInput, KW_RENUNGAN);

        boolean askJadwal =
                containsAny(lowerInput, KW_WAKTU_IBADAH);

        boolean askPendeta =
                containsAny(lowerInput, KW_PENDETA);

        // Jika user minta SEMUA
        boolean askAll =
                askTema &&
                        askAyat &&
                        askRenungan &&
                        askJadwal &&
                        askPendeta;

        StringBuilder sb = new StringBuilder();

        // Jika tidak ada keyword spesifik
        // tampilkan full info default
        if (!askTema && !askAyat && !askRenungan
                && !askJadwal && !askPendeta) {

            askTema = true;
            askAyat = true;
            askRenungan = true;
            askJadwal = true;
            askPendeta = true;
        }

        sb.append("⛪ IBADAH ").append(label.toUpperCase()).append("\n\n");

        if (askJadwal) {
            sb.append("📅 Hari/Tanggal : ")
                    .append(formattedDate)
                    .append("\n");

            sb.append("🕖 Waktu : ")
                    .append(ibadah.get("jam"))
                    .append("\n\n");
        }

        if (askAyat) {
            sb.append("📖 Ayat Alkitab : ")
                    .append(ibadah.get("ayat"))
                    .append("\n\n");
        }

        if (askPendeta) {
            sb.append("👤 Pelayan Firman : ")
                    .append(ibadah.get("pemimpin"))
                    .append("\n\n");
        }

        if (askTema) {
            sb.append("🌟 Tema Ibadah :\n")
                    .append(ibadah.get("tema"))
                    .append("\n\n");
        }

        if (askRenungan) {
            sb.append("✍ Renungan :\n")
                    .append(ibadah.get("renungan"));
        }

        return sb.toString().trim();
    }

    private String infoDetailPendeta() {
        List<Map<String, String>> list = IbadahDatabaseHelper.getAllPendeta();
        if (list.isEmpty()) {
            return "👨‍⚕️ Informasi Pendeta\nData pendeta belum tersedia di database.";
        }
        StringBuilder sb = new StringBuilder("👨‍⚕️ INFORMASI DETAIL PENDETA GEREJA\nBerikut adalah data pendeta pelayan di GKJ Ngupasan:\n\n");
        for (Map<String, String> p : list) {
            sb.append("• ").append(p.get("nama")).append("\n");
            sb.append("  - Jabatan: ").append(p.get("jabatan")).append("\n");
            sb.append("  - Spesialisasi: ").append(p.get("spesialisasi")).append("\n");
            sb.append("  - Jadwal Konseling: ").append(p.get("jadwal_konseling")).append("\n");
            sb.append("  - Kontak Konsultasi: ").append(p.get("kontak")).append("\n\n");
        }
        return sb.toString().trim();
    }

    private ChatMessage pesanWelcome() {
        String namaGereja = DatabaseHelper.getInfo("nama_gereja");
        return ChatMessage.botMessage(
                "Shalom! Selamat datang di Faith Buddy 🙏\n" +
                        "Asisten informasi " + namaGereja + ".\n\n" +
                        "Silakan ketik pertanyaan Anda atau gunakan menu sidebar untuk menjelajah:\n" +
                        "• Tanya jadwal ibadah: \"jadwal ibadah minggu depan\"\n" +
                        "• Tanya profil pendeta: \"siapa pendeta gereja ini\"\n" +
                        "• Layanan gereja: \"syarat baptis\", \"biaya pernikahan\", \"jadwal sidi\"");
    }

    private ChatMessage pesanSalam() {
        return ChatMessage.botMessage("Shalom! Tuhan memberkati 🙏\nAda yang bisa saya bantu hari ini?");
    }

    private ChatMessage pesanMakasih() {
        return ChatMessage.botMessage(
                "Sama-sama, saudara! 😊\n" +
                        "Semoga pelayanan ini bermanfaat. Tuhan Yesus memberkati!\n\n" +
                        "Ada lagi yang bisa saya bantu?");
    }

    private ChatMessage infoProfilGereja() {
        String nama    = DatabaseHelper.getInfo("nama_gereja");
        String tahun   = DatabaseHelper.getInfo("tahun_berdiri");
        String denom   = DatabaseHelper.getInfo("denomiasi");
        String visi    = DatabaseHelper.getInfo("visi");
        String misi    = DatabaseHelper.getInfo("misi");
        String alamat  = DatabaseHelper.getInfo("alamat");

        return ChatMessage.botMessage(
                "✝ Profil " + nama + "\n\n" +
                        "📍 Alamat      : " + alamat + "\n" +
                        "📅 Berdiri     : " + tahun + "\n" +
                        "⛪ Denominasi  : " + denom + "\n\n" +
                        "Visi:\n" + visi + "\n\n" +
                        "Misi:\n" + misi + "\n\n" +
                        "Untuk info lengkap, kunjungi website resmi kami di: " + DatabaseHelper.getInfo("website"));
    }

    private ChatMessage infoBaptis() {
        StepInfo[] s = {
                new StepInfo(1, "Siapkan Dokumen", "KTP, KK, pas foto 3×4, formulir pendaftaran dari sekretariat", "#1A3A2A"),
                new StepInfo(2, "Daftar ke Sekretariat", "Datang langsung atau hubungi WA " + DatabaseHelper.getInfo("whatsapp"), "#2D5A3D"),
                new StepInfo(3, "Sesi Konseling", "2 kali pertemuan dengan Pendeta, sekitar 2–3 minggu sebelum pelaksanaan", "#4A7A5A"),
                new StepInfo(4, "Pelaksanaan Baptis", "Dalam Ibadah Minggu sesuai jadwal yang ditetapkan Majelis", "#D4A843")
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
                        "Silakan daftar melalui sekretariat atau hubungi WhatsApp: " + DatabaseHelper.getInfo("whatsapp"));
    }

    private ChatMessage infoNikah() {
        StepInfo[] s = {
                new StepInfo(1, "Konsultasi Awal", "Temui Pendeta minimal 3 bulan sebelum hari H untuk koordinasi", "#1A3A2A"),
                new StepInfo(2, "Siapkan Dokumen", "KTP, Akta Lahir, Surat Baptis, Surat Sipil N1–N4, foto berdampingan", "#2D5A3D"),
                new StepInfo(3, "Kelas Pra-Nikah", "4 sesi setiap Sabtu 09.00–11.30 WIB di Ruang Pertemuan Lt.1", "#4A7A5A"),
                new StepInfo(4, "Gladi Resik", "H-1 pemberkatan, pukul 17.00 WIB di Gedung Utama", "#7A9A6A"),
                new StepInfo(5, "Pemberkatan Pernikahan", "Sesuai hari yang disepakati bersama Majelis", "#D4A843")
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
                        "• Lokasi: Ruang Pertemuan, Lantai 1\n\n" +
                        "⚠️ Pendaftaran pernikahan wajib minimal 3 bulan sebelum hari H.");
    }

    private ChatMessage infoSidi() {
        StepInfo[] s = {
                new StepInfo(1, "Pendaftaran", "Syarat utama: sudah dibaptis & berusia minimal 14 tahun", "#1A3A2A"),
                new StepInfo(2, "Kelas Persiapan SIDI", "8 pertemuan setiap Minggu 11.00–13.00 WIB (sekitar 2 bulan)", "#2D5A3D"),
                new StepInfo(3, "Ujian Sidi", "Ujian lisan: Pengakuan Iman, Katekismus, dan Tata Ibadah GKJ", "#4A7A5A"),
                new StepInfo(4, "Peneguhan Sidi", "Dalam Ibadah Minggu yang disaksikan seluruh jemaat", "#D4A843")
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
                        "• Lokasi: Ruang Katekisasi, Lantai 2\n\n" +
                        "Peneguhan Sidi:\n" +
                        "• Perkiraan: " + peneguhan.format(FMT_PANJANG) + "\n\n" +
                        "⏰ Pendaftaran ditutup 1 minggu sebelum kelas dimulai.");
    }

    private ChatMessage infoKonseling() {
        StepInfo[] s = {
                new StepInfo(1, "Ajukan Permohonan", "Isi formulir di sekretariat atau via WhatsApp " + DatabaseHelper.getInfo("whatsapp"), "#1A3A2A"),
                new StepInfo(2, "Konfirmasi Jadwal", "Sekretariat menghubungi Anda dalam 1–3 hari kerja", "#2D5A3D"),
                new StepInfo(3, "Sesi Konseling", "±60 menit di Ruang Konseling Pastoral, Lantai 2. Online juga tersedia.", "#4A7A5A"),
                new StepInfo(4, "Tindak Lanjut", "Pendeta merekomendasikan langkah lanjut sesuai kebutuhan", "#D4A843")
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
                        pend1 + " (Selasa & Kamis, 13.00–16.00 WIB)\n" +
                        "  Selasa terdekat: " + selasa.format(FMT_PENDEK) + "\n\n" +
                        pend2 + " (Rabu & Jumat, 10.00–13.00 WIB)\n" +
                        "  Rabu terdekat: " + rabu.format(FMT_PENDEK) + "\n\n" +
                        "📍 Lokasi    : Ruang Konseling Pastoral, Gedung Gereja Lt.2\n" +
                        "📱 WhatsApp : " + DatabaseHelper.getInfo("whatsapp"));
    }

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
                        "📞 Info lengkap hubungi: " + DatabaseHelper.getInfo("telepon"));
    }

    private ChatMessage infoIbadah() {
        LocalDate minggu = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return ChatMessage.botMessage(
                "⛪ Jadwal Ibadah " + DatabaseHelper.getInfo("nama_gereja") + "\n\n" +
                        "Ibadah Minggu (" + minggu.format(FMT_PENDEK) + "):\n" +
                        "  Ibadah I   : 07.00 WIB (Bahasa Jawa)\n" +
                        "  Ibadah II  : 09.30 WIB (Bahasa Indonesia)\n" +
                        "  Ibadah III : 17.00 WIB (Kontemporer)\n\n" +
                        "📚 Sekolah Minggu : 09.30 WIB, Ruang Anak Lt.1\n" +
                        "🙏 Ibadah Pemuda  : Sabtu 16.00–18.00 WIB\n\n" +
                        "📍 Lokasi: " + DatabaseHelper.getInfo("alamat"));
    }

    private ChatMessage biaya(String kategori) {
        if ("SEMUA".equals(kategori)) {
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
            sb.append("📞 Hubungi sekretariat untuk konfirmasi.");
            return ChatMessage.botMessage(sb.toString());
        }

        List<String[]> list = DatabaseHelper.getBiaya(kategori);
        if (list.isEmpty()) {
            return ChatMessage.botMessage("Info biaya " + namaKategori(kategori) + " belum tersedia.");
        }
        StringBuilder sb = new StringBuilder("💰 Biaya " + namaKategori(kategori) + "\n\n");
        for (String[] item : list) {
            sb.append("• ").append(item[0]).append(": ").append(item[1]).append("\n");
            if (item[2] != null && !item[2].isBlank())
                sb.append("  ↳ ").append(item[2]).append("\n");
        }
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
                        "⏰ Jam Operasional Sekretariat:\n" + DatabaseHelper.getInfo("jam_operasional"));
    }

    private ChatMessage balasanKontekstual() {
        return switch (state) {
            case BAPTIS    -> ChatMessage.botMessage("Tentang Pembaptisan, Anda dapat bertanya:\n• \"syarat baptis\"\n• \"jadwal baptis\"\n• \"biaya baptis\"");
            case NIKAH     -> ChatMessage.botMessage("Tentang Pernikahan, Anda dapat bertanya:\n• \"syarat pernikahan\"\n• \"jadwal pra-nikah\"\n• \"biaya pernikahan\"");
            case SIDI      -> ChatMessage.botMessage("Tentang SIDI, Anda dapat bertanya:\n• \"syarat sidi\"\n• \"jadwal sidi\"\n• \"biaya sidi\"");
            case KONSELING -> ChatMessage.botMessage("Untuk informasi konseling:\n• \"jadwal konseling\"\n• \"biaya konseling\"\n\nAtau hubungi: " + DatabaseHelper.getInfo("telepon"));
            default        -> ChatMessage.botMessage(
                    "Maaf, saya belum menemukan jawaban yang tepat. 🙏\n\n" +
                            "Silakan tanya tentang:\n" +
                            "• Jadwal ibadah minggu depan / minggu ini\n" +
                            "• Profil pendeta / siapa pendeta gereja\n" +
                            "• Syarat & biaya (Baptis, Pernikahan, SIDI, Konseling)\n" +
                            "• Visi, misi, profil, dan kontak gereja");
        };
    }

    private String formatStepMessageAsText(ChatMessage stepMsg) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 **").append(stepMsg.getContent()).append("**:\n\n");
        if (stepMsg.getSteps() != null) {
            for (StepInfo step : stepMsg.getSteps()) {
                sb.append("📍 **Langkah ").append(step.getNumber()).append("**: ").append(step.getTitle()).append("\n");
                sb.append("   ").append(step.getDetail()).append("\n\n");
            }
        }
        return sb.toString().trim();
    }

    private ChatMessage buatPesanSyarat(String kategori, String catatan) {
        List<String> list = DatabaseHelper.getSyarat(kategori);
        if (list.isEmpty()) {
            return ChatMessage.botMessage("Data persyaratan " + namaKategori(kategori) + " belum tersedia.");
        }
        StringBuilder sb = new StringBuilder("📋 Persyaratan " + namaKategori(kategori) + "\n\n");
        for (int i = 0; i < list.size(); i++)
            sb.append((i + 1)).append(". ").append(list.get(i)).append("\n");
        sb.append("\n⏰ Semua dokumen dikumpulkan paling lambat ").append(catatan);
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