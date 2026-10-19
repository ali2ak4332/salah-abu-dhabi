package ae.prayertimes.abudhabi;
import static org.junit.Assert.*;
import android.app.Activity;
import android.appwidget.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={26,29,35})
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class NewWidgetTest {
 private static final Instant NOW=Instant.parse("2026-10-17T08:00:00Z");
 private Context c; private ActivityController<Activity> controller;private FrameLayout host;
 @Before public void setup(){c=RuntimeEnvironment.getApplication();controller=Robolectric.buildActivity(Activity.class).setup().visible();host=new FrameLayout(controller.get());controller.get().setContentView(host);}
 @After public void close(){controller.pause().stop().destroy();}
 private View apply(boolean horizontal,Schedule s,Instant now,int w,int h)throws Exception{
  Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);
  RemoteViews v=PrayerWidgetFlexible.responsive(c,horizontal,s,now,b);
  Parcel parcel=Parcel.obtain();v.writeToParcel(parcel,0);parcel.setDataPosition(0);v=RemoteViews.CREATOR.createFromParcel(parcel);parcel.recycle();
  if(Build.VERSION.SDK_INT>=31){java.lang.reflect.Method select=RemoteViews.class.getDeclaredMethod("getRemoteViewsToApply",Context.class,android.util.SizeF.class);select.setAccessible(true);v=(RemoteViews)select.invoke(v,c,new android.util.SizeF(w,h));}
  host.removeAllViews();View root=v.apply(controller.get(),host);host.addView(root,new FrameLayout.LayoutParams(w,h));host.measure(View.MeasureSpec.makeMeasureSpec(w,1073741824),View.MeasureSpec.makeMeasureSpec(h,1073741824));host.layout(0,0,w,h);root.measure(View.MeasureSpec.makeMeasureSpec(w,1073741824),View.MeasureSpec.makeMeasureSpec(h,1073741824));root.layout(0,0,w,h);return root;
 }
 private void bounds(View v,int w,int h){
  if(v.getVisibility()!=View.VISIBLE)return;
  if(v instanceof TextView){TextView t=(TextView)v;assertNotNull(t.getLayout());assertTrue("Text height: "+t.getText(),t.getLayout().getHeight()<=t.getHeight()+2);for(int line=0;line<t.getLayout().getLineCount();line++){assertTrue("Text width: "+t.getText(),t.getLayout().getLineWidth(line)<=t.getWidth()+2);assertEquals(0,t.getLayout().getEllipsisCount(line));}android.graphics.Rect rect=new android.graphics.Rect(0,0,t.getWidth(),t.getHeight());host.offsetDescendantRectToMyCoords(t,rect);assertTrue(rect.toString(),rect.left>=0&&rect.top>=0&&rect.right<=w&&rect.bottom<=h);}
  if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)bounds(((ViewGroup)v).getChildAt(i),w,h);
 }
 @Test public void responsiveTimetablesFitAndKeepOfficialTimes()throws Exception{
  Schedule s=AlertScheduler.schedule(c);
  int[] ids={R.id.widget_fajr_time,R.id.widget_dhuhr_time,R.id.widget_asr_time,R.id.widget_maghrib_time,R.id.widget_isha_time};
  for(boolean horizontal:new boolean[]{false,true})for(boolean arabic:new boolean[]{false,true})for(String size:new String[]{"small","normal","large"})for(boolean eastern:new boolean[]{false,true})for(int[] dim:horizontal?new int[][]{{180,48},{250,64},{360,80}}:new int[][]{{80,80},{120,120},{200,200}}){
   RuntimeEnvironment.setQualifiers((arabic?"ar":"en")+"-rUS-w360dp-h800dp-port-mdpi");AppPreferences.prefs(c).edit().clear().putBoolean("arabic",arabic).putBoolean("eastern_digits",eastern).putBoolean("twelve_hour",eastern).putString("widget_text",size).commit();
   View root=apply(horizontal,s,NOW,dim[0],dim[1]);bounds(root,dim[0],dim[1]);assertEquals(arabic?1:0,root.getLayoutDirection());
   for(int row=0;row<5;row++)assertEquals(AppPreferences.time(c,s.days.get(NOW.atZone(Schedule.ZONE).toLocalDate())[Schedule.PRAYERS[row]]),((TextView)root.findViewById(ids[row])).getText().toString());
   assertTrue(root.isClickable());
   if(Build.VERSION.SDK_INT==35&&size.equals("normal")&&!eastern){Bitmap bitmap=Bitmap.createBitmap(dim[0],dim[1],Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));File folder=new File(System.getProperty("qa.output"),"new-widgets");folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,(horizontal?"horizontal":"flexible")+(arabic?"-ar-":"-en-")+dim[0]+"x"+dim[1]+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}
  }
 }
 @Test public void missingExpiryAndMidnightNeverRetainOldTimes()throws Exception{
  Schedule s=AlertScheduler.schedule(c);for(boolean horizontal:new boolean[]{false,true})for(Schedule data:new Schedule[]{s,null}){
   Instant end=s.days.lastKey().plusDays(1).atStartOfDay(Schedule.ZONE).toInstant();View root=apply(horizontal,data,end,horizontal?250:120,horizontal?64:120);assertEquals("—",((TextView)root.findViewById(R.id.widget_fajr_time)).getText().toString());assertNotNull(root.getContentDescription());
   Instant midnight=NOW.atZone(Schedule.ZONE).toLocalDate().plusDays(1).atStartOfDay(Schedule.ZONE).toInstant();root=apply(horizontal,s,midnight,horizontal?250:120,horizontal?64:120);assertEquals(AppPreferences.time(c,s.days.get(midnight.atZone(Schedule.ZONE).toLocalDate())[0]),((TextView)root.findViewById(R.id.widget_fajr_time)).getText().toString());
  }
 }
 @Test @Config(sdk=35) public void newProvidersParticipateInGlobalUpdates()throws Exception{
  AppWidgetManager m=AppWidgetManager.getInstance(c);for(Class<? extends AppWidgetProvider> type:Arrays.<Class<? extends AppWidgetProvider>>asList(PrayerWidgetFlexible.class,PrayerWidgetHorizontal.class)){
   int id=Shadows.shadowOf(m).createWidget(type,R.layout.widget_flexible);assertTrue(PrayerWidget.hasWidgets(c));PrayerWidget.updateAll(c);assertFalse("Global update must populate the new provider",((TextView)Shadows.shadowOf(m).getViewFor(id).findViewById(R.id.widget_fajr_time)).getText().toString().isEmpty());AppWidgetProvider provider=Shadows.shadowOf(m).getAppWidgetProviderFor(id);Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,80);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,80);provider.onAppWidgetOptionsChanged(c,m,id,b);provider.onReceive(c,new Intent(PrayerWidget.ACTION_REFRESH));provider.onDeleted(c,new int[]{id});
  }
 }
}
