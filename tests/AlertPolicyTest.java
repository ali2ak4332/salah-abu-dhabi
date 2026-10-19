import ae.prayertimes.abudhabi.*;
import java.io.*;
import java.time.*;

public class AlertPolicyTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static Instant at(String s){return LocalDateTime.parse(s).atZone(Schedule.ZONE).toInstant();}
    public static void main(String[] args)throws Exception {
        Schedule s=new Schedule(new FileReader(args[0]));
        check(AlertPolicy.next(s,at("2026-10-11T20:00"),false,false)==null,"disabled must cancel");
        check(AlertPolicy.next(s,at("2026-10-10T20:00"),true,false).epochMillis==at("2026-10-11T00:00").toEpochMilli(),"coverage start wakeup");
        AlertPolicy.Event midnight=AlertPolicy.next(s,at("2026-10-11T20:00"),true,true);
        check(midnight.prayerIndex==-1&&midnight.epochMillis==at("2026-10-12T00:00").toEpochMilli(),"widget midnight refresh");
        AlertPolicy.Event fajr=AlertPolicy.next(s,Instant.ofEpochMilli(midnight.epochMillis+1),true,true);
        check(fajr.prayerIndex==0&&fajr.epochMillis==at("2026-10-12T05:02").toEpochMilli(),"rearm Fajr after midnight");
        check(AlertPolicy.next(s,at("2026-10-11T20:00"),true,false).prayerIndex==0,"alert-only Fajr");
        check(AlertPolicy.next(s,at("2028-01-01T00:00"),true,true)==null,"expired stops");
        check(AlertPolicy.next(s,at("2027-12-31T23:00"),true,true).prayerIndex==-1,"last midnight clears widget");
        int checks=0;
        for(LocalDate d:s.days.keySet())for(int i:Schedule.PRAYERS){
            long epoch=ZonedDateTime.of(d,s.days.get(d)[i],Schedule.ZONE).toInstant().toEpochMilli();
            Instant due=Instant.ofEpochMilli(epoch);
            check(AlertPolicy.shouldNotify(s,i,epoch,due,0),"at prayer");
            check(!AlertPolicy.shouldNotify(s,i,epoch,due.minusMillis(1),0),"never early");
            check(!AlertPolicy.shouldNotify(s,i,epoch,due,epoch),"no duplicate");
            check(AlertPolicy.shouldNotify(s,i,epoch,due.plusSeconds(15*60),0),"allowed late window");
            check(!AlertPolicy.shouldNotify(s,i,epoch,due.plusSeconds(15*60+1),0),"skip stale");
            check(!AlertPolicy.shouldNotify(s,i,epoch+1000,due.plusSeconds(2),0),"validate source timestamp");
            AlertPolicy.Event next=AlertPolicy.next(s,due.plusMillis(1),true,true);
            if(next!=null)check(next.epochMillis>epoch,"reschedule strictly future");
            checks++;
        }
        long sunrise=at("2026-10-11T06:15").toEpochMilli();
        check(!AlertPolicy.shouldNotify(s,1,sunrise,Instant.ofEpochMilli(sunrise),0),"no sunrise notification");
        check(!AlertPolicy.shouldNotify(s,6,sunrise,Instant.ofEpochMilli(sunrise),0),"invalid index");
        System.out.println("PASS: alert opt-out, midnight, expiry, stale/duplicate/early protection and all "+checks+" prayer deliveries.");
        check(!AlertPolicy.remindersEnabled(0,61),"reminders opt in");
        check(!AlertPolicy.remindersEnabled(7,61),"invalid reminder lead rejected");
        check(!AlertPolicy.remindersEnabled(5,2),"sunrise-only mask rejected");
        check(!AlertPolicy.remindersEnabled(5,0),"no selected prayers");
        check(AlertPolicy.nextReminder(s,at("2026-10-11T00:00"),0,61)==null,"reminders off");
        check(AlertPolicy.nextReminder(s,at("2028-01-01T00:00"),5,61)==null,"no reminders outside data end");
        check(AlertPolicy.next(s,at("2026-10-11T00:00"),false,false)==null&&AlertPolicy.nextReminder(s,at("2026-10-11T00:00"),5,61)!=null,"reminder enabled without regular alerts");
        check(AlertPolicy.next(s,at("2026-10-11T00:00"),true,false)!=null&&AlertPolicy.nextReminder(s,at("2026-10-11T00:00"),0,61)==null,"regular alerts enabled without reminders");
        int reminderChecks=0;
        for(LocalDate d:s.days.keySet())for(int i:Schedule.PRAYERS)for(int lead:new int[]{5,10,15}){
            long prayer=ZonedDateTime.of(d,s.days.get(d)[i],Schedule.ZONE).toInstant().toEpochMilli();
            long epoch=prayer-lead*60_000L;Instant due=Instant.ofEpochMilli(epoch);
            AlertPolicy.Reminder next=AlertPolicy.nextReminder(s,due,lead,61);
            check(next!=null&&next.prayerIndex==i&&next.epochMillis==epoch&&next.prayerEpochMillis==prayer&&next.leadMinutes==lead,"exact reminder source event");
            check(AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,lead,61,due,0),"reminder at exact time");
            check(!AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,lead,61,due.minusMillis(1),0),"reminder never early");
            check(AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,lead,61,due.plusMillis(AlertPolicy.MAX_REMINDER_LATENESS_MILLIS),0),"reminder bounded late window");
            check(!AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,lead,61,due.plusMillis(AlertPolicy.MAX_REMINDER_LATENESS_MILLIS+1),0),"stale reminder skipped");
            check(!AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,lead,61,Instant.ofEpochMilli(prayer),0),"reminder never at actual prayer");
            check(!AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,lead,61,due,prayer),"one reminder per prayer");
            check(!AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,0,61,due,0),"disabled settings reject old reminder");
            check(!AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,lead==5?10:5,61,due,0),"changed lead rejects old reminder");
            check(!AlertPolicy.shouldRemind(s,i,epoch,prayer,lead,lead,61&~(1<<i),due,0),"deselected prayer rejects old reminder");
            check(!AlertPolicy.shouldRemind(s,i,epoch+1000,prayer+1000,lead,lead,61,due.plusSeconds(2),0),"validate actual source prayer time");
            check(!AlertPolicy.shouldRemind(s,i,epoch+1000,prayer,lead,lead,61,due.plusSeconds(2),0),"validate exact reminder offset");
            AlertPolicy.Reminder nextPrayer=AlertPolicy.nextReminder(s,due.plusMillis(1),lead,61);
            if(nextPrayer!=null)check(nextPrayer.epochMillis>epoch,"next reminder strictly future");
            AlertPolicy.Reminder only=AlertPolicy.nextReminder(s,d.atStartOfDay(Schedule.ZONE).toInstant(),lead,1<<i);
            check(only.prayerIndex==i&&only.epochMillis==epoch,"selected prayer mask");
            reminderChecks++;
        }
        long firstFajr=at("2026-10-11T05:02").toEpochMilli();
        AlertPolicy.Reminder coverage=AlertPolicy.nextReminder(s,at("2026-10-01T00:00"),15,61);
        check(coverage!=null&&coverage.prayerEpochMillis==firstFajr,"reminder before coverage schedules first source prayer");
        AlertPolicy.Reminder tomorrow=AlertPolicy.nextReminder(s,at("2026-10-11T23:59"),15,61);
        check(tomorrow!=null&&tomorrow.prayerEpochMillis==at("2026-10-12T05:02").toEpochMilli(),"midnight reminder remains next Fajr");
        check(!AlertPolicy.shouldRemind(s,1,sunrise-300_000,sunrise,5,5,63,Instant.ofEpochMilli(sunrise-300_000),0),"no sunrise reminder even bad persisted mask");
        Schedule midnightSchedule=new Schedule(new StringReader("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n2026-10-11,00:05,01:00,10:00,14:00,18:00,19:00\n"));
        AlertPolicy.Reminder beforeMidnight=AlertPolicy.nextReminder(midnightSchedule,at("2026-10-10T20:00"),15,1);
        check(beforeMidnight.epochMillis==at("2026-10-10T23:50").toEpochMilli(),"advance reminder crosses previous midnight");
        check(AlertPolicy.shouldRemind(midnightSchedule,0,beforeMidnight.epochMillis,beforeMidnight.prayerEpochMillis,15,15,1,Instant.ofEpochMilli(beforeMidnight.epochMillis),0),"cross-midnight delivery validates prayer's covered date");
        System.out.println("PASS: independent reminders, opt-in/masks, setting changes, midnight/coverage, never early or at prayer, duplicates/stale checks and all "+reminderChecks+" reminder boundaries.");
    }
}
