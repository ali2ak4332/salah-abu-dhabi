import ae.prayertimes.abudhabi.Schedule;
import java.io.*;
import java.time.*;
public class ScheduleTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Instant at(String local){return LocalDateTime.parse(local).atZone(Schedule.ZONE).toInstant();}
    public static void main(String[] args)throws Exception{
        Schedule s=new Schedule(new FileReader(args[0]));
        check(s.days.size()==447,"447 days");
        check(s.days.firstKey().toString().equals("2026-10-11"),"start");
        check(s.days.lastKey().toString().equals("2027-12-31"),"end");
        check(s.next(at("2026-10-11T04:00")).index==0,"before Fajr");
        check(s.next(at("2026-10-11T05:03")).index==2,"skip sunrise");
        check(s.next(at("2026-10-11T18:04")).index==5,"after Maghrib");
        Schedule.Next n=s.next(at("2026-10-11T20:00"));
        check(n.index==0&&n.at.toLocalDate().toString().equals("2026-10-12"),"tomorrow Fajr");
        check(s.next(at("2026-10-10T20:00"))==null,"before coverage");
        check(s.next(at("2027-12-31T23:00"))==null,"after final Isha");
        check(s.next(at("2028-01-01T01:00"))==null,"expired");
        check(s.next(at("2027-10-10T23:00")).at.toLocalDate().equals(LocalDate.of(2027,10,11)),"old end now continues to verified extension");
        check(s.next(at("2027-12-30T23:00")).at.toLocalDate().equals(LocalDate.of(2027,12,31)),"final day available");
        check(s.clamp(LocalDate.of(2028,1,1)).equals(LocalDate.of(2027,12,31)),"picker clamps to final official date");
        check(s.next(Instant.parse("2026-10-11T20:30:00Z")).at.toLocalDate().toString().equals("2026-10-12"),"UAE midnight");
        for(LocalDate d:s.days.keySet())for(int i:Schedule.PRAYERS){
            Instant instant=ZonedDateTime.of(d,s.days.get(d)[i],Schedule.ZONE).toInstant();
            check(s.next(instant).index==i,"exact time "+d+" "+i);
            Schedule.Next after=s.next(instant.plusSeconds(1));
            if(after!=null)check(after.at.toInstant().isAfter(instant),"no past countdown");
        }
        try{new Schedule(new StringReader("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n2026-01-01,05:00,06:00,12:00,15:00,18:00,19:00\n2026-01-03,05:00,06:00,12:00,15:00,18:00,19:00"));throw new AssertionError("gap accepted");}catch(IOException expected){}
        System.out.println("PASS: coverage, midnight, time zone, sunrise exclusion, expiry, and all 2,235 prayer boundaries.");
    }
}
