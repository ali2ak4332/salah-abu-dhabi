package ae.prayertimes.abudhabi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.SizeF;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Shared implementation only. The launcher registers exactly Compact and Strip. */
public class PrayerWidget extends AppWidgetProvider {
    public static final String ACTION_REFRESH="ae.prayertimes.abudhabi.WIDGET_REFRESH";
    static final String ACTION_MINUTE="ae.prayertimes.abudhabi.WIDGET_MINUTE";
    public static final String OPEN_WIDGET_SETTINGS="open_widget_settings";
    private static final Class<?>[] PROVIDERS={PrayerWidgetCompact.class,PrayerWidgetStrip.class};
    private static final int[] NAMES={R.id.widget_fajr_name,R.id.widget_dhuhr_name,R.id.widget_asr_name,R.id.widget_maghrib_name,R.id.widget_isha_name};
    private static final int[] TIMES={R.id.widget_fajr_time,R.id.widget_dhuhr_time,R.id.widget_asr_time,R.id.widget_maghrib_time,R.id.widget_isha_time};
    private static final int[] ROWS={R.id.widget_fajr_row,R.id.widget_dhuhr_row,R.id.widget_asr_row,R.id.widget_maghrib_row,R.id.widget_isha_row};
    private static final String[] EN={"Fajr","Dhuhr","Asr","Maghrib","Isha"},AR={"الفجر","الظهر","العصر","المغرب","العشاء"};
    protected boolean horizontal(){return false;}

    @Override public void onReceive(Context c,Intent intent){
        if(ACTION_REFRESH.equals(intent.getAction())){updateAll(c);AlertScheduler.reschedule(c);}
        else if(ACTION_MINUTE.equals(intent.getAction()))updateAll(c);
        else super.onReceive(c,intent);
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){
        Instant now=Instant.now();updateInstances(c,m,ids,horizontal(),now);updateAllExcept(c,now,ids);AlertScheduler.reschedule(c);
    }
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){
        Instant now=Instant.now();update(c,m,id,horizontal(),load(c),options,now);updateAllExcept(c,now,new int[]{id});
    }
    @Override public void onEnabled(Context c){updateAll(c);AlertScheduler.reschedule(c);}
    @Override public void onDisabled(Context c){updateAll(c);AlertScheduler.reschedule(c);}
    @Override public void onDeleted(Context c,int[] ids){updateAll(c);AlertScheduler.reschedule(c);}
    public static boolean hasWidgets(Context c){
        AppWidgetManager m=AppWidgetManager.getInstance(c);
        for(Class<?> provider:PROVIDERS)if(m.getAppWidgetIds(new ComponentName(c,provider)).length>0)return true;
        return false;
    }
    public static void updateAll(Context c){updateAll(c,Instant.now());}
    static void updateAll(Context c,Instant now){updateAllExcept(c,now,null);}
    private static void updateAllExcept(Context c,Instant now,int[] alreadyUpdated){
        AppWidgetManager m=AppWidgetManager.getInstance(c);Schedule s=load(c);
        for(int shape=0;shape<PROVIDERS.length;shape++){
            int[] ids=m.getAppWidgetIds(new ComponentName(c,PROVIDERS[shape]));
            for(int id:ids)if(!contains(alreadyUpdated,id))update(c,m,id,shape==1,s,null,now);
        }
        scheduleMinute(c,now);
    }
    private static boolean contains(int[] ids,int target){
        // Callback targets were already attempted, including IDs not yet in launcher discovery.
        if(ids!=null)for(int id:ids)if(id==target)return true;
        return false;
    }
    static void updateInstances(Context c,AppWidgetManager m,int[] ids,boolean horizontal){
        updateInstances(c,m,ids,horizontal,Instant.now());
    }
    static void updateInstances(Context c,AppWidgetManager m,int[] ids,boolean horizontal,Instant now){
        if(ids==null)return;Schedule s=load(c);
        for(int id:ids)update(c,m,id,horizontal,s,null,now);
    }
    private static Schedule load(Context c){try{return AlertScheduler.schedule(c);}catch(Exception e){return null;}}
    private static void update(Context c,AppWidgetManager m,int id,boolean horizontal,Schedule s,Bundle options,Instant now){
        // A stale or rejected launcher instance cannot break other widgets or app startup.
        try{if(options==null)options=m.getAppWidgetOptions(id);m.updateAppWidget(id,responsive(c,horizontal,s,now,options));}
        catch(RuntimeException e){Log.w("PrayerWidget","Could not refresh widget "+id,e);}
    }
    /** One-shot display refreshes never wake the device or use allow-while-idle. Live
     * timers switch to an absolute target in the final minute; snapshots update each
     * minute. Android can defer even the guard refresh, so an old live Chronometer can
     * still run past zero until the launcher applies a refresh. */
    static final long TIMER_GUARD_MILLIS=60_000L;
    static void scheduleMinute(Context c,Instant now){
        AlarmManager am=c.getSystemService(AlarmManager.class);
        for(Class<?> receiver:new Class<?>[]{PrayerWidget.class,PrayerWidgetCompact.class,PrayerWidgetStrip.class}){
            PendingIntent old=PendingIntent.getBroadcast(c,705,new Intent(c,receiver).setAction(ACTION_MINUTE),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
            if(old!=null){am.cancel(old);old.cancel();}
        }
        try{
            AppWidgetManager m=AppWidgetManager.getInstance(c);
            boolean compact=m.getAppWidgetIds(new ComponentName(c,PrayerWidgetCompact.class)).length>0;
            boolean strip=m.getAppWidgetIds(new ComponentName(c,PrayerWidgetStrip.class)).length>0;
            if(!compact&&!strip)return;
            Schedule schedule=load(c);
            PrayerPhase phase=iqamahPhase(c,schedule,now);
            long nextMinute=(now.toEpochMilli()/60_000L+1)*60_000L,next=Long.MAX_VALUE;
            // Both shapes light the called prayer during its iqamah window; clear it on time.
            if(phase!=null)next=phase.targetAt.toInstant().toEpochMilli();
            Countdown target=compact?countdownTarget(c,schedule,now):null;
            if(target!=null){
                long at=target.target.toInstant().toEpochMilli(),remaining=at-now.toEpochMilli();
                next=Math.min(next,at); // Include adhan even if the alert is deferred/disabled.
                if(remaining>TIMER_GUARD_MILLIS){
                    next=Math.min(next,at-TIMER_GUARD_MILLIS);
                    if(!liveTimer(c))next=Math.min(next,nextMinute);
                }
            }
            if(next==Long.MAX_VALUE)return;
            PendingIntent tick=PendingIntent.getBroadcast(c,705,new Intent(c,compact?PrayerWidgetCompact.class:PrayerWidgetStrip.class).setAction(ACTION_MINUTE),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            try{if(AlertScheduler.precise(c))am.setExact(AlarmManager.RTC,next,tick);else am.set(AlarmManager.RTC,next,tick);}
            catch(SecurityException revoked){am.set(AlarmManager.RTC,next,tick);}
        }catch(RuntimeException error){Log.w("PrayerWidget","Could not schedule display refresh",error);}
    }
    /** Android's Chronometer formats with the device locale's digits. Use it live only when
     * those digits match the saved Western/Eastern preference; otherwise use snapshots. */
    static boolean liveTimer(Context c){
        String sample=DateUtils.formatElapsedTime(65);boolean sawDigit=false,eastern=AppPreferences.eastern(c);
        for(int i=0;i<sample.length();i++){
            char ch=sample.charAt(i);
            if(Character.isDigit(ch)){
                sawDigit=true;
                if(eastern?(ch<'\u0660'||ch>'\u0669'):(ch<'0'||ch>'9'))return false;
            }
        }
        return sawDigit;
    }
    /** What Compact counts down to: the estimated iqamah while that window is open,
     * otherwise the next saved adhan (today or tomorrow). Null outside saved data. */
    static final class Countdown {
        final int index;final boolean iqamah;final ZonedDateTime target;
        Countdown(int index,boolean iqamah,ZonedDateTime target){this.index=index;this.iqamah=iqamah;this.target=target;}
    }
    static Countdown countdownTarget(Context c,Schedule schedule,Instant now){
        if(schedule==null)return null;
        PrayerPhase phase=iqamahPhase(c,schedule,now);
        if(phase!=null)return new Countdown(phase.index,true,phase.targetAt);
        Schedule.Next next=schedule.next(now);
        if(next!=null&&next.at.toInstant().equals(now))next=schedule.next(now.plusNanos(1));
        return next==null?null:new Countdown(next.index,false,next.at);
    }
    static PrayerPhase iqamahPhase(Context c,Schedule schedule,Instant now){
        if(schedule==null)return null;
        PrayerPhase phase=AppPreferences.phase(c,schedule,now);
        return phase!=null&&phase.iqamah&&!now.isBefore(phase.adhanAt.toInstant())&&now.isBefore(phase.targetAt.toInstant())?phase:null;
    }
    static String iqamahMinutes(Context c,PrayerPhase phase,Instant now){
        if(phase==null||now.isBefore(phase.adhanAt.toInstant())||!now.isBefore(phase.targetAt.toInstant()))return "";
        long remaining=phase.targetAt.toInstant().toEpochMilli()-now.toEpochMilli();
        return AppPreferences.digits(c,Long.toString((remaining+59_999L)/60_000L));
    }
    static RemoteViews responsive(Context c,boolean horizontal,Schedule s,Instant now,Bundle options){
        if(options==null)options=new Bundle();
        if(Build.VERSION.SDK_INT>=31){
            try{
                ArrayList<SizeF> requested=options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES);
                Map<SizeF,RemoteViews> map=new LinkedHashMap<>();
                if(requested!=null&&requested.size()<=16)for(SizeF size:requested){
                    if(size!=null&&valid(size.getWidth())&&valid(size.getHeight()))map.put(size,render(c,horizontal,s,now,(int)size.getWidth(),(int)size.getHeight()));
                }
                if(!map.isEmpty())return new RemoteViews(map);
            }catch(RuntimeException malformed){Log.w("PrayerWidget","Ignoring invalid launcher size list",malformed);}
        }
        int dw=horizontal?300:140,dh=horizontal?64:160;
        int minW=dimension(options,AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,dw);
        int maxW=dimension(options,AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,minW);
        int minH=dimension(options,AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,dh);
        int maxH=dimension(options,AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,minH);
        // Legacy hosts pair maxWidth/minHeight (landscape), minWidth/maxHeight (portrait).
        return new RemoteViews(render(c,horizontal,s,now,maxW,minH),render(c,horizontal,s,now,minW,maxH));
    }
    private static int dimension(Bundle options,String key,int fallback){int value=options.getInt(key,fallback);return valid(value)?value:fallback;}
    private static boolean valid(float value){return !Float.isNaN(value)&&!Float.isInfinite(value)&&value>=1&&value<=4096;}

    private static final String[] EN_ALL={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"},AR_ALL={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};
    /** Header label around the timer; "%s" is replaced by the time remaining. */
    static String countdownFormat(boolean ar,Countdown target){
        if(target.iqamah)return ar?"الإقامة بعد %s":"Iqamah in %s";
        return (ar?AR_ALL:EN_ALL)[target.index]+(ar?" بعد %s":" in %s");
    }
    static String targetTimeFormat(boolean ar,Countdown target){
        return (target.iqamah?(ar?"الإقامة": "Iqamah"):(ar?AR_ALL:EN_ALL)[target.index])+(ar?" عند %s":" at %s");
    }
    static boolean finalMinute(Countdown target,Instant now){
        return target!=null&&target.target.toInstant().toEpochMilli()-now.toEpochMilli()<=TIMER_GUARD_MILLIS;
    }
    /** H:MM, minutes rounded up; used only when the native timer's digits would differ. */
    static String snapshot(Context c,long millis){
        long minutes=Math.max(1,(Math.max(0,millis)+59_999L)/60_000L);
        return AppPreferences.digits(c,String.format(Locale.US,"%d:%02d",minutes/60,minutes%60));
    }
    static RemoteViews render(Context c,boolean horizontal,Schedule schedule,Instant now,int requestedWidth,int requestedHeight){
        int width=Math.max(1,Math.min(4096,requestedWidth)),height=Math.max(1,Math.min(4096,requestedHeight));
        boolean ar=AlertScheduler.arabic(c);String[] names=ar?AR:EN,clocks=new String[5];
        LocalDate today=now.atZone(Schedule.ZONE).toLocalDate();
        LocalTime[] times=schedule==null?null:schedule.days.get(today);
        Schedule.Next next=schedule==null?null:schedule.next(now);
        for(int row=0;row<5;row++)clocks[row]=times==null?"—":AppPreferences.time(c,times[Schedule.PRAYERS[row]]);
        String date=AppPreferences.digits(c,now.atZone(Schedule.ZONE).format(DateTimeFormatter.ofPattern("d MMM",ar?new Locale("ar"):Locale.ENGLISH)));
        int pad=horizontal?(height<56?2:4):6;
        boolean header=horizontal?height>=96:height>=140;
        float without=fit(c,width,height,horizontal,pad,false,names,clocks,date);
        float font=header?fit(c,width,height,horizontal,pad,true,names,clocks,date):without;
        // Date is useful only if it does not significantly shrink the five prayer times.
        if(header&&((horizontal&&font<without*.9f)||(!horizontal&&font<9))){header=false;font=without;}
        // The prayer that matters now: the called prayer during its iqamah window, else the next adhan today.
        PrayerPhase phase=iqamahPhase(c,schedule,now);
        int lit=-1;boolean litIqamah=false;
        if(times!=null){
            if(phase!=null&&phase.adhanAt.toLocalDate().equals(today)){lit=phase.index;litIqamah=true;}
            else if(next!=null&&next.at.toLocalDate().equals(today))lit=next.index;
        }
        Countdown target=horizontal||times==null?null:countdownTarget(c,schedule,now);
        boolean absolute=finalMinute(target,now);
        boolean live=target!=null&&!absolute&&liveTimer(c);
        String countdownText=target==null?"":String.format(Locale.US,absolute?targetTimeFormat(ar,target):countdownFormat(ar,target),
            absolute?AppPreferences.time(c,target.target.toLocalTime()):live?"0:00:00":snapshot(c,target.target.toInstant().toEpochMilli()-now.toEpochMilli()));
        float headerFont=font;
        boolean dateVisible=header;
        if(!horizontal){
            // The five prayer rows keep their fitted size; the header uses only remaining room.
            headerFont=compactHeaderFont(c,width,height,pad,font,names,clocks,date,ar,true);
            dateVisible=headerFont>=9;
            if(!dateVisible)headerFont=compactHeaderFont(c,width,height,pad,font,names,clocks,date,ar,false);
            header=headerFont>=9&&(dateVisible||target!=null);
        }
        RemoteViews v=new RemoteViews(c.getPackageName(),horizontal?R.layout.widget_strip:(ar?R.layout.widget_compact_ar:R.layout.widget_compact));
        AppPreferences.Palette palette=AppPreferences.widgetPalette(c);
        int opacity=Math.max(0,Math.min(100,AppPreferences.prefs(c).getInt("widget_opacity",100)));
        boolean darkInk=opacity==0||palette.dark;
        int text=opacity==0?Color.WHITE:palette.text,accent=opacity==0?0xffe5cb8c:palette.highlight;
        int iqamahColour=darkInk?0xfff0b23f:0xff8a5a06;
        int muted=opacity==0?0xccffffff:palette.muted;
        // Prayers already behind the lit one fade back; they stay legible at ~60% contrast.
        int past=opacity==0?0x99ffffff:((palette.muted&0x00ffffff)|0xa6000000);
        // Shape drawables keep their corner radii at every size, with zero bitmap parcel cost.
        v.setImageViewResource(R.id.widget_background,backgroundResource(palette));
        v.setInt(R.id.widget_background,"setImageAlpha",Math.round(255f*opacity/100));
        v.setInt(R.id.widget_root,"setLayoutDirection",ar?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        int pixels=Math.round(pad*c.getResources().getDisplayMetrics().density);
        int vertical=Math.round((horizontal?pad:4)*c.getResources().getDisplayMetrics().density);
        v.setViewPadding(R.id.widget_content,pixels,vertical,pixels,vertical);
        v.setViewVisibility(R.id.widget_header,header?View.VISIBLE:View.GONE);
        v.setViewVisibility(R.id.widget_meta,dateVisible?View.VISIBLE:View.GONE);
        v.setTextViewText(R.id.widget_meta,date);v.setTextViewTextSize(R.id.widget_meta,TypedValue.COMPLEX_UNIT_DIP,headerFont);
        if(!horizontal){
            int timerColour=target!=null&&target.iqamah?iqamahColour:accent;
            String spoken=target==null?null:(target.iqamah
                ?(ar?"الإقامة التقديرية عند ":"Estimated iqamah at ")
                :(ar?"أذان ":"")+(ar?AR_ALL:EN_ALL)[target.index]+(ar?" عند ":" adhan at "))
                +AppPreferences.time(c,target.target.toLocalTime())+" · "
                +AppPreferences.date(c,target.target.toLocalDate(),"d MMM")+(ar?"، بتوقيت أبوظبي":", Abu Dhabi time");
            boolean showTimer=header&&target!=null;
            v.setViewVisibility(R.id.widget_countdown,showTimer&&live?View.VISIBLE:View.GONE);
            v.setViewVisibility(R.id.widget_iqamah_minutes,showTimer&&!live?View.VISIBLE:(showTimer?View.GONE:View.INVISIBLE));
            if(showTimer&&live){
                long base=SystemClock.elapsedRealtime()+(target.target.toInstant().toEpochMilli()-now.toEpochMilli());
                v.setChronometer(R.id.widget_countdown,base,countdownFormat(ar,target),true);
                v.setChronometerCountDown(R.id.widget_countdown,true);
            }else v.setChronometer(R.id.widget_countdown,SystemClock.elapsedRealtime(),null,false);
            v.setTextViewText(R.id.widget_iqamah_minutes,showTimer&&!live?countdownText:"");
            for(int id:new int[]{R.id.widget_countdown,R.id.widget_iqamah_minutes}){
                v.setTextViewTextSize(id,TypedValue.COMPLEX_UNIT_DIP,headerFont);
                v.setTextColor(id,timerColour);
                v.setContentDescription(id,spoken);
            }
        }
        v.setTextColor(R.id.widget_meta,muted);
        v.setContentDescription(R.id.widget_meta,AppPreferences.date(c,today,"EEEE, d MMMM yyyy")+(ar?"، بتوقيت أبوظبي":", Abu Dhabi time"));
        int pill=litIqamah?(darkInk?R.drawable.widget_pill_iqamah_dark:R.drawable.widget_pill_iqamah_light)
            :(darkInk?R.drawable.widget_pill_next_dark:R.drawable.widget_pill_next_light);
        for(int row=0;row<5;row++){
            int index=Schedule.PRAYERS[row];
            if(horizontal){
                int side=Math.round(2*c.getResources().getDisplayMetrics().density);
                int rowPad=Math.round((height<56?0:3)*c.getResources().getDisplayMetrics().density);
                v.setViewPadding(ROWS[row],side,rowPad,side,rowPad);
            }
            boolean active=index==lit;
            boolean behind=lit>=0&&index<lit;
            int colour=active?(litIqamah?iqamahColour:accent):behind?past:text;
            v.setTextViewText(NAMES[row],names[row]);v.setTextViewText(TIMES[row],clocks[row]);
            v.setTextViewTextSize(TIMES[row],TypedValue.COMPLEX_UNIT_DIP,font);
            v.setTextViewTextSize(NAMES[row],TypedValue.COMPLEX_UNIT_DIP,horizontal?stripNameSize(font):font);
            v.setTextColor(TIMES[row],colour);
            v.setTextColor(NAMES[row],active||behind?colour:horizontal?muted:text);
            // Every refresh sets or clears the pill explicitly (0 removes the background).
            v.setInt(ROWS[row],"setBackgroundResource",active?pill:0);
            String state=active?(litIqamah?(ar?"، بانتظار الإقامة":", awaiting iqamah"):(ar?"، الأذان القادم":", next adhan")):"";
            v.setContentDescription(ROWS[row],names[row]+" "+clocks[row]+state+(ar?"، بتوقيت أبوظبي":", Abu Dhabi time"));
        }
        v.setContentDescription(R.id.widget_root,times==null?(ar?"لا مواقيت محفوظة لهذا اليوم. اضغط لفتح التطبيق.":"No saved times for today. Tap to open the app."):null);
        Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,470,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
    /** Horizontal names sit above their times in a lighter, smaller line. */
    static final float STRIP_NAME_SCALE=.78f;
    static float stripNameSize(float timeSize){return Math.max(9f,timeSize*STRIP_NAME_SCALE);}
    private static String[] headerSamples(Context c){
        boolean ar=AlertScheduler.arabic(c);
        String[] samples=new String[(Schedule.PRAYERS.length+1)*3];
        for(int i=0;i<Schedule.PRAYERS.length;i++)samples[i]=AppPreferences.digits(c,
            String.format(Locale.US,countdownFormat(ar,new Countdown(Schedule.PRAYERS[i],false,null)),"00:00:00"));
        int count=Schedule.PRAYERS.length+1;
        samples[count-1]=AppPreferences.digits(c,
            String.format(Locale.US,countdownFormat(ar,new Countdown(0,true,null)),"00:00:00"));
        for(int meridiem=0;meridiem<2;meridiem++){
            String clock=AppPreferences.time(c,LocalTime.of(meridiem==0?11:23,59));
            for(int i=0;i<count;i++)samples[count*(meridiem+1)+i]=String.format(Locale.US,
                targetTimeFormat(ar,new Countdown(i<count-1?Schedule.PRAYERS[i]:0,i==count-1,null)),clock);
        }
        return samples;
    }
    private static float minimumHeaderHeight(Context c,TextPaint paint){
        // A single wide line includes every possible fallback glyph.
        return lineHeight(String.join(" ",headerSamples(c)),paint);
    }
    private static float compactHeaderFont(Context c,int width,int height,int pad,float prayerFont,String[] names,String[] clocks,String date,boolean ar,boolean withDate){
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setFontFeatureSettings("tnum");
        float density=c.getResources().getDisplayMetrics().density;
        paint.setTextSize(prayerFont*density);float line=0;
        for(int i=0;i<5;i++)line=Math.max(line,Math.max(lineHeight(names[i],paint),lineHeight(clocks[i],paint))/density);
        // Keep one pixel of slack in each row, matching the fit calculation. At ordinary
        // Compact sizes the prayer fit reserves a readable header instead of hiding it.
        float available=height-2*4-2-5*(line+1);
        String[] samples=headerSamples(c);String glyphs=String.join(" ",samples)+(withDate?" "+date:"");
        for(float size=Math.min(13,prayerFont);size>=9;size-=.25f){
            paint.setTextSize(size*density);float h=lineHeight(glyphs,paint)/density,w=0;
            // Measure every localized label; character count cannot predict glyph width.
            for(String timer:samples)w=Math.max(w,paint.measureText(timer)/density);
            w+=(withDate?paint.measureText(date)/density:0)+8+12;
            if(h<=available&&w<=width-2*pad-2)return size;
        }
        return 0;
    }
    private static int backgroundResource(AppPreferences.Palette p){
        if(p.surface==0xff1c2b3b)return R.drawable.widget_dark_navy;
        if(p.surface==0xfffefefc)return R.drawable.widget_light_navy;
        if(p.surface==0xff302a22)return R.drawable.widget_dark_sand;
        if(p.surface==0xfffffdf8)return R.drawable.widget_light_sand;
        return p.dark?R.drawable.widget_dark_green:R.drawable.widget_light_green;
    }
    /** Fit one common size against every actual localized glyph, including fallback line
     * metrics. The two shapes stay fixed; only text and optional date respond to space. */
    private static float fit(Context c,int width,int height,boolean horizontal,int pad,boolean header,String[] names,String[] clocks,String date){
        String pref=AppPreferences.prefs(c).getString("widget_text","normal");
        float desired=("small".equals(pref)?14:"large".equals(pref)?22:18)+Math.max(0,Math.min(width,height)-160)/30f;
        desired=Math.min(30,desired*Math.max(.85f,Math.min(2,c.getResources().getConfiguration().fontScale)));
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setFontFeatureSettings("tnum");
        float density=c.getResources().getDisplayMetrics().density;
        float low=.5f,high=desired,best=.5f,compactHeader=0;
        if(header&&!horizontal){paint.setTextSize(9*density);compactHeader=minimumHeaderHeight(c,paint)/density+2;}
        for(int attempt=0;attempt<10;attempt++){
            float size=(low+high)/2;
            // Names are measured in the heavier time face too, so lighter names always fit.
            float nameSize=horizontal?stripNameSize(size):size;
            paint.setTextSize(nameSize*density);float nameLine=0,nameWidth=0;
            for(int i=0;i<5;i++){nameLine=Math.max(nameLine,lineHeight(names[i],paint)/density);nameWidth=Math.max(nameWidth,paint.measureText(names[i])/density);}
            paint.setTextSize(size*density);float timeLine=0,timeWidth=0;
            for(int i=0;i<5;i++){timeLine=Math.max(timeLine,lineHeight(clocks[i],paint)/density);timeWidth=Math.max(timeWidth,paint.measureText(clocks[i])/density);}
            float headerHeight=0;
            if(header){
                if(horizontal)headerHeight=lineHeight(date,paint)/density+3;
                else headerHeight=compactHeader;
            }
            float inner=width-2*pad;
            boolean fits;
            if(horizontal){
                // Column: 2dp side padding each way, 3dp top/bottom pill padding.
                float column=inner/5-4;
                fits=nameLine+timeLine+(height<56?0:6)+1<=height-2*pad-headerHeight&&nameWidth<=column-1&&timeWidth<=column-1;
            }else{
                // Row: 6dp side padding each way inside the pill.
                float cellH=(height-2*4-headerHeight)/5;
                float nameW=(inner-12)/2.3f,timeW=(inner-12)*1.3f/2.3f;
                fits=Math.max(nameLine,timeLine)+1<=cellH&&nameWidth<=nameW-1&&timeWidth<=timeW-1;
            }
            if(header)fits&=paint.measureText(date)/density<=inner-1;
            if(fits){best=size;low=size;}else high=size;
        }
        // Quarter-dp steps avoid perceptible jitter during resizing.
        return Math.max(.5f,(float)Math.floor(best*4)/4);
    }
    private static int lineHeight(String value,TextPaint paint){
        StaticLayout.Builder builder=StaticLayout.Builder.obtain(value,0,value.length(),paint,Math.max(8192,(int)Math.ceil(paint.measureText(value))+8)).setIncludePad(false);
        if(Build.VERSION.SDK_INT>=28)builder.setUseLineSpacingFromFallbacks(true);
        return builder.build().getHeight();
    }
}
