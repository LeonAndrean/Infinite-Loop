package com.gereja.chatbot.service;

import java.util.List;

/**
 * FuzzyMatcher – Pencocokan kata dengan toleransi typo menggunakan Levenshtein Distance.
 *
 * Cara kerja:
 *  - "Jatwal"  vs "jadwal"  → distance = 1 → COCOK (threshold ≤ 2)
 *  - "Minggue" vs "minggu"  → distance = 1 → COCOK
 *  - "Babtis"  vs "baptis"  → distance = 1 → COCOK
 *  - "Xyz"     vs "jadwal"  → distance = 5 → TIDAK COCOK
 *
 * Threshold adaptif berdasarkan panjang keyword:
 *  - keyword ≤ 4 karakter → max 1 edit  (mencegah false-positive di kata pendek)
 *  - keyword 5–7 karakter → max 2 edit
 *  - keyword ≥ 8 karakter → max 3 edit
 */
public class FuzzyMatcher {

    /**
     * Cek apakah salah satu token (kata) dari input fuzzy-match dengan keyword.
     * Input multi-kata dipecah per token, masing-masing token dicek terhadap keyword.
     *
     * Contoh: input = "jatwal minggu ini", keyword = "jadwal"
     *   → token "jatwal" vs "jadwal" → distance 1 → true
     */
    public static boolean tokenMatchesKeyword(String input, String keyword) {
        input   = input.toLowerCase().trim();
        keyword = keyword.toLowerCase().trim();

        // 1. Exact / contains match (sudah cepat, tidak perlu hitung distance)
        if (input.contains(keyword)) return true;

        int threshold = adaptiveThreshold(keyword.length());

        // 2. Keyword satu kata → bandingkan tiap token input
        if (!keyword.contains(" ")) {
            for (String token : input.split("\\s+")) {
                if (levenshtein(token, keyword) <= threshold) return true;
            }
            return false;
        }

        // 3. Keyword multi-kata (mis. "bulan ini") → sliding window token-to-token
        String[] kwTokens    = keyword.split("\\s+");
        String[] inputTokens = input.split("\\s+");
        if (inputTokens.length < kwTokens.length) return false;

        for (int i = 0; i <= inputTokens.length - kwTokens.length; i++) {
            boolean windowMatch = true;
            for (int j = 0; j < kwTokens.length; j++) {
                int t = adaptiveThreshold(kwTokens[j].length());
                if (levenshtein(inputTokens[i + j], kwTokens[j]) > t) {
                    windowMatch = false;
                    break;
                }
            }
            if (windowMatch) return true;
        }
        return false;
    }

    /**
     * Cek apakah input fuzzy-match dengan salah satu keyword dari list.
     */
    public static boolean matchesAny(String input, List<String> keywords) {
        for (String kw : keywords) {
            if (tokenMatchesKeyword(input, kw)) return true;
        }
        return false;
    }

    // ─── Threshold adaptif ──────────────────────────────────────
    private static int adaptiveThreshold(int keywordLength) {
        if (keywordLength <= 4) return 1;
        if (keywordLength <= 7) return 2;
        return 3;
    }

    // ─── Levenshtein Distance (dynamic programming) ─────────────
    public static int levenshtein(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],   // substitusi
                                   Math.min(dp[i - 1][j],         // hapus
                                            dp[i][j - 1]));       // tambah
                }
            }
        }
        return dp[m][n];
    }
}
