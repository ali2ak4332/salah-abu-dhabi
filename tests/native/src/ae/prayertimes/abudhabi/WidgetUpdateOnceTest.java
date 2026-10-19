package ae.prayertimes.abudhabi;

import static org.junit.Assert.*;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

/** Callback targets and remaining instances must share one refresh pass, not duplicate work. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,29,35},shadows=TwoWidgetTest.ControlledManager.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class WidgetUpdateOnceTest {
    private Context context;
    @Before public void reset(){
        context=RuntimeEnvironment.getApplication();AppPreferences.prefs(context).edit().clear().commit();
        TwoWidgetTest.ControlledManager.installed=true;TwoWidgetTest.ControlledManager.stripOnly=false;
        TwoWidgetTest.ControlledManager.rejected=-1;TwoWidgetTest.ControlledManager.rejectOptions=false;
        TwoWidgetTest.ControlledManager.updated.clear();TwoWidgetTest.ControlledManager.queried.clear();
    }
    private void assertOnce(Integer... expected){
        List<Integer> actual=TwoWidgetTest.ControlledManager.updated;
        assertEquals("Refresh must contain exactly the expected instances",new HashSet<>(Arrays.asList(expected)),new HashSet<>(actual));
        for(int id:expected)assertEquals("Each instance must refresh only once: "+id+" in "+actual,1,Collections.frequency(actual,id));
    }
    @Test public void callbackTargetsAndOtherInstancesRefreshOnce(){
        for(boolean strip:new boolean[]{false,true}){
            PrayerWidget provider=strip?new PrayerWidgetStrip():new PrayerWidgetCompact();
            int[] ids=strip?new int[]{201,202}:new int[]{101,102};
            TwoWidgetTest.ControlledManager.updated.clear();provider.onUpdate(context,AppWidgetManager.getInstance(context),ids);assertOnce(101,102,201,202);
            // The callback can name a newly bound ID not yet returned by launcher discovery.
            TwoWidgetTest.ControlledManager.updated.clear();provider.onUpdate(context,AppWidgetManager.getInstance(context),new int[]{999});assertOnce(999,101,102,201,202);
            TwoWidgetTest.ControlledManager.updated.clear();provider.onUpdate(context,AppWidgetManager.getInstance(context),null);assertOnce(101,102,201,202);
            TwoWidgetTest.ControlledManager.updated.clear();provider.onUpdate(context,AppWidgetManager.getInstance(context),new int[0]);assertOnce(101,102,201,202);
        }
    }
    @Test public void failedInstanceDoesNotRepeatHealthyInstancesAndLaterRecovers(){
        for(boolean optionsFailure:new boolean[]{false,true}){
            TwoWidgetTest.ControlledManager.rejected=101;TwoWidgetTest.ControlledManager.rejectOptions=optionsFailure;
            TwoWidgetTest.ControlledManager.updated.clear();new PrayerWidgetCompact().onUpdate(context,AppWidgetManager.getInstance(context),new int[]{101,102});assertOnce(102,201,202);
            TwoWidgetTest.ControlledManager.rejected=-1;TwoWidgetTest.ControlledManager.updated.clear();PrayerWidget.updateAll(context);assertOnce(101,102,201,202);
        }
    }
}
