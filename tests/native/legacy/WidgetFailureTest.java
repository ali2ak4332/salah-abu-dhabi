package ae.prayertimes.abudhabi;

import static org.junit.Assert.*;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowAppWidgetManager;

/** A launcher/service rejecting one widget must not interrupt the other instances or app. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, shadows=WidgetFailureTest.RejectingManager.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class WidgetFailureTest {
    @Implements(AppWidgetManager.class)
    public static class RejectingManager extends ShadowAppWidgetManager {
        static int rejected=101;
        static boolean rejectOptions;
        static int providerQueries;
        static final List<Integer> updated=new ArrayList<>();
        @Implementation public int[] getAppWidgetIds(ComponentName provider){
            providerQueries++;
            return provider.getClassName().equals(PrayerWidget.class.getName())
                ?new int[]{101,102}:provider.getClassName().equals(PrayerWidgetTall.class.getName())?new int[]{201}:new int[0];
        }
        @Implementation public Bundle getAppWidgetOptions(int id){
            if(id==rejected&&rejectOptions)throw new IllegalArgumentException("Rejected widget options");
            return new Bundle();
        }
        @Implementation public void updateAppWidget(int id,RemoteViews views){
            if(id==rejected)throw new IllegalStateException("Launcher rejected widget update");
            updated.add(id);
        }
        @Implementation public void updateAppWidget(int[] ids,RemoteViews views){
            for(int id:ids)updateAppWidget(id,views);
        }
    }
    public static class FixedActivity extends MainActivity {
        @Override protected Instant now(){return Instant.parse("2026-10-17T08:00:00Z");}
    }
    private Context context;
    @Before public void reset(){
        RuntimeEnvironment.setQualifiers("en-rUS-w360dp-h800dp-port-mdpi");
        context=RuntimeEnvironment.getApplication();
        AppPreferences.prefs(context).edit().clear().commit();
        RejectingManager.rejected=101;RejectingManager.rejectOptions=false;
        RejectingManager.providerQueries=0;RejectingManager.updated.clear();
    }
    @Test public void updateAllContinuesAfterOneInstanceIsRejected(){
        PrayerWidget.updateAll(context);
        assertEquals(Arrays.asList(102,201),RejectingManager.updated);
        // Another refresh can recover the rejected instance without re-adding any widget.
        RejectingManager.rejected=-1;RejectingManager.updated.clear();
        PrayerWidget.updateAll(context);
        assertEquals(Arrays.asList(101,102,201),RejectingManager.updated);
    }
    @Test public void optionsFailureDoesNotBlockOtherWidgets(){
        RejectingManager.rejectOptions=true;
        PrayerWidget.updateAll(context);
        assertEquals(Arrays.asList(102,201),RejectingManager.updated);
    }
    @Test public void providerCallbacksContinueToSchedulingAfterRejection(){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        AppPreferences.prefs(context).edit().putBoolean("schedule_error",true).commit();
        new PrayerWidget().onUpdate(context,manager,new int[]{101,102});
        assertEquals(Arrays.asList(102),RejectingManager.updated);
        assertFalse("Scheduling must still run after the rejected instance",
            AppPreferences.prefs(context).contains("schedule_error"));
        RejectingManager.providerQueries=0;
        AppPreferences.prefs(context).edit().putBoolean("countdown_seconds",false).commit();
        new PrayerWidget().onAppWidgetOptionsChanged(context,manager,101,new Bundle());
        assertTrue("Minute refresh policy must still run",RejectingManager.providerQueries>0);
    }
    @Test public void appCanOpenAndResumeInBothLanguagesWithARejectedWidget(){
        for(boolean arabic:new boolean[]{false,true}){
            AppPreferences.prefs(context).edit().putBoolean("arabic",arabic).commit();
            ActivityController<FixedActivity> activity=Robolectric.buildActivity(FixedActivity.class);
            try{
                activity.create().start().resume().visible();
                assertFivePrayers(activity.get(),arabic);
                activity.pause().resume();
                assertFivePrayers(activity.get(),arabic);
            }finally{activity.pause().stop().destroy();}
        }
    }
    private void assertFivePrayers(MainActivity activity,boolean arabic){
        String[] names=arabic?new String[]{"الفجر","الظهر","العصر","المغرب","العشاء"}
            :new String[]{"Fajr","Dhuhr","Asr","Maghrib","Isha"};
        View root=activity.findViewById(android.R.id.content);
        for(int row=0;row<Schedule.PRAYERS.length;row++){
            TextView name=root.findViewWithTag("prayer_name_"+Schedule.PRAYERS[row]);
            assertNotNull(name);assertEquals(names[row],name.getText().toString());
        }
    }
}
