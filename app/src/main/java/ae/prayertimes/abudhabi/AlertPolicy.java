package ae.prayertimes.abudhabi;

import java.time.*;

/** Pure scheduling decisions; never synthesizes prayer times. */
public final class AlertPolicy {
    public static final long MAX_LATENESS_MILLIS = 15 * 60 * 1000;
    public static final long MAX_REMINDER_LATENESS_MILLIS = 2 * 60 * 1000;
    public static final int PRAYER_MASK = 61; // Fajr, Dhuhr, Asr, Maghrib, Isha; never sunrise.
    public static final class Event {
        public final long epochMillis;
        public final int prayerIndex; // -1 = silent widget date refresh
        Event(long epochMillis, int prayerIndex) { this.epochMillis=epochMillis; this.prayerIndex=prayerIndex; }
    }
    public static Event next(Schedule schedule, Instant now, boolean alerts, boolean widgets) {
        if (!alerts && !widgets) return null;
        LocalDate date = now.atZone(Schedule.ZONE).toLocalDate();
        if(date.isBefore(schedule.days.firstKey()))return new Event(schedule.days.firstKey().atStartOfDay(Schedule.ZONE).toInstant().toEpochMilli(),-1);
        Schedule.Next prayer = schedule.next(now);
        Event event = prayer == null ? null : new Event(prayer.at.toInstant().toEpochMilli(), prayer.index);
        if (widgets && !date.isAfter(schedule.days.lastKey())) {
            Instant midnight = date.plusDays(1).atStartOfDay(Schedule.ZONE).toInstant();
            if (event == null || midnight.toEpochMilli() < event.epochMillis) event=new Event(midnight.toEpochMilli(),-1);
        }
        return event;
    }
    public static boolean shouldNotify(Schedule schedule, int index, long epochMillis, Instant now, long lastNotified) {
        if(index<0 || index>5 || index==1 || epochMillis<=0 || epochMillis==lastNotified) return false;
        long delay=now.toEpochMilli()-epochMillis;
        if(delay<0 || delay>MAX_LATENESS_MILLIS) return false;
        ZonedDateTime at=Instant.ofEpochMilli(epochMillis).atZone(Schedule.ZONE);
        LocalTime[] times=schedule.days.get(at.toLocalDate());
        return times!=null && times[index].equals(at.toLocalTime());
    }

    /** Sunrise is a separate saved event, never a sixth prayer or an iqamah. */
    public static Event nextSunrise(Schedule schedule, Instant now, boolean enabled) {
        if(!enabled)return null;
        LocalDate today=now.atZone(Schedule.ZONE).toLocalDate();
        for(LocalDate date:schedule.days.tailMap(today,true).keySet()) {
            Instant at=ZonedDateTime.of(date,schedule.days.get(date)[1],Schedule.ZONE).toInstant();
            if(!at.isBefore(now))return new Event(at.toEpochMilli(),1);
        }
        return null;
    }
    public static boolean shouldNotifySunrise(Schedule schedule,long epochMillis,Instant now,long lastNotified) {
        if(epochMillis<=0||epochMillis==lastNotified)return false;
        long delay=now.toEpochMilli()-epochMillis;
        if(delay<0||delay>MAX_LATENESS_MILLIS)return false;
        ZonedDateTime at=Instant.ofEpochMilli(epochMillis).atZone(Schedule.ZONE);
        LocalTime[] times=schedule.days.get(at.toLocalDate());
        return times!=null&&times[1].equals(at.toLocalTime());
    }

    public static final class Reminder {
        public final long epochMillis, prayerEpochMillis;
        public final int prayerIndex, leadMinutes;
        Reminder(long epochMillis, long prayerEpochMillis, int prayerIndex, int leadMinutes) {
            this.epochMillis=epochMillis;this.prayerEpochMillis=prayerEpochMillis;
            this.prayerIndex=prayerIndex;this.leadMinutes=leadMinutes;
        }
    }
    public static boolean validLead(int minutes) {return minutes==5||minutes==10||minutes==15;}
    public static boolean remindersEnabled(int minutes,int mask) {return validLead(minutes)&&(mask&PRAYER_MASK)!=0;}
    public static Reminder nextReminder(Schedule schedule, Instant now, int minutes, int mask) {
        if(!remindersEnabled(minutes,mask))return null;
        // Search from today so a reminder can also precede the first covered prayer.
        LocalDate today=now.atZone(Schedule.ZONE).toLocalDate();
        for(LocalDate date:schedule.days.tailMap(today,true).keySet()){
            LocalTime[] times=schedule.days.get(date);
            for(int index:Schedule.PRAYERS){
                if((mask&(1<<index))==0)continue;
                long prayer=ZonedDateTime.of(date,times[index],Schedule.ZONE).toInstant().toEpochMilli();
                long reminder=prayer-minutes*60_000L;
                if(reminder>=now.toEpochMilli())return new Reminder(reminder,prayer,index,minutes);
            }
        }
        return null;
    }
    public static boolean shouldRemind(Schedule schedule, int index, long epochMillis, long prayerEpochMillis,
            int scheduledLead, int currentLead, int currentMask, Instant now, long lastRemindedPrayer) {
        if(!remindersEnabled(currentLead,currentMask)||scheduledLead!=currentLead||index<0||index>5||index==1)return false;
        if((currentMask&(1<<index))==0||prayerEpochMillis<=0||prayerEpochMillis==lastRemindedPrayer)return false;
        if(epochMillis!=prayerEpochMillis-currentLead*60_000L)return false;
        long delay=now.toEpochMilli()-epochMillis;
        if(delay<0||delay>MAX_REMINDER_LATENESS_MILLIS||now.toEpochMilli()>=prayerEpochMillis)return false;
        ZonedDateTime prayer=Instant.ofEpochMilli(prayerEpochMillis).atZone(Schedule.ZONE);
        LocalTime[] times=schedule.days.get(prayer.toLocalDate());
        return times!=null&&times[index].equals(prayer.toLocalTime());
    }
}
