package ae.prayertimes.abudhabi;

import java.time.LocalTime;
import java.util.Locale;

/** Shared, locale-independent time arithmetic with an explicit digit preference. */
public final class DisplayFormat {
    private DisplayFormat() {}
    public static String digits(String value, boolean eastern) {
        StringBuilder out=new StringBuilder(value.length());
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);int n=-1;
            if(c>='0'&&c<='9')n=c-'0';
            else if(c>='\u0660'&&c<='\u0669')n=c-'\u0660';
            else if(c>='\u06f0'&&c<='\u06f9')n=c-'\u06f0';
            out.append(n<0?c:(char)((eastern?'\u0660':'0')+n));
        }
        return out.toString();
    }
    public static String time(LocalTime time, boolean twelve, boolean eastern, boolean arabic) {
        int hour=time.getHour();
        String value=twelve?String.format(Locale.US,"%d:%02d %s",hour%12==0?12:hour%12,time.getMinute(),hour<12?(arabic?"ص":"AM"):(arabic?"م":"PM"))
            :String.format(Locale.US,"%02d:%02d",hour,time.getMinute());
        return digits(value,eastern);
    }
    public static String countdown(long seconds,boolean showSeconds,boolean eastern) {
        seconds=Math.max(0,seconds);
        long minutes=(seconds+59)/60;
        return digits(showSeconds?String.format(Locale.US,"%02d:%02d:%02d",seconds/3600,(seconds%3600)/60,seconds%60)
            :String.format(Locale.US,"%02d:%02d",minutes/60,minutes%60),eastern);
    }
}
