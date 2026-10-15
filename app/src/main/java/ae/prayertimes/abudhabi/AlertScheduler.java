package ae.prayertimes.abudhabi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import java.io.*;
import java.time.*;
import java.util.Locale;

public final class AlertScheduler {
    public static final String ACTION="ae.prayertimes.abudhabi.PRAYER_TICK";
    public static final String REMINDER_ACTION="ae.prayertimes.abudhabi.ADVANCE_REMINDER";
    public static final String IQAMAH_WIDGET_ACTION="ae.prayertimes.abudhabi.IQAMAH_WIDGET_TICK";
    public static final String CHANNEL="prayer_chime_v1";
    public static final String REMINDER_CHANNEL="prayer_advance_v1";
    public static final String ENABLED="alerts_enabled";
    public static final String REMINDER_MINUTES="reminder_minutes", REMINDER_MASK="reminder_mask";
    private static Schedule cached;
    public static SharedPreferences prefs(Context c){return c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);}
    public static boolean arabic(Context c){return prefs(c).getBoolean("arabic",Locale.getDefault().getLanguage().equals("ar"));}
    public static boolean enabled(Context c){return prefs(c).getBoolean(ENABLED,false);}
    public static int reminderMinutes(Context c){int m=prefs(c).getInt(REMINDER_MINUTES,0);return AlertPolicy.validLead(m)?m:0;}
    public static int reminderMask(Context c){return prefs(c).getInt(REMINDER_MASK,AlertPolicy.PRAYER_MASK)&AlertPolicy.PRAYER_MASK;}
    public static boolean remindersEnabled(Context c){return AlertPolicy.remindersEnabled(reminderMinutes(c),reminderMask(c));}
    public static boolean anyAlertsEnabled(Context c){return enabled(c)||remindersEnabled(c);}
    public static synchronized Schedule schedule(Context c)throws IOException{
        if(cached==null)try(Reader r=new InputStreamReader(c.getAssets().open("times.csv"),"UTF-8")){cached=new Schedule(r);}
        return cached;
    }
    public static void createChannel(Context c){
        createChannel(c,CHANNEL,"Prayer chime","Short chime at Fajr, Dhuhr, Asr, Maghrib and Isha, using Abu Dhabi times.");
        createChannel(c,REMINDER_CHANNEL,"Advance prayer reminders","Optional reminders before selected Abu Dhabi prayers.");
    }
    private static void createChannel(Context c,String id,String name,String description){
        NotificationChannel channel=new NotificationChannel(id,name,NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(description);
        channel.setSound(Uri.parse("android.resource://"+c.getPackageName()+"/raw/prayer_chime"),new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        channel.enableVibration(false);
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    private static boolean channelAllowed(Context c,String channel){
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false;
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        NotificationChannel ch=nm.getNotificationChannel(channel);
        return nm.areNotificationsEnabled()&&(ch==null||ch.getImportance()!=NotificationManager.IMPORTANCE_NONE);
    }
    public static boolean notificationsAllowed(Context c){return channelAllowed(c,CHANNEL);}
    public static boolean reminderNotificationsAllowed(Context c){return channelAllowed(c,REMINDER_CHANNEL);}
    public static boolean precise(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
    public static boolean soundEnabled(Context c){NotificationChannel ch=c.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL);return ch!=null&&ch.getSound()!=null&&ch.getImportance()>=NotificationManager.IMPORTANCE_DEFAULT;}
    private static PendingIntent pending(Context c,int flags,AlertPolicy.Event event){
        Intent i=new Intent(c,AlarmReceiver.class).setAction(ACTION);
        if(event!=null)i.putExtra("at",event.epochMillis).putExtra("prayer",event.prayerIndex);
        return PendingIntent.getBroadcast(c,701,i,flags|PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent reminderPending(Context c,int flags,AlertPolicy.Reminder event){
        Intent i=new Intent(c,AlarmReceiver.class).setAction(REMINDER_ACTION);
        if(event!=null)i.putExtra("at",event.epochMillis).putExtra("prayer",event.prayerIndex).putExtra("prayer_at",event.prayerEpochMillis).putExtra("lead",event.leadMinutes);
        return PendingIntent.getBroadcast(c,703,i,flags|PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent iqamahPending(Context c,int flags,long at){
        Intent i=new Intent(c,AlarmReceiver.class).setAction(IQAMAH_WIDGET_ACTION);
        if(at>0)i.putExtra("at",at);
        return PendingIntent.getBroadcast(c,704,i,flags|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void cancel(AlarmManager am,PendingIntent pi){if(pi!=null){am.cancel(pi);pi.cancel();}}
    public static void reschedule(Context c){reschedule(c,Instant.now());}
    static synchronized void reschedule(Context c,Instant after){
        AlarmManager am=c.getSystemService(AlarmManager.class);SharedPreferences p=prefs(c);
        // Settle each independent pending delivery before replacing either alarm. A late
        // reminder must never erase a regular prayer alert already waiting for delivery.
        Instant now=Instant.now();
        try{deliverPrayer(c,p.getInt("scheduled_prayer",-1),p.getLong("scheduled_at",0),now);}
        catch(Exception e){Log.e("PrayerSchedule","Cannot settle pending prayer alert",e);}
        try{deliverReminder(c,p.getInt("scheduled_reminder_prayer",-1),p.getLong("scheduled_reminder_at",0),p.getLong("scheduled_reminder_prayer_at",0),p.getInt("scheduled_reminder_lead",0),now);}
        catch(Exception e){Log.e("PrayerSchedule","Cannot settle pending advance reminder",e);}
        cancel(am,pending(c,PendingIntent.FLAG_NO_CREATE,null));
        cancel(am,reminderPending(c,PendingIntent.FLAG_NO_CREATE,null));
        cancel(am,iqamahPending(c,PendingIntent.FLAG_NO_CREATE,0));
        p.edit().remove("scheduled_at").remove("scheduled_prayer").remove("scheduled_reminder_at").remove("scheduled_reminder_prayer").remove("scheduled_reminder_prayer_at").remove("scheduled_reminder_lead").remove("scheduled_iqamah_at").remove("schedule_error").apply();
        try{
            boolean alerts=enabled(c)&&notificationsAllowed(c);boolean widgets=PrayerWidget.hasWidgets(c);
            AlertPolicy.Event event=AlertPolicy.next(schedule(c),after,alerts,widgets);
            if(event!=null&&event.prayerIndex>=0&&event.epochMillis==p.getLong("last_alert",0))event=AlertPolicy.next(schedule(c),Instant.ofEpochMilli(event.epochMillis+1),alerts,widgets);
            if(event!=null){
                PendingIntent pi=pending(c,PendingIntent.FLAG_UPDATE_CURRENT,event);
                try{if(precise(c))am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,event.epochMillis,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,event.epochMillis,pi);}
                catch(SecurityException revoked){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,event.epochMillis,pi);}
                p.edit().putLong("scheduled_at",event.epochMillis).putInt("scheduled_prayer",event.prayerIndex).apply();
            }
        }catch(Exception e){p.edit().putBoolean("schedule_error",true).apply();Log.e("PrayerSchedule","Cannot schedule prayer timetable",e);}
        try{
            if(PrayerWidget.hasWidgets(c)){
                PrayerPhase phase=AppPreferences.phase(c,schedule(c),after);
                if(phase!=null&&phase.iqamah){
                    long target=phase.targetAt.toInstant().toEpochMilli();
                    PendingIntent pi=iqamahPending(c,PendingIntent.FLAG_UPDATE_CURRENT,target);
                    // Display-only transition: keep it separate and silent, and preserve
                    // the idle-alarm quota for prayer alerts. Launcher updates can defer in Doze.
                    try{if(precise(c))am.setExact(AlarmManager.RTC_WAKEUP,target,pi);else am.set(AlarmManager.RTC_WAKEUP,target,pi);}
                    catch(SecurityException revoked){am.set(AlarmManager.RTC_WAKEUP,target,pi);}
                    p.edit().putLong("scheduled_iqamah_at",target).apply();
                }
            }
        }catch(Exception e){Log.e("PrayerSchedule","Cannot schedule estimated iqamah widget transition",e);}
        try{
            if(!remindersEnabled(c)||!reminderNotificationsAllowed(c))return;
            AlertPolicy.Reminder event=AlertPolicy.nextReminder(schedule(c),after,reminderMinutes(c),reminderMask(c));
            if(event!=null&&event.prayerEpochMillis==p.getLong("last_reminder_prayer",0))event=AlertPolicy.nextReminder(schedule(c),Instant.ofEpochMilli(event.epochMillis+1),reminderMinutes(c),reminderMask(c));
            if(event!=null){
                PendingIntent pi=reminderPending(c,PendingIntent.FLAG_UPDATE_CURRENT,event);
                // Do not consume Android's idle-alarm quota shortly before the prayer.
                // Advance reminders may be deferred during Doze; stale ones are skipped.
                try{if(precise(c))am.setExact(AlarmManager.RTC_WAKEUP,event.epochMillis,pi);else am.set(AlarmManager.RTC_WAKEUP,event.epochMillis,pi);}
                catch(SecurityException revoked){am.set(AlarmManager.RTC_WAKEUP,event.epochMillis,pi);}
                p.edit().putLong("scheduled_reminder_at",event.epochMillis).putInt("scheduled_reminder_prayer",event.prayerIndex).putLong("scheduled_reminder_prayer_at",event.prayerEpochMillis).putInt("scheduled_reminder_lead",event.leadMinutes).apply();
            }
        }catch(Exception e){p.edit().putBoolean("schedule_error",true).apply();Log.e("PrayerSchedule","Cannot schedule advance reminder",e);}
    }
    private static void deliverPrayer(Context c,int index,long epoch,Instant now)throws IOException{
        if(enabled(c)&&notificationsAllowed(c)&&AlertPolicy.shouldNotify(schedule(c),index,epoch,now,prefs(c).getLong("last_alert",0)))
            if(notify(c,index,epoch,false))prefs(c).edit().putLong("last_alert",epoch).commit();
    }
    private static void deliverReminder(Context c,int index,long epoch,long prayerEpoch,int lead,Instant now)throws IOException{
        if(reminderNotificationsAllowed(c)&&AlertPolicy.shouldRemind(schedule(c),index,epoch,prayerEpoch,lead,reminderMinutes(c),reminderMask(c),now,prefs(c).getLong("last_reminder_prayer",0)))
            if(notifyReminder(c,index,epoch,prayerEpoch,now))prefs(c).edit().putLong("last_reminder_prayer",prayerEpoch).commit();
    }
    public static synchronized void received(Context c,Intent i){
        try{
            createChannel(c);Instant now=Instant.now();long epoch=i.getLongExtra("at",0);int index=i.getIntExtra("prayer",-1);
            if(ACTION.equals(i.getAction()))deliverPrayer(c,index,epoch,now);
            else if(REMINDER_ACTION.equals(i.getAction()))deliverReminder(c,index,epoch,i.getLongExtra("prayer_at",0),i.getIntExtra("lead",0),now);
            PrayerWidget.updateAll(c);
            // At the exact boundary, move past the delivered event to prevent rearming it.
            Instant after=now;
            if((ACTION.equals(i.getAction())||REMINDER_ACTION.equals(i.getAction())||IQAMAH_WIDGET_ACTION.equals(i.getAction()))&&epoch<=now.toEpochMilli())after=Instant.ofEpochMilli(Math.max(now.toEpochMilli(),epoch+1));
            reschedule(c,after);
        }catch(Exception e){Log.e("PrayerSchedule","Cannot update prayer alerts",e);reschedule(c);}
    }
    public static boolean test(Context c){createChannel(c);return notify(c,-1,System.currentTimeMillis(),true);}
    private static String name(Context c,int index){String[] en={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"},ar={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};return arabic(c)?ar[index]:en[index];}
    private static boolean notify(Context c,int index,long epoch,boolean test){
        if(!notificationsAllowed(c))return false;
        boolean ar=arabic(c);
        String title=test?(ar?"تجربة تنبيه الصلاة":"Prayer chime test"):(ar?"صلاة "+name(c,index):name(c,index)+" prayer");
        String time=AppPreferences.time(c,Instant.ofEpochMilli(epoch).atZone(Schedule.ZONE).toLocalTime());
        String body=test?(ar?"هذا هو صوت التنبيه القصير للصلاة.":"This is your short prayer notification sound."):(ar?"أبوظبي · "+time+" · توقيت الإمارات":"Abu Dhabi · "+time+" UAE time");
        return post(c,CHANNEL,test?799:710+index,title,body,epoch,60*60*1000);
    }
    private static boolean notifyReminder(Context c,int index,long epoch,long prayerEpoch,Instant now){
        if(!reminderNotificationsAllowed(c)||now.toEpochMilli()>=prayerEpoch)return false;
        boolean ar=arabic(c);String prayer=name(c,index);
        String time=AppPreferences.time(c,Instant.ofEpochMilli(prayerEpoch).atZone(Schedule.ZONE).toLocalTime());
        String minutes=AppPreferences.digits(c,Long.toString((prayerEpoch-now.toEpochMilli()+59_999L)/60_000L));
        String title=ar?"تذكير بصلاة "+prayer:prayer+" reminder";
        String body=ar?"باقي نحو "+minutes+" دقيقة · "+time+" · أبوظبي":"In about "+minutes+" min · "+time+" · Abu Dhabi";
        return post(c,REMINDER_CHANNEL,730+index,title,body,epoch,prayerEpoch-now.toEpochMilli());
    }
    private static boolean post(Context c,String channel,int id,String title,String body,long epoch,long timeout){
        PendingIntent open=PendingIntent.getActivity(c,702,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(c,channel).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setColor(AppPreferences.palette(c).accent).setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).setVisibility(Notification.VISIBILITY_PUBLIC).setWhen(epoch).setShowWhen(true).setTimeoutAfter(timeout).build();
        try{c.getSystemService(NotificationManager.class).notify(id,n);return true;}catch(SecurityException e){return false;}
    }
}
