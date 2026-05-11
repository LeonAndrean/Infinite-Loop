package com.gereja.chatbot.service;

import com.gereja.chatbot.model.Notification;
import com.gereja.chatbot.model.Notification.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * NotificationService - daftar jadwal & deadline mendatang.
 * Semua tanggal dihitung otomatis dari hari ini, tidak ada data hardcode.
 */
public class NotificationService {

    private final ObservableList<Notification> notifications =
            FXCollections.observableArrayList();

    public NotificationService() {
        muatData();
    }

    private void muatData() {
        LocalDate today = LocalDate.now();
        Locale id       = new Locale("id","ID");

        // Jadwal dinamis
        LocalDate pranikah  = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDate sidi      = today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        LocalDate konseling = today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        LocalDate baptis    = today.plusMonths(1).withDayOfMonth(1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY));
        LocalDate deadlineBaptis = baptis.minusWeeks(2);
        LocalDate peneguhan      = today.plusMonths(2)
                .with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));
        LocalDate deadlineSidi   = peneguhan.minusWeeks(5);

        String bulanBaptis    = baptis.getMonth().getDisplayName(TextStyle.FULL, id);
        String bulanPeneguhan = peneguhan.getMonth().getDisplayName(TextStyle.FULL, id);
        String bulanPranikah  = pranikah.getMonth().getDisplayName(TextStyle.FULL, id);

        notifications.addAll(
                // Jadwal
                new Notification("n1","💍","Kelas Pra-Nikah – "+bulanPranikah,
                        "Sabtu • 09.00 – 11.30 WIB","Ruang Serbaguna Lt.2",
                        Category.JADWAL, pranikah,"#D4A843"),
                new Notification("n2","📖","Kelas Persiapan SIDI – "+bulanPranikah,
                        "Minggu • 11.00 – 13.00 WIB","Aula Gereja Utama",
                        Category.JADWAL, sidi,"#2D5A3D"),
                new Notification("n3","🤝","Konseling – Pdt. Harianto",
                        "Selasa • 14.00 – 15.00 WIB","Ruang Konseling Pastoral",
                        Category.JADWAL, konseling,"#6A4A8A"),
                new Notification("n4","💧","Ibadah Pembaptisan "+bulanBaptis,
                        "Minggu • 08.00 WIB","Gedung Ibadah Utama",
                        Category.JADWAL, baptis,"#1A3A2A"),
                // Deadline
                new Notification("d1","⏰","Deadline Daftar Baptis "+bulanBaptis,
                        "Tutup: "+deadlineBaptis,"Sekretariat",
                        Category.DEADLINE, deadlineBaptis,"#E05252"),
                new Notification("d2","⏰","Deadline Daftar SIDI "+bulanPeneguhan,
                        "Tutup: "+deadlineSidi,"Sekretariat",
                        Category.DEADLINE, deadlineSidi,"#D4A843")
        );
    }

    public ObservableList<Notification> getNotifications() { return notifications; }

    public long getUnreadCount() {
        return notifications.stream()
                .filter(n -> n.getStatus() == Notification.Status.UNREAD).count();
    }

    public void markAllRead() {
        notifications.forEach(n -> n.setStatus(Notification.Status.READ));
    }

    public void markRead(String id) {
        notifications.stream().filter(n -> n.getId().equals(id))
                .findFirst().ifPresent(n -> n.setStatus(Notification.Status.READ));
    }
}