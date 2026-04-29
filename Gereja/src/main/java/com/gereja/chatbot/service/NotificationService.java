package com.gereja.chatbot.service;

import com.gereja.chatbot.model.Notification;
import com.gereja.chatbot.model.Notification.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;

/**
 * Service untuk mengelola daftar notifikasi dan jadwal.
 */
public class NotificationService {

    private final ObservableList<Notification> notifications = FXCollections.observableArrayList();

    public NotificationService() {
        loadSampleData();
    }

    private void loadSampleData() {
        LocalDate today = LocalDate.now();

        notifications.addAll(
            // ── Jadwal ──────────────────────────────────────────
            new Notification(
                "n1", "💍", "Kelas Pra-Nikah – Sesi 1",
                "Sabtu • 09.00 – 11.30 WIB",
                "Ruang Serbaguna Lt.2",
                Category.JADWAL,
                today.plusDays(4),
                "#D4A843"
            ),
            new Notification(
                "n2", "📖", "Kelas Persiapan SIDI – Sesi 3",
                "Minggu • 11.00 – 13.00 WIB",
                "Aula Gereja Utama",
                Category.JADWAL,
                today.plusDays(5),
                "#2D5A3D"
            ),
            new Notification(
                "n3", "🤝", "Konseling – Pdt. Harianto",
                "Selasa • 14.00 – 15.00 WIB",
                "Ruang Konseling Pastoral",
                Category.JADWAL,
                today.plusDays(7),
                "#6A4A8A"
            ),
            new Notification(
                "n4", "💧", "Ibadah Pembaptisan Maret",
                "Minggu • 08.00 WIB",
                "Gedung Ibadah Utama",
                Category.JADWAL,
                today.plusDays(19),
                "#1A3A2A"
            ),

            // ── Deadline ─────────────────────────────────────────
            new Notification(
                "d1", "⏰", "Deadline Daftar Baptis Maret",
                "Tutup: " + today.plusDays(7),
                "Sekretariat / Aplikasi",
                Category.DEADLINE,
                today.plusDays(7),
                "#E05252"
            ),
            new Notification(
                "d2", "⏰", "Deadline Daftar SIDI April",
                "Tutup: " + today.plusDays(23),
                "Sekretariat / Aplikasi",
                Category.DEADLINE,
                today.plusDays(23),
                "#D4A843"
            )
        );
    }

    public ObservableList<Notification> getNotifications() {
        return notifications;
    }

    public long getUnreadCount() {
        return notifications.stream()
            .filter(n -> n.getStatus() == Notification.Status.UNREAD)
            .count();
    }

    public void markAllRead() {
        notifications.forEach(n -> n.setStatus(Notification.Status.READ));
    }

    public void markRead(String id) {
        notifications.stream()
            .filter(n -> n.getId().equals(id))
            .findFirst()
            .ifPresent(n -> n.setStatus(Notification.Status.READ));
    }
}
