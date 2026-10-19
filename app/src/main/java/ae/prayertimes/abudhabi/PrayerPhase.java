package ae.prayertimes.abudhabi;

import java.time.*;
import java.util.Map;

/** Display-only phase. Iqamah targets are user-adjustable estimates, not source prayer times. */
public final class PrayerPhase {
    public final int index;
    public final ZonedDateTime adhanAt, targetAt;
    public final boolean iqamah;

    private PrayerPhase(int index,ZonedDateTime adhanAt,ZonedDateTime targetAt,boolean iqamah){
        this.index=index;this.adhanAt=adhanAt;this.targetAt=targetAt;this.iqamah=iqamah;
    }

    public static PrayerPhase at(Schedule schedule,Instant now,boolean enabled,int[] offsets){
        if(enabled){
            LocalDate today=now.atZone(Schedule.ZONE).toLocalDate();
            Map.Entry<LocalDate,LocalTime[]> day=schedule.days.floorEntry(today);
            // The latest saved adhan may still have an estimated iqamah pending, including
            // the final Isha in the timetable. Never manufacture an adhan after coverage.
            while(day!=null){
                boolean found=false;
                for(int p=Schedule.PRAYERS.length-1;p>=0;p--){
                    int index=Schedule.PRAYERS[p];
                    ZonedDateTime adhan=ZonedDateTime.of(day.getKey(),day.getValue()[index],Schedule.ZONE);
                    if(adhan.toInstant().isAfter(now))continue;
                    int minutes=offsets!=null&&offsets.length>index?offsets[index]:0;
                    boolean fridayDhuhr=index==2&&day.getKey().getDayOfWeek()==DayOfWeek.FRIDAY;
                    if(minutes>=1&&minutes<=60&&!fridayDhuhr){
                        ZonedDateTime target=adhan.plusMinutes(minutes);
                        if(now.isBefore(target.toInstant()))return new PrayerPhase(index,adhan,target,true);
                    }
                    found=true;
                    break;
                }
                if(found)break;
                day=schedule.days.lowerEntry(day.getKey());
            }
        }
        // The displayed phase passes an adhan boundary even when iqamah is disabled.
        // Scheduling and notification policy continue to use Schedule.next unchanged.
        Schedule.Next next=schedule.next(now);
        if(next!=null&&next.at.toInstant().equals(now))next=schedule.next(now.plusNanos(1));
        return next==null?null:new PrayerPhase(next.index,next.at,next.at,false);
    }
}
