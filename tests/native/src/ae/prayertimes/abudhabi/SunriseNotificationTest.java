package ae.prayertimes.abudhabi;

import static org.junit.Assert.*;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.ListView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.lang.reflect.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;

/** Independent alarm, Android permission, delivery, recovery and bilingual settings coverage. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class SunriseNotificationTest {
    private static final Instant MORNING=LocalDateTime.parse("2026-10-17T00:00").atZone(Schedule.ZONE).toInstant();
    private Context context;
    private SharedPreferences prefs;
    private NotificationManager notifications;
    private ShadowNotificationManager shadowNotifications;
    private ShadowAlarmManager alarms;
    private Field cacheField;
    private Object originalCache;
    private ActivityController<FixedActivity> controller;

    public static class FixedActivity extends MainActivity {
        static Instant time=MORNING;
        @Override protected Instant now(){return time;}
    }

    @Implements(AlarmManager.class)
    public static class RevokedExactAlarmManager extends ShadowAlarmManager {
        static int attempts;
        @Implementation protected void setExactAndAllowWhileIdle(int type,long when,PendingIntent pending){
            attempts++;
            throw new SecurityException("Exact alarm access revoked between check and set");
        }
    }

    @Implements(AlarmManager.class)
    public static class SelectiveFailureAlarmManager extends ShadowAlarmManager {
        static String failingAction;
        @Implementation protected void setExactAndAllowWhileIdle(int type,long when,PendingIntent pending){
            if(failingAction.equals(Shadows.shadowOf(pending).getSavedIntent().getAction()))throw new IllegalStateException("Injected scheduling failure");
            super.setExactAndAllowWhileIdle(type,when,pending);
        }
    }

    @Before public void reset()throws Exception{
        context=RuntimeEnvironment.getApplication();
        RuntimeEnvironment.setQualifiers("en-rUS-w360dp-h800dp-port-mdpi");
        RuntimeEnvironment.setFontScale(1f);
        prefs=AlertScheduler.prefs(context);prefs.edit().clear().putBoolean("arabic",false).commit();
        if(Build.VERSION.SDK_INT>=33)Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        notifications=context.getSystemService(NotificationManager.class);
        shadowNotifications=Shadows.shadowOf(notifications);
        shadowNotifications.setNotificationsEnabled(true);
        alarms=Shadows.shadowOf(context.getSystemService(AlarmManager.class));
        ShadowAlarmManager.setCanScheduleExactAlarms(true);
        ShadowAlarmManager.setAutoSchedule(false);
        cacheField=AlertScheduler.class.getDeclaredField("cached");cacheField.setAccessible(true);
        originalCache=cacheField.get(null);cacheField.set(null,null);
        FixedActivity.time=MORNING;
        AlertScheduler.createChannel(context);
    }
    @After public void restore()throws Exception{
        stopActivity();
        if(cacheField!=null)cacheField.set(null,originalCache);
    }
    private void stopActivity(){if(controller!=null){controller.pause().stop().destroy();controller=null;}}
    private FixedActivity launch(boolean arabic){
        stopActivity();prefs.edit().putBoolean("arabic",arabic).commit();
        RuntimeEnvironment.setQualifiers((arabic?"ar":"en")+"-rUS-w360dp-h800dp-port-mdpi");
        controller=Robolectric.buildActivity(FixedActivity.class).create().start().resume().visible();
        return controller.get();
    }
    private void invoke(FixedActivity activity,String method)throws Exception{
        Method m=MainActivity.class.getDeclaredMethod(method);m.setAccessible(true);m.invoke(activity);
    }
    private AlertDialog dialog(){AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertNotNull(d);assertTrue(d.isShowing());return d;}
    private void select(AlertDialog d,int index){
        ListView list=d.getListView();
        list.performItemClick(list.getChildAt(index),index,list.getAdapter().getItemId(index));
    }
    private void screenshotDialog(AlertDialog dialog,String name)throws Exception{
        View view=dialog.getWindow().getDecorView();
        view.measure(View.MeasureSpec.makeMeasureSpec(344,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.AT_MOST));
        view.layout(0,0,344,view.getMeasuredHeight());
        Bitmap image=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(image));
        File directory=new File("screenshots/v2.6");directory.mkdirs();
        try(FileOutputStream stream=new FileOutputStream(new File(directory,name+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,stream));}
        image.recycle();
    }
    private ShadowAlarmManager.ScheduledAlarm alarm(String action){
        ShadowAlarmManager.ScheduledAlarm found=null;
        for(ShadowAlarmManager.ScheduledAlarm item:alarms.getScheduledAlarms()){
            if(item.operation!=null&&action.equals(Shadows.shadowOf(item.operation).getSavedIntent().getAction())){
                assertNull("Only one alarm for "+action,found);found=item;
            }
        }
        return found;
    }
    private long officialSunrise(LocalDate date)throws Exception{
        Schedule s=AlertScheduler.schedule(context);
        return date.atTime(s.days.get(date)[1]).atZone(Schedule.ZONE).toInstant().toEpochMilli();
    }
    private void allAlerts(){prefs.edit().putBoolean(AlertScheduler.ENABLED,true).putInt(AlertScheduler.REMINDER_MINUTES,5).putInt(AlertScheduler.REMINDER_MASK,61).commit();}
    private Intent sunriseIntent(long epoch){return new Intent(context,AlarmReceiver.class).setAction(AlertScheduler.SUNRISE_ACTION).putExtra("at",epoch).putExtra("prayer",1);}
    private void receive(long epoch){new AlarmReceiver().onReceive(context,sunriseIntent(epoch));}
    private void deliver(long epoch,Instant now)throws Exception{
        Method m=AlertScheduler.class.getDeclaredMethod("deliverSunrise",Context.class,long.class,Instant.class);m.setAccessible(true);m.invoke(null,context,epoch,now);
    }
    private void blockChannel(String channel){
        NotificationChannel blocked=notifications.getNotificationChannel(channel);
        blocked.setImportance(NotificationManager.IMPORTANCE_NONE);
        notifications.createNotificationChannel(blocked);
    }
    /** Receiver uses the actual JVM clock. A tiny injected saved timetable makes delivery
     * deterministic without changing production time sources or waiting until sunrise. */
    private long injectDueSchedule()throws Exception{
        ZonedDateTime due=Instant.now().minusSeconds(3).truncatedTo(ChronoUnit.SECONDS).atZone(Schedule.ZONE);
        if(due.toLocalTime().equals(LocalTime.MIDNIGHT))due=due.minusSeconds(1);
        LocalTime sunrise=due.toLocalTime();
        StringBuilder csv=new StringBuilder("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n");
        for(int offset=-1;offset<=2;offset++){
            csv.append(due.toLocalDate().plusDays(offset));
            for(int step=-1;step<=4;step++)csv.append(',').append(sunrise.plusNanos(step));
            csv.append('\n');
        }
        cacheField.set(null,new Schedule(new StringReader(csv.toString())));
        return due.toInstant().toEpochMilli();
    }

    @Test @Config(sdk={26,29,35}) public void enabledByDefaultWithOwnExactSavedTimeAlarm()throws Exception{
        assertTrue(AlertScheduler.sunriseEnabled(context));
        assertFalse(AlertScheduler.enabled(context));assertFalse(AlertScheduler.remindersEnabled(context));
        AlertScheduler.reschedule(context,MORNING);
        ShadowAlarmManager.ScheduledAlarm sunrise=alarm(AlertScheduler.SUNRISE_ACTION);assertNotNull(sunrise);
        assertEquals(officialSunrise(LocalDate.of(2026,10,17)),sunrise.getTriggerAtMs());
        assertEquals(sunrise.getTriggerAtMs(),prefs.getLong("scheduled_sunrise_at",0));
        assertEquals(AlarmManager.RTC_WAKEUP,sunrise.getType());
        assertEquals(ShadowAlarmManager.WINDOW_EXACT,sunrise.getWindowLengthMs());assertTrue(sunrise.isAllowWhileIdle());
        assertEquals(1,alarms.getScheduledAlarms().size());
        assertNull(alarm(AlertScheduler.ACTION));assertNull(alarm(AlertScheduler.REMINDER_ACTION));
    }

    @Test public void prayerReminderAndSunriseHaveDistinctImmutablePendingIdentities(){
        allAlerts();AlertScheduler.reschedule(context,MORNING);
        Set<PendingIntent> distinct=new HashSet<>();
        String[] actions={AlertScheduler.ACTION,AlertScheduler.REMINDER_ACTION,AlertScheduler.SUNRISE_ACTION};
        int[] requests={701,703,705};
        for(int i=0;i<actions.length;i++){
            ShadowAlarmManager.ScheduledAlarm item=alarm(actions[i]);assertNotNull(item);
            ShadowPendingIntent pending=Shadows.shadowOf(item.operation);
            assertEquals(requests[i],pending.getRequestCode());assertTrue(pending.isImmutable());assertTrue(pending.isBroadcast());
            assertEquals(new ComponentName(context,AlarmReceiver.class),pending.getSavedIntent().getComponent());
            distinct.add(item.operation);
        }
        assertEquals(3,distinct.size());assertEquals(3,alarms.getScheduledAlarms().size());
        assertNull(alarm(AlertScheduler.IQAMAH_WIDGET_ACTION));
    }

    @Test public void repeatedReschedulesDoNotAccumulateAlarms(){
        allAlerts();for(int i=0;i<5;i++)AlertScheduler.reschedule(context,MORNING);
        assertEquals(3,alarms.getScheduledAlarms().size());assertNotNull(alarm(AlertScheduler.SUNRISE_ACTION));
        assertNotNull(alarm(AlertScheduler.ACTION));assertNotNull(alarm(AlertScheduler.REMINDER_ACTION));
    }

    @Test public void disableCancelsSunriseAndPreservesOtherScheduledDeliveries(){
        allAlerts();AlertScheduler.reschedule(context,MORNING);
        PendingIntent prior=alarm(AlertScheduler.SUNRISE_ACTION).operation;
        long prayer=prefs.getLong("scheduled_at",0),reminder=prefs.getLong("scheduled_reminder_at",0);
        prefs.edit().putBoolean(AlertScheduler.SUNRISE_ENABLED,false).commit();AlertScheduler.reschedule(context,MORNING);
        assertNull(alarm(AlertScheduler.SUNRISE_ACTION));assertTrue(Shadows.shadowOf(prior).isCanceled());
        assertFalse(prefs.contains("scheduled_sunrise_at"));
        assertEquals(prayer,alarm(AlertScheduler.ACTION).getTriggerAtMs());assertEquals(reminder,alarm(AlertScheduler.REMINDER_ACTION).getTriggerAtMs());
        assertTrue(AlertScheduler.enabled(context));assertEquals(5,AlertScheduler.reminderMinutes(context));
    }

    @Test public void runtimePermissionDenialCancelsAlarmWithoutChangingDefaultPreference(){
        AlertScheduler.reschedule(context,MORNING);assertNotNull(alarm(AlertScheduler.SUNRISE_ACTION));
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
        assertFalse(AlertScheduler.sunriseNotificationsAllowed(context));AlertScheduler.reschedule(context,MORNING);
        assertNull(alarm(AlertScheduler.SUNRISE_ACTION));assertFalse(prefs.contains("scheduled_sunrise_at"));
        assertTrue(AlertScheduler.sunriseEnabled(context));
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AlertScheduler.reschedule(context,MORNING);assertNotNull(alarm(AlertScheduler.SUNRISE_ACTION));
    }

    @Test public void globalNotificationDenialCancelsSunrise(){
        AlertScheduler.reschedule(context,MORNING);shadowNotifications.setNotificationsEnabled(false);
        AlertScheduler.reschedule(context,MORNING);assertNull(alarm(AlertScheduler.SUNRISE_ACTION));assertFalse(AlertScheduler.sunriseNotificationsAllowed(context));
    }

    @Test @Config(sdk={26,29,35}) public void blockedSunriseChannelLeavesPrayerAndReminderChannelsIndependent(){
        allAlerts();blockChannel(AlertScheduler.SUNRISE_CHANNEL);AlertScheduler.reschedule(context,MORNING);
        assertFalse(AlertScheduler.sunriseNotificationsAllowed(context));assertTrue(AlertScheduler.notificationsAllowed(context));assertTrue(AlertScheduler.reminderNotificationsAllowed(context));
        assertNull(alarm(AlertScheduler.SUNRISE_ACTION));assertNotNull(alarm(AlertScheduler.ACTION));assertNotNull(alarm(AlertScheduler.REMINDER_ACTION));
    }

    @Test public void blockedPrayerChannelDoesNotBlockSunrise(){
        allAlerts();blockChannel(AlertScheduler.CHANNEL);AlertScheduler.reschedule(context,MORNING);
        assertFalse(AlertScheduler.notificationsAllowed(context));assertTrue(AlertScheduler.sunriseNotificationsAllowed(context));
        assertNotNull(alarm(AlertScheduler.SUNRISE_ACTION));assertNull(alarm(AlertScheduler.ACTION));assertNotNull(alarm(AlertScheduler.REMINDER_ACTION));
    }

    @Test public void missingExactAlarmAccessUsesIdleCompatibleInexactFallback(){
        ShadowAlarmManager.setCanScheduleExactAlarms(false);AlertScheduler.reschedule(context,MORNING);
        ShadowAlarmManager.ScheduledAlarm sunrise=alarm(AlertScheduler.SUNRISE_ACTION);assertNotNull(sunrise);
        assertNotEquals(ShadowAlarmManager.WINDOW_EXACT,sunrise.getWindowLengthMs());assertTrue(sunrise.isAllowWhileIdle());
        assertFalse(prefs.getBoolean("schedule_error",false));
    }

    @Test @Config(shadows=RevokedExactAlarmManager.class) public void exactPermissionRevokedDuringSetFallsBackSafely(){
        RevokedExactAlarmManager.attempts=0;AlertScheduler.reschedule(context,MORNING);
        assertEquals(1,RevokedExactAlarmManager.attempts);
        ShadowAlarmManager.ScheduledAlarm sunrise=alarm(AlertScheduler.SUNRISE_ACTION);assertNotNull(sunrise);
        assertNotEquals(ShadowAlarmManager.WINDOW_EXACT,sunrise.getWindowLengthMs());assertTrue(sunrise.isAllowWhileIdle());
        assertFalse(prefs.getBoolean("schedule_error",false));
    }

    @Test @Config(shadows=SelectiveFailureAlarmManager.class) public void sunriseAndPrayerSchedulingFailuresHaveIndependentStatus(){
        allAlerts();SelectiveFailureAlarmManager.failingAction=AlertScheduler.SUNRISE_ACTION;AlertScheduler.reschedule(context,MORNING);
        assertTrue(prefs.getBoolean("sunrise_schedule_error",false));assertFalse(prefs.getBoolean("schedule_error",false));
        assertNull(alarm(AlertScheduler.SUNRISE_ACTION));assertNotNull(alarm(AlertScheduler.ACTION));assertNotNull(alarm(AlertScheduler.REMINDER_ACTION));
        SelectiveFailureAlarmManager.failingAction=AlertScheduler.ACTION;AlertScheduler.reschedule(context,MORNING);
        assertFalse(prefs.getBoolean("sunrise_schedule_error",false));assertTrue(prefs.getBoolean("schedule_error",false));
        assertNotNull(alarm(AlertScheduler.SUNRISE_ACTION));assertNull(alarm(AlertScheduler.ACTION));assertNotNull(alarm(AlertScheduler.REMINDER_ACTION));
    }

    @Test public void deliveredExactBoundaryRearmsTomorrowInsteadOfSameSunrise()throws Exception{
        long due=officialSunrise(LocalDate.of(2026,10,17));prefs.edit().putLong("last_sunrise",due).commit();
        AlertScheduler.reschedule(context,Instant.ofEpochMilli(due));
        assertEquals(officialSunrise(LocalDate.of(2026,10,18)),alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
    }

    @Test public void coverageStartsWithFirstSavedSunriseAndStopsAtLast()throws Exception{
        Schedule s=AlertScheduler.schedule(context);
        AlertScheduler.reschedule(context,s.days.firstKey().minusDays(2).atStartOfDay(Schedule.ZONE).toInstant());
        assertEquals(officialSunrise(s.days.firstKey()),alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
        long last=officialSunrise(s.days.lastKey());AlertScheduler.reschedule(context,Instant.ofEpochMilli(last));
        assertEquals(last,alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
        AlertScheduler.reschedule(context,Instant.ofEpochMilli(last+1));
        assertNull(alarm(AlertScheduler.SUNRISE_ACTION));assertFalse(prefs.contains("scheduled_sunrise_at"));
        assertFalse(prefs.getBoolean("schedule_error",false));
    }

    @Test @Config(sdk={26,29,35}) public void receiverPostsOwnNotificationAndRearmsNextSavedSunrise()throws Exception{
        long due=injectDueSchedule();receive(due);
        Notification notice=shadowNotifications.getNotification(751);assertNotNull(notice);
        assertEquals(AlertScheduler.SUNRISE_CHANNEL,notice.getChannelId());assertEquals("Sunrise",notice.extras.getString(Notification.EXTRA_TITLE));
        assertTrue(notice.extras.getString(Notification.EXTRA_TEXT).contains("Abu Dhabi"));
        assertTrue(notice.extras.getString(Notification.EXTRA_TEXT).contains("UAE time"));
        assertEquals(due,notice.when);assertEquals(15*60_000L,notice.getTimeoutAfter());
        assertEquals(due,prefs.getLong("last_sunrise",0));assertEquals(1,shadowNotifications.size());
        assertEquals(due+86_400_000L,alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
        assertFalse(prefs.contains("last_alert"));assertFalse(prefs.contains("last_reminder_prayer"));
        ShadowPendingIntent open=Shadows.shadowOf(notice.contentIntent);
        assertTrue(open.isActivity());assertTrue(open.isImmutable());assertEquals(new ComponentName(context,MainActivity.class),open.getSavedIntent().getComponent());
    }

    @Test public void duplicateReceiverReplayNeverRepostsDismissedSunrise()throws Exception{
        long due=injectDueSchedule();receive(due);assertNotNull(shadowNotifications.getNotification(751));notifications.cancel(751);
        receive(due);receive(due);
        assertNull(shadowNotifications.getNotification(751));assertEquals(due,prefs.getLong("last_sunrise",0));
        assertEquals(due+86_400_000L,alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
    }

    @Test public void deliveredHistorySurvivesClockRollbackWithoutSuppressingUndeliveredDates()throws Exception{
        long first=officialSunrise(LocalDate.of(2026,10,17));
        long middle=officialSunrise(LocalDate.of(2026,10,18));
        long third=officialSunrise(LocalDate.of(2026,10,19));
        deliver(first,Instant.ofEpochMilli(first));assertNotNull(shadowNotifications.getNotification(751));notifications.cancel(751);
        deliver(third,Instant.ofEpochMilli(third));assertNotNull(shadowNotifications.getNotification(751));notifications.cancel(751);
        // A later successful alert must not make an older alert eligible again after rollback.
        deliver(first,Instant.ofEpochMilli(first));deliver(third,Instant.ofEpochMilli(third));
        assertNull(shadowNotifications.getNotification(751));assertEquals(third,prefs.getLong("last_sunrise",0));
        AlertScheduler.reschedule(context,MORNING);
        assertEquals("An undelivered date between delivered dates stays eligible",middle,alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
        deliver(middle,Instant.ofEpochMilli(middle));assertNotNull(shadowNotifications.getNotification(751));notifications.cancel(751);
        AlertScheduler.reschedule(context,MORNING);
        assertEquals("Scheduling skips every previously delivered date",officialSunrise(LocalDate.of(2026,10,20)),alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
        deliver(first,Instant.ofEpochMilli(first));deliver(middle,Instant.ofEpochMilli(middle));deliver(third,Instant.ofEpochMilli(third));
        assertNull(shadowNotifications.getNotification(751));
    }

    @Test public void disabledOrBlockedDeliveryCannotWriteLastNotifiedRecord()throws Exception{
        long due=injectDueSchedule();prefs.edit().putBoolean(AlertScheduler.SUNRISE_ENABLED,false).commit();receive(due);
        assertNull(shadowNotifications.getNotification(751));assertFalse(prefs.contains("last_sunrise"));
        prefs.edit().putBoolean(AlertScheduler.SUNRISE_ENABLED,true).commit();blockChannel(AlertScheduler.SUNRISE_CHANNEL);receive(due);
        assertNull(shadowNotifications.getNotification(751));assertFalse(prefs.contains("last_sunrise"));
    }

    @Test public void deniedRuntimePermissionDoesNotConsumePendingSunrise()throws Exception{
        long due=injectDueSchedule();Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);receive(due);
        assertNull(shadowNotifications.getNotification(751));assertFalse(prefs.contains("last_sunrise"));
        assertFalse(prefs.getStringSet("sunrise_delivered",Collections.<String>emptySet()).contains(Long.toString(due)));
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);receive(due);
        assertNotNull(shadowNotifications.getNotification(751));assertEquals(due,prefs.getLong("last_sunrise",0));
    }

    @Test public void earlyStaleForgedAndOutsideCoverageDeliveriesAreRejected()throws Exception{
        long due=officialSunrise(LocalDate.of(2026,10,17));Instant at=Instant.ofEpochMilli(due);
        deliver(due,at.minusMillis(1));deliver(due,at.plusMillis(AlertPolicy.MAX_LATENESS_MILLIS+1));deliver(due+1,at.plusMillis(1));
        long outside=LocalDateTime.parse("2028-01-01T06:00").atZone(Schedule.ZONE).toInstant().toEpochMilli();deliver(outside,Instant.ofEpochMilli(outside));
        assertEquals(0,shadowNotifications.size());assertFalse(prefs.contains("last_sunrise"));
        deliver(due,at.plusMillis(AlertPolicy.MAX_LATENESS_MILLIS));assertNotNull(shadowNotifications.getNotification(751));assertEquals(due,prefs.getLong("last_sunrise",0));
    }

    @Test public void rescheduleSettlesPendingSunriseOnlyOnce()throws Exception{
        long due=injectDueSchedule();prefs.edit().putLong("scheduled_sunrise_at",due).commit();
        AlertScheduler.reschedule(context);assertNotNull(shadowNotifications.getNotification(751));
        notifications.cancel(751);AlertScheduler.reschedule(context);assertNull(shadowNotifications.getNotification(751));
        assertEquals(due,prefs.getLong("last_sunrise",0));assertEquals(due+86_400_000L,alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
    }

    @Test public void bootUpdateClockTimezoneAndPermissionRecoveryRestoreOneAlarm()throws Exception{
        long due=injectDueSchedule();
        for(String action:new String[]{Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED}){
            ShadowAlarmManager.ScheduledAlarm previous=alarm(AlertScheduler.SUNRISE_ACTION);
            if(previous!=null)context.getSystemService(AlarmManager.class).cancel(previous.operation);
            prefs.edit().remove("scheduled_sunrise_at").commit();
            new AlarmReceiver().onReceive(context,new Intent(action));
            assertNotNull(action,alarm(AlertScheduler.SUNRISE_ACTION));assertEquals(action,due+86_400_000L,alarm(AlertScheduler.SUNRISE_ACTION).getTriggerAtMs());
            assertEquals(action,1,alarms.getScheduledAlarms().size());
        }
    }

    @Test public void notificationAndChannelAreLocalizedAndUseShortChime()throws Exception{
        for(boolean ar:new boolean[]{false,true}){
            prefs.edit().putBoolean("arabic",ar).putBoolean("eastern_digits",ar).remove("last_sunrise").remove("sunrise_delivered").commit();
            AlertScheduler.createChannel(context);long due=injectDueSchedule();receive(due);
            Notification notice=shadowNotifications.getNotification(751);assertNotNull(notice);
            assertEquals(ar?"الشروق":"Sunrise",notice.extras.getString(Notification.EXTRA_TITLE));
            String body=notice.extras.getString(Notification.EXTRA_TEXT);
            assertTrue(body,body.contains(ar?"أبوظبي":"Abu Dhabi"));
            assertTrue(body,body.contains(AppPreferences.time(context,Instant.ofEpochMilli(due).atZone(Schedule.ZONE).toLocalTime())));
            NotificationChannel channel=notifications.getNotificationChannel(AlertScheduler.SUNRISE_CHANNEL);
            assertNotNull(channel);assertTrue(channel.getName().toString().contains(ar?"الشروق":"Sunrise"));
            assertEquals(NotificationManager.IMPORTANCE_HIGH,channel.getImportance());assertFalse(channel.shouldVibrate());
            assertEquals("android.resource://"+context.getPackageName()+"/raw/prayer_chime",channel.getSound().toString());
            assertNotEquals(AlertScheduler.CHANNEL,channel.getId());assertNotEquals(AlertScheduler.REMINDER_CHANNEL,channel.getId());
            notifications.cancel(751);
        }
    }

    @Test public void freshLaunchNeverAutomaticallyRequestsNotificationPermission()throws Exception{
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
        FixedActivity activity=launch(false);
        assertTrue(AlertScheduler.sunriseEnabled(context));assertNull(Shadows.shadowOf(activity).getLastRequestedPermission());
        assertNull(Shadows.shadowOf(activity).getNextStartedActivity());assertNull(alarm(AlertScheduler.SUNRISE_ACTION));
        invoke(activity,"sunriseSettings");assertTrue(dialog().getListView().getAdapter().getItem(0).toString().contains("allow notifications"));
        assertNull(Shadows.shadowOf(activity).getLastRequestedPermission());
    }

    @Test public void bilingualSettingsSwitchPersistsWithoutChangingFivePrayersIqamahOrWidgets()throws Exception{
        for(boolean ar:new boolean[]{false,true}){
            prefs.edit().putBoolean(AlertScheduler.SUNRISE_ENABLED,true).putBoolean(AlertScheduler.ENABLED,true).putInt(AlertScheduler.REMINDER_MINUTES,10).putInt(AlertScheduler.REMINDER_MASK,17)
                .putBoolean("iqamah_enabled",true).putInt("iqamah_offset_0",31).putString("widget_mode","light").putInt("widget_opacity",65).commit();
            FixedActivity activity=launch(ar);invoke(activity,"homeSettings");
            AlertDialog home=dialog();int sunriseRow=-1;
            for(int i=0;i<home.getListView().getAdapter().getCount();i++)if((ar?"الشروق":"Sunrise").equals(home.getListView().getAdapter().getItem(i).toString()))sunriseRow=i;
            assertTrue("Sunrise settings must be reachable from the main menu",sunriseRow>=0);select(home,sunriseRow);
            assertEquals(ar?"الشروق":"Sunrise",Shadows.shadowOf(dialog()).getTitle());screenshotDialog(dialog(),(ar?"ar":"en")+"-sunrise-settings");select(dialog(),0);
            AlertDialog choice=dialog();assertEquals(1,choice.getListView().getCheckedItemPosition());assertEquals(2,choice.getListView().getAdapter().getCount());screenshotDialog(choice,(ar?"ar":"en")+"-sunrise-switch");select(choice,0);
            assertFalse(AlertScheduler.sunriseEnabled(context));assertNull(alarm(AlertScheduler.SUNRISE_ACTION));
            assertTrue(AlertScheduler.enabled(context));assertEquals(10,AlertScheduler.reminderMinutes(context));assertEquals(17,AlertScheduler.reminderMask(context));
            assertTrue(prefs.getBoolean("iqamah_enabled",false));assertEquals(31,AppPreferences.iqamahMinutes(context)[0]);
            assertEquals("light",prefs.getString("widget_mode",""));assertEquals(65,prefs.getInt("widget_opacity",0));
            controller.recreate();activity=controller.get();assertFalse(AlertScheduler.sunriseEnabled(context));
            invoke(activity,"sunriseSettings");select(dialog(),0);assertEquals(0,dialog().getListView().getCheckedItemPosition());select(dialog(),1);assertTrue(AlertScheduler.sunriseEnabled(context));
            assertTrue(AlertScheduler.enabled(context));assertEquals(10,AlertScheduler.reminderMinutes(context));
            assertArrayEquals(new int[]{0,2,3,4,5},Schedule.PRAYERS);assertNull(activity.getWindow().getDecorView().findViewWithTag("prayer_1"));
            for(int prayer:Schedule.PRAYERS)assertNotNull(activity.getWindow().getDecorView().findViewWithTag("prayer_"+prayer));
        }
    }

    @Test public void soundSettingsTargetsOnlySunriseChannel()throws Exception{
        FixedActivity activity=launch(false);invoke(activity,"sunriseSettings");select(dialog(),1);
        Intent settings=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(settings);
        assertEquals(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS,settings.getAction());
        assertEquals(AlertScheduler.SUNRISE_CHANNEL,settings.getStringExtra(Settings.EXTRA_CHANNEL_ID));
        assertEquals(context.getPackageName(),settings.getStringExtra(Settings.EXTRA_APP_PACKAGE));
    }

    @Test public void explicitPermissionSetupSurvivesActivityRecreationAndKeepsSunriseRoute()throws Exception{
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.POST_NOTIFICATIONS);
        FixedActivity activity=launch(false);invoke(activity,"sunriseSettings");select(dialog(),1);
        ShadowActivity.PermissionsRequest request=Shadows.shadowOf(activity).getLastRequestedPermission();assertNotNull(request);
        assertArrayEquals(new String[]{Manifest.permission.POST_NOTIFICATIONS},request.requestedPermissions);
        controller.recreate();activity=controller.get();
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        blockChannel(AlertScheduler.SUNRISE_CHANNEL);
        activity.onRequestPermissionsResult(request.requestCode,new String[]{Manifest.permission.POST_NOTIFICATIONS},new int[]{PackageManager.PERMISSION_GRANTED});
        Intent settings=Shadows.shadowOf(activity).getNextStartedActivity();assertNotNull(settings);
        assertEquals(AlertScheduler.SUNRISE_CHANNEL,settings.getStringExtra(Settings.EXTRA_CHANNEL_ID));
        assertFalse(AlertScheduler.enabled(context));assertFalse(AlertScheduler.remindersEnabled(context));
    }

    @Test public void expiredTimetableStatusAndBilingualHelpExplainLimits()throws Exception{
        for(boolean ar:new boolean[]{false,true}){
            FixedActivity.time=Instant.parse("2028-01-01T12:00:00Z");FixedActivity activity=launch(ar);invoke(activity,"sunriseSettings");
            assertTrue(dialog().getListView().getAdapter().getItem(0).toString().contains(ar?"انتهى الجدول المحفوظ":"saved timetable ended"));
            select(dialog(),3);String help=Shadows.shadowOf(dialog()).getMessage().toString();
            for(String expected:ar?new String[]{"الأوقاف","الصلوات الخمس","١٥","٢٠٢٧","عدم الإزعاج"}:new String[]{"Awqaf","five prayers","15 minutes","2027","Do Not Disturb"})assertTrue(expected,help.contains(expected));
            dialog().dismiss();
        }
    }
}
