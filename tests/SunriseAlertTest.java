import ae.prayertimes.abudhabi.*;
import java.io.*;
import java.time.*;
import java.util.*;

/** Exhaustive saved-timetable coverage for the independent Shurooq notification. */
public class SunriseAlertTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static long epoch(LocalDate day,LocalTime time){return ZonedDateTime.of(day,time,Schedule.ZONE).toInstant().toEpochMilli();}
    static void event(AlertPolicy.Event event,long expected,String message){
        check(event!=null,message+": missing event");
        check(event.prayerIndex==1,message+": sunrise index must be 1");
        check(event.epochMillis==expected,message+": must use the exact saved sunrise");
    }
    public static void main(String[] args)throws Exception{
        Schedule schedule=new Schedule(new FileReader(args[0]));
        LocalDate first=schedule.days.firstKey(),last=schedule.days.lastKey();
        long firstSunrise=epoch(first,schedule.days.get(first)[1]);
        long lastSunrise=epoch(last,schedule.days.get(last)[1]);
        check(AlertPolicy.nextSunrise(schedule,Instant.ofEpochMilli(firstSunrise),false)==null,"disabled sunrise has no alarm");
        event(AlertPolicy.nextSunrise(schedule,first.minusDays(10).atStartOfDay(Schedule.ZONE).toInstant(),true),firstSunrise,"before coverage");
        check(AlertPolicy.nextSunrise(schedule,Instant.ofEpochMilli(lastSunrise+1),true)==null,"no extrapolation after final saved sunrise");
        check(AlertPolicy.nextSunrise(schedule,last.plusDays(1).atStartOfDay(Schedule.ZONE).toInstant(),true)==null,"no event beyond coverage");
        int checked=0;
        for(Map.Entry<LocalDate,LocalTime[]> row:schedule.days.entrySet()){
            LocalDate day=row.getKey();
            long sunrise=epoch(day,row.getValue()[1]);Instant due=Instant.ofEpochMilli(sunrise);
            event(AlertPolicy.nextSunrise(schedule,day.atStartOfDay(Schedule.ZONE).toInstant(),true),sunrise,day+" midnight");
            event(AlertPolicy.nextSunrise(schedule,due.minusMillis(1),true),sunrise,day+" just before sunrise");
            event(AlertPolicy.nextSunrise(schedule,due,true),sunrise,day+" exact boundary");
            check(AlertPolicy.shouldNotifySunrise(schedule,sunrise,due,0),day+" deliver at official sunrise");
            check(!AlertPolicy.shouldNotifySunrise(schedule,sunrise,due.minusMillis(1),0),day+" never early");
            check(!AlertPolicy.shouldNotifySunrise(schedule,sunrise,due,sunrise),day+" suppress duplicate");
            check(AlertPolicy.shouldNotifySunrise(schedule,sunrise,due.plusMillis(AlertPolicy.MAX_LATENESS_MILLIS),0),day+" inclusive 15-minute grace");
            check(!AlertPolicy.shouldNotifySunrise(schedule,sunrise,due.plusMillis(AlertPolicy.MAX_LATENESS_MILLIS+1),0),day+" skip stale alarm");
            check(!AlertPolicy.shouldNotifySunrise(schedule,sunrise+1,due.plusMillis(1),0),day+" reject altered millisecond");
            check(!AlertPolicy.shouldNotifySunrise(schedule,sunrise+60_000,due.plusSeconds(60),0),day+" reject altered minute");
            check(!AlertPolicy.shouldNotify(schedule,1,sunrise,due,0),day+" five-prayer channel must still exclude sunrise");
            Schedule.Next nextPrayer=schedule.next(due);
            check(nextPrayer!=null&&nextPrayer.index==2,day+" next-prayer countdown remains Dhuhr");
            check(AlertPolicy.nextReminder(schedule,due,5,1<<1)==null,day+" sunrise must never enter prayer-reminder masks");
            for(int prayer:Schedule.PRAYERS){
                long prayerAt=epoch(day,row.getValue()[prayer]);
                check(!AlertPolicy.shouldNotifySunrise(schedule,prayerAt,Instant.ofEpochMilli(prayerAt),0),day+" prayer "+prayer+" cannot masquerade as sunrise");
            }
            LocalDate tomorrow=schedule.days.higherKey(day);
            AlertPolicy.Event following=AlertPolicy.nextSunrise(schedule,due.plusMillis(1),true);
            if(tomorrow==null)check(following==null,"final sunrise stops immediately after boundary");
            else{
                long next=epoch(tomorrow,schedule.days.get(tomorrow)[1]);
                event(following,next,day+" rearm next covered day");
                event(AlertPolicy.nextSunrise(schedule,day.atTime(23,59,59).atZone(Schedule.ZONE).toInstant(),true),next,day+" midnight rollover");
            }
            checked++;
        }
        long before=epoch(first.minusDays(1),schedule.days.get(first)[1]);
        long after=epoch(last.plusDays(1),schedule.days.get(last)[1]);
        check(!AlertPolicy.shouldNotifySunrise(schedule,before,Instant.ofEpochMilli(before),0),"no delivery before saved coverage");
        check(!AlertPolicy.shouldNotifySunrise(schedule,after,Instant.ofEpochMilli(after),0),"no delivery after saved coverage");
        check(!AlertPolicy.shouldNotifySunrise(schedule,0,Instant.EPOCH,0),"zero timestamp rejected");
        check(!AlertPolicy.shouldNotifySunrise(schedule,-1,Instant.EPOCH,0),"negative timestamp rejected");
        TimeZone original=TimeZone.getDefault();
        try{
            for(String zone:new String[]{"UTC","America/New_York","Asia/Tokyo"}){
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                event(AlertPolicy.nextSunrise(schedule,Instant.ofEpochMilli(firstSunrise),true),firstSunrise,"device zone "+zone);
                check(AlertPolicy.shouldNotifySunrise(schedule,firstSunrise,Instant.ofEpochMilli(firstSunrise),0),"delivery remains fixed to Abu Dhabi in "+zone);
            }
        }finally{TimeZone.setDefault(original);}
        check(Arrays.equals(Schedule.PRAYERS,new int[]{0,2,3,4,5}),"five-prayer list remains unchanged");
        check(AlertPolicy.PRAYER_MASK==61,"prayer reminder mask remains unchanged");
        System.out.println("PASS: independent sunrise policy for all "+checked+" saved days; exact/early/late/duplicate/source validation, coverage, timezone and five-prayer separation.");
    }
}
