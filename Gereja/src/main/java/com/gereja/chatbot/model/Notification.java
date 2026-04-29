package com.gereja.chatbot.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Model untuk notifikasi jadwal & batas pendaftaran.
 */
public class Notification {

    public enum Category { JADWAL, DEADLINE, INFO }
    public enum Status    { UNREAD, READ }

    private final String   id;
    private final String   emoji;
    private final String   title;
    private final String   subtitle;
    private final String   location;
    private final Category category;
    private       Status   status;
    private final LocalDate date;
    private final String   accentColor;

    public Notification(String id, String emoji, String title,
                        String subtitle, String location,
                        Category category, LocalDate date, String accentColor) {
        this.id          = id;
        this.emoji       = emoji;
        this.title       = title;
        this.subtitle    = subtitle;
        this.location    = location;
        this.category    = category;
        this.status      = Status.UNREAD;
        this.date        = date;
        this.accentColor = accentColor;
    }

    /** Jumlah hari hingga tanggal jadwal/deadline. */
    public long daysUntil() {
        return ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    /** Progress 0.0–1.0 untuk deadline (misal: 30 hari total → berapa sudah lewat). */
    public double deadlineProgress(int totalDays) {
        long passed = totalDays - daysUntil();
        return Math.max(0, Math.min(1.0, (double) passed / totalDays));
    }

    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy",
                           new java.util.Locale("id", "ID")));
    }

    // ── Getters / Setters ─────────────────────────────────────

    public String   getId()          { return id;          }
    public String   getEmoji()       { return emoji;       }
    public String   getTitle()       { return title;       }
    public String   getSubtitle()    { return subtitle;    }
    public String   getLocation()    { return location;    }
    public Category getCategory()    { return category;    }
    public Status   getStatus()      { return status;      }
    public void     setStatus(Status s){ this.status = s;  }
    public LocalDate getDate()       { return date;        }
    public String   getAccentColor() { return accentColor; }
}
