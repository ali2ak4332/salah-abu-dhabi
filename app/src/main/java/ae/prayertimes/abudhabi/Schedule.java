package ae.prayertimes.abudhabi;

import java.io.*;
import java.time.*;
import java.util.*;

public final class Schedule {
    public static final ZoneId ZONE = ZoneId.of("Asia/Dubai");
    public final TreeMap<LocalDate, LocalTime[]> days = new TreeMap<>();
    public static final int[] PRAYERS = {0, 2, 3, 4, 5};
    public Schedule(Reader input) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        String header = reader.readLine();
        if (!"date,fajr,sunrise,dhuhr,asr,maghrib,isha".equals(header)) throw new IOException("Invalid timetable header");
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length != 7) throw new IOException("Invalid timetable row");
            LocalDate date = LocalDate.parse(parts[0]);
            LocalTime[] times = new LocalTime[6];
            for (int i = 0; i < 6; i++) {
                times[i] = LocalTime.parse(parts[i + 1]);
                if (i > 0 && !times[i].isAfter(times[i - 1])) throw new IOException("Invalid time order");
            }
            if (days.put(date, times) != null) throw new IOException("Duplicate date");
        }
        if (days.isEmpty()) throw new IOException("Empty timetable");
        LocalDate previous = null;
        for (LocalDate date : days.keySet()) {
            if (previous != null && !date.equals(previous.plusDays(1))) throw new IOException("Missing date");
            previous = date;
        }
    }
    public LocalDate clamp(LocalDate date) {
        if (date.isBefore(days.firstKey())) return days.firstKey();
        if (date.isAfter(days.lastKey())) return days.lastKey();
        return date;
    }
    public static final class Next {
        public final int index;
        public final ZonedDateTime at;
        Next(int index, ZonedDateTime at) { this.index = index; this.at = at; }
    }
    public Next next(Instant now) {
        LocalDate today = now.atZone(ZONE).toLocalDate();
        // Never extrapolate or treat a distant first row as today's schedule.
        if (!days.containsKey(today)) return null;
        for (int offset = 0; offset < 2; offset++) {
            LocalDate date = today.plusDays(offset);
            LocalTime[] times = days.get(date);
            if (times == null) continue;
            for (int i : PRAYERS) {
                ZonedDateTime at = ZonedDateTime.of(date, times[i], ZONE);
                if (!at.toInstant().isBefore(now)) return new Next(i, at);
            }
        }
        return null;
    }
}
