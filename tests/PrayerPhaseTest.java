import ae.prayertimes.abudhabi.*;
import java.io.*;
import java.time.*;

public final class PrayerPhaseTest {
    static final int[] DEFAULTS={25,0,20,20,5,20};
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    static Instant at(String value){return LocalDateTime.parse(value).atZone(Schedule.ZONE).toInstant();}
    static void nextAdhan(Schedule schedule,Instant now,PrayerPhase phase,String label){
        Schedule.Next next=schedule.next(now);
        if(next!=null&&next.at.toInstant().equals(now))next=schedule.next(now.plusNanos(1));
        check((next==null)==(phase==null),label+" availability");
        if(next!=null)check(!phase.iqamah&&phase.index==next.index&&phase.adhanAt.equals(next.at)&&phase.targetAt.equals(next.at),label+" next adhan");
    }
    static void iqamah(PrayerPhase phase,int index,ZonedDateTime adhan,int offset,String label){
        check(phase!=null&&phase.iqamah&&phase.index==index&&phase.adhanAt.equals(adhan)&&phase.targetAt.equals(adhan.plusMinutes(offset)),label);
    }
    public static void main(String[] args)throws Exception{
        Schedule schedule=new Schedule(new FileReader(args[0]));
        int checked=0,fridays=0;
        for(LocalDate date:schedule.days.keySet())for(int index:Schedule.PRAYERS){
            ZonedDateTime adhan=ZonedDateTime.of(date,schedule.days.get(date)[index],Schedule.ZONE);
            Instant begin=adhan.toInstant(),end=adhan.plusMinutes(DEFAULTS[index]).toInstant();
            PrayerPhase before=PrayerPhase.at(schedule,begin.minusMillis(1),true,DEFAULTS);
            check(before!=null&&!before.iqamah&&before.index==index&&before.targetAt.equals(adhan),"before adhan counts down to source time");
            PrayerPhase submillisecond=PrayerPhase.at(schedule,begin.minusNanos(1),true,DEFAULTS);
            check(submillisecond!=null&&!submillisecond.iqamah&&submillisecond.index==index&&submillisecond.targetAt.equals(adhan),"last nanosecond still targets upcoming adhan");
            boolean fridayDhuhr=index==2&&date.getDayOfWeek()==DayOfWeek.FRIDAY;
            if(fridayDhuhr){
                nextAdhan(schedule,begin,PrayerPhase.at(schedule,begin,true,DEFAULTS),"Friday Dhuhr exact");
                nextAdhan(schedule,end.minusMillis(1),PrayerPhase.at(schedule,end.minusMillis(1),true,DEFAULTS),"Friday Dhuhr no estimated iqamah");
                fridays++;
            }else{
                iqamah(PrayerPhase.at(schedule,begin,true,DEFAULTS),index,adhan,DEFAULTS[index],"at adhan begins estimated iqamah");
                iqamah(PrayerPhase.at(schedule,begin.plusMillis(1),true,DEFAULTS),index,adhan,DEFAULTS[index],"after adhan remains estimated iqamah");
                iqamah(PrayerPhase.at(schedule,end.minusMillis(1),true,DEFAULTS),index,adhan,DEFAULTS[index],"before iqamah boundary");
            }
            nextAdhan(schedule,end,PrayerPhase.at(schedule,end,true,DEFAULTS),"at iqamah boundary");
            nextAdhan(schedule,end.plusMillis(1),PrayerPhase.at(schedule,end.plusMillis(1),true,DEFAULTS),"after iqamah boundary");
            nextAdhan(schedule,begin,PrayerPhase.at(schedule,begin,false,DEFAULTS),"feature disabled at adhan");
            nextAdhan(schedule,begin.plusSeconds(1),PrayerPhase.at(schedule,begin.plusSeconds(1),false,DEFAULTS),"feature disabled after adhan");
            for(int invalid:new int[]{-1,0,61}){
                int[] custom=DEFAULTS.clone();custom[index]=invalid;
                nextAdhan(schedule,begin,PrayerPhase.at(schedule,begin,true,custom),"disabled or invalid offset");
            }
            for(int minutes:new int[]{1,60}){
                int[] custom=DEFAULTS.clone();custom[index]=minutes;
                if(!fridayDhuhr){
                    iqamah(PrayerPhase.at(schedule,begin,true,custom),index,adhan,minutes,"custom offset");
                    iqamah(PrayerPhase.at(schedule,adhan.plusMinutes(minutes).toInstant().minusMillis(1),true,custom),index,adhan,minutes,"custom offset end exclusive");
                }
                nextAdhan(schedule,adhan.plusMinutes(minutes).toInstant(),PrayerPhase.at(schedule,adhan.plusMinutes(minutes).toInstant(),true,custom),"custom offset transition");
            }
            checked++;
        }
        check(PrayerPhase.at(schedule,at("2026-10-10T23:59"),true,DEFAULTS)==null,"no adhan extrapolation before coverage");
        check(PrayerPhase.at(schedule,at("2028-01-01T00:00"),true,DEFAULTS)==null,"no adhan extrapolation after coverage");
        LocalDate last=schedule.days.lastKey();ZonedDateTime lastIsha=ZonedDateTime.of(last,schedule.days.get(last)[5],Schedule.ZONE);
        iqamah(PrayerPhase.at(schedule,lastIsha.toInstant().plusMillis(1),true,DEFAULTS),5,lastIsha,20,"last saved Isha retains iqamah phase");
        check(PrayerPhase.at(schedule,lastIsha.plusMinutes(20).toInstant(),true,DEFAULTS)==null,"end after final estimated iqamah");
        Instant first=ZonedDateTime.of(schedule.days.firstKey(),schedule.days.firstEntry().getValue()[0],Schedule.ZONE).toInstant();
        nextAdhan(schedule,first,PrayerPhase.at(schedule,first,true,null),"null offsets disable estimate");
        nextAdhan(schedule,first,PrayerPhase.at(schedule,first,true,new int[0]),"short offsets disable estimate");
        Schedule synthetic=new Schedule(new StringReader("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n2026-10-11,05:00,06:00,12:00,15:00,18:00,23:45\n"));
        int[] custom=DEFAULTS.clone();custom[5]=60;
        ZonedDateTime lateIsha=at("2026-10-11T23:45").atZone(Schedule.ZONE);
        iqamah(PrayerPhase.at(synthetic,at("2026-10-12T00:00"),true,custom),5,lateIsha,60,"last known iqamah crosses midnight");
        check(PrayerPhase.at(synthetic,at("2026-10-12T00:45"),true,custom)==null,"cross-midnight target expires without new times");
        Schedule overlap=new Schedule(new StringReader("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n2026-10-11,00:05,00:30,00:45,03:00,18:00,19:00\n"));
        int[] overlapping={60,60,0,20,5,20};
        iqamah(PrayerPhase.at(overlap,at("2026-10-11T00:30"),true,overlapping),0,at("2026-10-11T00:05").atZone(Schedule.ZONE),60,"sunrise never starts an iqamah phase");
        nextAdhan(overlap,at("2026-10-11T00:45"),PrayerPhase.at(overlap,at("2026-10-11T00:45"),true,overlapping),"new adhan ends any prior phase");
        System.out.println("PASS: all "+checked+" adhan/estimated-iqamah boundaries, "+fridays+" Friday Dhuhr exclusions, custom offsets, disablement, midnight and final coverage.");
    }
}
