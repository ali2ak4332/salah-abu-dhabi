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
        public final int bg,surface,text,muted,hero,heroText,heroMuted,tint,border,accent;
        Palette(boolean dark,String family){
            this.dark=dark;
            if("navy".equals(family)){
                bg=dark?0xff0e1622:0xfff1f5fa;surface=dark?0xff1b283b:0xffffffff;text=dark?0xffeef3fc:0xff1c304c;muted=dark?0xffafbed1:0xff526579;
                hero=dark?0xff1f3859:0xff203e63;heroMuted=0xffc5d7f2;tint=dark?0xff293c54:0xffe1eaf5;border=dark?0xff7594bd:0xff8aa1bd;accent=dark?0xffb4c9ef:0xff203e63;
            }else if("sand".equals(family)){
                bg=dark?0xff1b1713:0xfffaf6ef;surface=dark?0xff2b251f:0xfffffdf8;text=dark?0xfff5ede1:0xff463c32;muted=dark?0xffc4b5a3:0xff726451;
                hero=dark?0xff5c4330:0xff654932;heroMuted=0xfff0d8b4;tint=dark?0xff403428:0xffeee2cf;border=dark?0xffaf9173:0xffaa9071;accent=dark?0xffe3c196:0xff654932;
            }else{
                bg=dark?0xff101714:0xfff5f7f3;surface=dark?0xff1c2923:0xffffffff;text=dark?0xffedf3ee:0xff193d34;muted=dark?0xffafbbb4:0xff5c6f67;
                hero=dark?0xff244638:0xff193d34;heroMuted=0xffddc991;tint=dark?0xff283d32:0xffe6ede1;border=dark?0xff83a184:0xff94ab91;accent=dark?0xffafd2a8:0xff193d34;
            }
            heroText=0xffffffff;
        }
    }
}
