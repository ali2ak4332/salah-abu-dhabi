package ae.prayertimes.abudhabi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Common preferences for the app, notifications and launcher widgets. */
public final class AppPreferences {
    private AppPreferences() {}
    public static SharedPreferences prefs(Context c){return AlertScheduler.prefs(c);}
    public static boolean eastern(Context c){return prefs(c).getBoolean("eastern_digits",false);}
    public static boolean twelve(Context c){return prefs(c).getBoolean("twelve_hour",false);}
    public static String digits(Context c,String value){return DisplayFormat.digits(value,eastern(c));}
    public static String time(Context c,LocalTime time){return DisplayFormat.time(time,twelve(c),eastern(c),AlertScheduler.arabic(c));}
    public static String date(Context c,LocalDate date,String pattern){return digits(c,date.format(DateTimeFormatter.ofPattern(pattern,AlertScheduler.arabic(c)?new Locale("ar"):Locale.ENGLISH)));}
    public static String countdown(Context c,long seconds){return DisplayFormat.countdown(seconds,prefs(c).getBoolean("countdown_seconds",true),eastern(c));}
    public static int[] iqamahMinutes(Context c){
        int[] values={25,0,20,20,5,20};
        for(int index:Schedule.PRAYERS){int saved=prefs(c).getInt("iqamah_offset_"+index,values[index]);values[index]=saved>=0&&saved<=60?saved:values[index];}
        return values;
    }
    public static PrayerPhase phase(Context c,Schedule schedule,Instant now){
        return PrayerPhase.at(schedule,now,prefs(c).getBoolean("iqamah_enabled",true),iqamahMinutes(c));
    }
    private static boolean dark(Context c,String mode){
        return "dark".equals(mode)||("system".equals(mode)&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
    }
    public static Palette palette(Context c){return new Palette(dark(c,prefs(c).getString("appearance_mode","system")),prefs(c).getString("accent","green"));}
    public static Palette widgetPalette(Context c){
        String mode=prefs(c).getString("widget_mode","dark"),accent=prefs(c).getString("widget_accent","match");
        boolean night="match".equals(mode)?palette(c).dark:dark(c,mode);
        return new Palette(night,"match".equals(accent)?prefs(c).getString("accent","green"):accent);
    }
    public static final class Palette {
        public final boolean dark;
        public final int bg,surface,text,muted,hero,heroText,heroMuted,tint,border,accent,highlight;
        Palette(boolean dark,String family){
            this.dark=dark;
            if("navy".equals(family)){
                bg=dark?0xff111a24:0xfff5f6f7;surface=dark?0xff1c2b3b:0xfffefefc;text=dark?0xfff1f4f8:0xff21364d;muted=dark?0xffb4c0cf:0xff596a7c;
                hero=dark?0xff284461:0xff233f5b;heroMuted=0xffdfbf78;tint=dark?0xff283b50:0xffe9eef3;border=dark?0xff465b73:0xffd1dae3;accent=dark?0xffbed2e7:0xff294e70;
            }else if("sand".equals(family)){
                bg=dark?0xff201c17:0xfffaf6ee;surface=dark?0xff302a22:0xfffffdf8;text=dark?0xfff8f0e4:0xff493c2d;muted=dark?0xffc5b8a5:0xff726450;
                hero=dark?0xff604a34:0xff654c32;heroMuted=0xffe5d0aa;tint=dark?0xff40372b:0xfff0e7d8;border=dark?0xff65563f:0xffddd1bc;accent=dark?0xffe4c99e:0xff705333;
            }else{
                bg=dark?0xff101a17:0xfff6f4ee;surface=dark?0xff1b2b25:0xffffffff;text=dark?0xfff3f5f0:0xff213a34;muted=dark?0xffb0bfb4:0xff65736c;
                hero=dark?0xff244b3d:0xff153f35;heroMuted=0xffdfbf78;tint=dark?0xff283d33:0xffeaf1e9;border=dark?0xff405a4b:0xffe2e5dc;accent=dark?0xffb5d6bc:0xff153f35;
            }
            heroText=0xfffffdf7;
            // Reserve a warm accent for the active prayer and its key time.
            highlight=dark?0xffe5cb8c:0xff78551b;
        }
    }
}
