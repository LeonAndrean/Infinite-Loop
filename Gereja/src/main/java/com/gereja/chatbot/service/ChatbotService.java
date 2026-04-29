package com.gereja.chatbot.service;

import com.gereja.chatbot.model.ChatMessage;
import com.gereja.chatbot.model.ChatMessage.StepInfo;

import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════
 *  ChatbotService
 *  Otak dari chatbot – menangani logika respon berdasarkan
 *  input pengguna. Menggunakan keyword matching sederhana
 *  yang dapat diganti dengan NLP di masa depan.
 * ═══════════════════════════════════════════════════════════
 */
public class ChatbotService {

    // ── Keyword maps ──────────────────────────────────────────

    private static final List<String> KEYWORDS_BAPTIS = List.of(
        "baptis", "baptisan", "pembaptisan", "dibaptis"
    );
    private static final List<String> KEYWORDS_NIKAH = List.of(
        "nikah", "pernikahan", "menikah", "wedding", "kawin", "pra-nikah", "pranikah"
    );
    private static final List<String> KEYWORDS_SIDI = List.of(
        "sidi", "peneguhan", "sidi/peneguhan", "kelas sidi"
    );
    private static final List<String> KEYWORDS_KONSELING = List.of(
        "konseling", "konsultasi", "bimbingan", "pendeta", "pastoral"
    );
    private static final List<String> KEYWORDS_JADWAL = List.of(
        "jadwal", "pendaftaran", "daftar", "kapan", "tanggal", "waktu", "deadline", "batas"
    );
    private static final List<String> KEYWORDS_SALAM = List.of(
        "halo", "hello", "hi", "selamat", "pagi", "siang", "sore", "malam", "shalom", "hai"
    );
    private static final List<String> KEYWORDS_TERIMA_KASIH = List.of(
        "terima kasih", "thanks", "makasih", "terimakasih", "thank"
    );
    private static final List<String> KEYWORDS_SYARAT = List.of(
        "syarat", "persyaratan", "dokumen", "berkas", "kebutuhan"
    );
    private static final List<String> KEYWORDS_KONTAK = List.of(
        "kontak", "hubungi", "telepon", "telpon", "alamat", "lokasi", "email"
    );

    // ── State management ─────────────────────────────────────

    public enum ConversationState {
        INITIAL, BAPTIS, NIKAH, SIDI, KONSELING, JADWAL, GENERAL
    }

    private ConversationState currentState = ConversationState.INITIAL;

    // ── Main process method ───────────────────────────────────

    /**
     * Proses input pengguna dan kembalikan respons chatbot.
     * @param input teks dari pengguna
     * @return list ChatMessage (bisa lebih dari satu pesan)
     */
    public List<ChatMessage> processInput(String input) {
        if (input == null || input.isBlank()) return Collections.emptyList();

        String lower = input.toLowerCase().trim();
        List<ChatMessage> responses = new ArrayList<>();

        // Deteksi intent berdasarkan keyword
        if (containsAny(lower, KEYWORDS_SALAM)) {
            responses.add(buildSalamResponse());
            currentState = ConversationState.INITIAL;

        } else if (containsAny(lower, KEYWORDS_TERIMA_KASIH)) {
            responses.add(ChatMessage.botMessage(
                "Sama-sama! 🙏 Semoga informasi ini bermanfaat. " +
                "Jika ada pertanyaan lain, jangan ragu untuk bertanya ya. " +
                "Tuhan memberkati! ✝"
            ));

        } else if (containsAny(lower, KEYWORDS_BAPTIS)) {
            currentState = ConversationState.BAPTIS;
            if (containsAny(lower, KEYWORDS_SYARAT)) {
                responses.add(buildSyaratBaptis());
            } else {
                responses.add(buildInfoPembaptisan());
            }

        } else if (containsAny(lower, KEYWORDS_NIKAH)) {
            currentState = ConversationState.NIKAH;
            if (containsAny(lower, KEYWORDS_SYARAT)) {
                responses.add(buildSyaratPernikahan());
            } else {
                responses.add(buildInfoPernikahan());
            }

        } else if (containsAny(lower, KEYWORDS_SIDI)) {
            currentState = ConversationState.SIDI;
            responses.add(buildInfoSidi());

        } else if (containsAny(lower, KEYWORDS_KONSELING)) {
            currentState = ConversationState.KONSELING;
            responses.add(buildInfoKonseling());

        } else if (containsAny(lower, KEYWORDS_JADWAL)) {
            currentState = ConversationState.JADWAL;
            responses.add(buildInfoJadwal());

        } else if (containsAny(lower, KEYWORDS_KONTAK)) {
            responses.add(buildInfoKontak());

        } else {
            // Konteks lanjutan berdasarkan state
            responses.add(buildContextualResponse(lower));
        }

        return responses;
    }

    // ── Shortcut for menu clicks ──────────────────────────────

    public ChatMessage getInfoPembaptisan()  { return buildInfoPembaptisan(); }
    public ChatMessage getInfoPernikahan()   { return buildInfoPernikahan();  }
    public ChatMessage getInfoSidi()         { return buildInfoSidi();        }
    public ChatMessage getInfoKonseling()    { return buildInfoKonseling();   }
    public ChatMessage getInfoJadwal()       { return buildInfoJadwal();      }
    public ChatMessage getWelcomeMessage()   { return buildWelcomeMessage();  }

    // ── Response builders ─────────────────────────────────────

    private ChatMessage buildWelcomeMessage() {
        return ChatMessage.botMessage(
            "Shalom! 🙏 Selamat datang di layanan informasi GerejaCare.\n\n" +
            "Saya siap membantu Anda mendapatkan informasi tentang pelayanan gerejawi. " +
            "Silakan pilih layanan yang Anda butuhkan:"
        );
    }

    private ChatMessage buildSalamResponse() {
        return ChatMessage.botMessage(
            "Shalom! 🙏 Selamat datang!\n\n" +
            "Saya adalah asisten digital GerejaCare yang siap membantu Anda " +
            "mendapatkan informasi pelayanan gerejawi. Anda dapat bertanya tentang:\n\n" +
            "• 💧 Pembaptisan\n" +
            "• 💍 Pernikahan Gerejawi\n" +
            "• 📖 SIDI / Peneguhan\n" +
            "• 🤝 Konseling Pendeta\n" +
            "• 📅 Jadwal & Pendaftaran"
        );
    }

    private ChatMessage buildInfoPembaptisan() {
        StepInfo[] steps = {
            new StepInfo(1, "Persyaratan Dokumen",
                "KTP/Kartu Keluarga, Foto 3×4 (2 lembar), " +
                "Surat rekomendasi dari orang tua/wali (untuk anak-anak), " +
                "Formulir pendaftaran baptis",
                "#1A3A2A"),
            new StepInfo(2, "Pendaftaran",
                "Daftar ke sekretariat gereja atau melalui aplikasi ini. " +
                "Senin–Jumat pukul 08.00–16.00 WIB",
                "#2D5A3D"),
            new StepInfo(3, "Sesi Konseling",
                "Mengikuti 2× sesi konseling bersama Pendeta " +
                "(±2 minggu sebelum pelaksanaan)",
                "#4A7A5A"),
            new StepInfo(4, "Pelaksanaan Baptis",
                "Dilaksanakan pada Ibadah Minggu sesuai jadwal yang " +
                "ditetapkan Majelis Jemaat",
                "#D4A843")
        };
        String[] replies = {
            "📋 Lihat Persyaratan Lengkap",
            "📅 Jadwal Baptis",
            "📝 Daftar Sekarang",
            "❓ Tanya Lebih Lanjut"
        };
        return ChatMessage.botStepMessage("💧 Panduan Lengkap Pembaptisan", steps, replies);
    }

    private ChatMessage buildSyaratBaptis() {
        return ChatMessage.botMessage(
            "📋 Persyaratan Pembaptisan:\n\n" +
            "Dokumen yang perlu disiapkan:\n" +
            "1. Fotokopi KTP pemohon\n" +
            "2. Fotokopi Kartu Keluarga\n" +
            "3. Pas foto 3×4 sebanyak 2 lembar\n" +
            "4. Formulir pendaftaran (tersedia di sekretariat)\n\n" +
            "Untuk Pembaptisan Anak (di bawah 17 tahun):\n" +
            "5. Surat pernyataan orang tua/wali\n" +
            "6. Akta Kelahiran anak\n\n" +
            "📌 Catatan: Semua dokumen dikumpulkan paling lambat " +
            "2 minggu sebelum tanggal pelaksanaan."
        );
    }

    private ChatMessage buildInfoPernikahan() {
        StepInfo[] steps = {
            new StepInfo(1, "Konsultasi Awal",
                "Konsultasi dengan Pendeta untuk membahas rencana " +
                "pernikahan (minimal 3 bulan sebelum hari H)",
                "#1A3A2A"),
            new StepInfo(2, "Persyaratan Dokumen",
                "KTP, Akta Lahir, Surat Baptis kedua mempelai, " +
                "Surat Nikah Sipil (N1-N4), Foto 4×6 berdampingan",
                "#2D5A3D"),
            new StepInfo(3, "Kelas Pra-Nikah",
                "Mengikuti 4× sesi kelas pra-nikah bersama Pendeta " +
                "(setiap Sabtu pagi, 09.00–11.30 WIB)",
                "#4A7A5A"),
            new StepInfo(4, "Gladi Resik",
                "Gladi resik dilakukan H-1 sebelum pemberkatan, " +
                "pukul 17.00 WIB",
                "#7A9A6A"),
            new StepInfo(5, "Pemberkatan Pernikahan",
                "Dilaksanakan pada hari yang telah disepakati " +
                "bersama Majelis Jemaat",
                "#D4A843")
        };
        String[] replies = {
            "📋 Persyaratan Dokumen",
            "📅 Jadwal Kelas Pra-Nikah",
            "📝 Konsultasi Pendeta",
            "💒 Info Venue"
        };
        return ChatMessage.botStepMessage("💍 Panduan Pernikahan Gerejawi", steps, replies);
    }

    private ChatMessage buildSyaratPernikahan() {
        return ChatMessage.botMessage(
            "📋 Persyaratan Pernikahan Gerejawi:\n\n" +
            "Dokumen Kedua Mempelai:\n" +
            "1. Fotokopi KTP masing-masing\n" +
            "2. Fotokopi Akta Kelahiran\n" +
            "3. Surat Baptis gereja asal\n" +
            "4. Pas foto berdampingan 4×6 (4 lembar)\n\n" +
            "Surat dari Catatan Sipil:\n" +
            "5. Surat N1 (Keterangan untuk menikah)\n" +
            "6. Surat N2 (Keterangan asal-usul)\n" +
            "7. Surat N4 (Keterangan orang tua)\n\n" +
            "📌 Semua berkas diserahkan minimal 3 bulan sebelum hari H."
        );
    }

    private ChatMessage buildInfoSidi() {
        StepInfo[] steps = {
            new StepInfo(1, "Pendaftaran",
                "Daftar ke sekretariat atau melalui aplikasi ini. " +
                "Syarat: sudah dibaptis & berusia minimal 14 tahun",
                "#1A3A2A"),
            new StepInfo(2, "Kelas Persiapan SIDI",
                "Mengikuti 8× pertemuan kelas SIDI setiap Minggu " +
                "pukul 11.00–13.00 WIB (±2 bulan)",
                "#2D5A3D"),
            new StepInfo(3, "Ujian / Evaluasi",
                "Ujian lisan tentang Pengakuan Iman, Katekismus, " +
                "dan Tata Ibadah Gereja",
                "#4A7A5A"),
            new StepInfo(4, "Peneguhan Sidi",
                "Peneguhan dilakukan dalam Ibadah Minggu yang khidmat " +
                "disaksikan seluruh jemaat",
                "#D4A843")
        };
        String[] replies = {
            "📋 Materi Kelas SIDI",
            "📅 Jadwal Kelas",
            "📝 Daftar SIDI",
            "❓ FAQ SIDI"
        };
        return ChatMessage.botStepMessage("📖 Panduan SIDI / Peneguhan Sidi", steps, replies);
    }

    private ChatMessage buildInfoKonseling() {
        StepInfo[] steps = {
            new StepInfo(1, "Ajukan Permohonan",
                "Isi formulir permohonan konseling di sekretariat " +
                "atau melalui aplikasi ini",
                "#1A3A2A"),
            new StepInfo(2, "Konfirmasi Jadwal",
                "Tim sekretariat akan menghubungi Anda untuk " +
                "konfirmasi jadwal dengan Pendeta (1–3 hari kerja)",
                "#2D5A3D"),
            new StepInfo(3, "Sesi Konseling",
                "Sesi berlangsung ±60 menit di Ruang Konseling Pastoral. " +
                "Tersedia juga konseling online via video call",
                "#4A7A5A"),
            new StepInfo(4, "Tindak Lanjut",
                "Pendeta akan merekomendasikan tindak lanjut sesuai " +
                "kebutuhan pastoral Anda",
                "#D4A843")
        };
        String[] replies = {
            "📝 Ajukan Konseling",
            "👤 Profil Pendeta",
            "🕐 Jadwal Konseling",
            "📞 Hubungi Langsung"
        };
        return ChatMessage.botStepMessage("🤝 Panduan Konseling Pastoral", steps, replies);
    }

    private ChatMessage buildInfoJadwal() {
        return ChatMessage.botMessage(
            "📅 Jadwal Layanan Gerejawi 2024:\n\n" +
            "💧 PEMBAPTISAN:\n" +
            "• Pendaftaran: s.d. 29 Februari 2024\n" +
            "• Pelaksanaan: Minggu, 10 Maret 2024\n\n" +
            "💍 PERNIKAHAN:\n" +
            "• Konsultasi: Setiap Senin & Rabu, 10.00–12.00 WIB\n" +
            "• Kelas Pra-Nikah: Sabtu, 09.00–11.30 WIB\n\n" +
            "📖 SIDI:\n" +
            "• Pendaftaran: s.d. 15 Maret 2024\n" +
            "• Kelas: Minggu, 11.00–13.00 WIB (Feb–Apr)\n" +
            "• Peneguhan: Minggu, 28 April 2024\n\n" +
            "🤝 KONSELING:\n" +
            "• Pdt. Harianto: Selasa & Kamis, 13.00–16.00 WIB\n" +
            "• Pdt. Maria: Rabu & Jumat, 10.00–13.00 WIB\n\n" +
            "📌 Untuk pendaftaran dan info lebih lanjut, " +
            "hubungi sekretariat: (021) 123-4567"
        );
    }

    private ChatMessage buildInfoKontak() {
        return ChatMessage.botMessage(
            "📞 Informasi Kontak Gereja:\n\n" +
            "🏛 Sekretariat:\n" +
            "Telepon: (021) 123-4567\n" +
            "WhatsApp: +62 812-3456-7890\n" +
            "Email: sekretariat@gerejacare.org\n\n" +
            "📍 Alamat:\n" +
            "Jl. Pelayanan No. 1, Jakarta Pusat\n\n" +
            "🕒 Jam Operasional:\n" +
            "Senin–Jumat: 08.00–16.00 WIB\n" +
            "Sabtu: 09.00–12.00 WIB\n" +
            "Minggu: 07.00–13.00 WIB\n\n" +
            "💻 Website: www.gerejacare.org"
        );
    }

    private ChatMessage buildContextualResponse(String input) {
        // Respons kontekstual berdasarkan state percakapan
        return switch (currentState) {
            case BAPTIS -> ChatMessage.botMessage(
                "Apakah Anda ingin tahu lebih lanjut tentang pembaptisan? " +
                "Anda bisa bertanya tentang:\n" +
                "• Persyaratan dokumen\n• Jadwal pembaptisan\n" +
                "• Cara pendaftaran\n• Biaya (jika ada)"
            );
            case NIKAH -> ChatMessage.botMessage(
                "Apakah ada yang ingin ditanyakan lebih lanjut tentang pernikahan gerejawi? " +
                "Misalnya persyaratan, jadwal kelas pra-nikah, atau tata cara pemberkatan."
            );
            case SIDI -> ChatMessage.botMessage(
                "Apakah ada pertanyaan lanjutan tentang SIDI? " +
                "Anda bisa tanya tentang materi kelas, jadwal, atau persyaratan."
            );
            case KONSELING -> ChatMessage.botMessage(
                "Untuk konseling pastoral, Anda bisa langsung menghubungi " +
                "sekretariat di (021) 123-4567 atau mengisi formulir permohonan. " +
                "Apakah ada yang perlu diklarifikasi?"
            );
            default -> ChatMessage.botMessage(
                "Maaf, saya belum memahami pertanyaan Anda. 😊\n\n" +
                "Anda dapat bertanya tentang:\n" +
                "• 💧 Pembaptisan\n• 💍 Pernikahan Gerejawi\n" +
                "• 📖 SIDI / Peneguhan\n• 🤝 Konseling Pendeta\n" +
                "• 📅 Jadwal & Pendaftaran\n• 📞 Informasi Kontak\n\n" +
                "Atau ketikkan kata kunci seperti 'syarat baptis', 'jadwal SIDI', dll."
            );
        };
    }

    // ── Utility ───────────────────────────────────────────────

    private boolean containsAny(String input, List<String> keywords) {
        return keywords.stream().anyMatch(input::contains);
    }

    public ConversationState getCurrentState() { return currentState; }
    public void resetState() { currentState = ConversationState.INITIAL; }
}
