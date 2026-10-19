import ae.prayertimes.abudhabi.DisplayFormat;
import java.time.LocalTime;
import java.time.Instant;
import java.util.Locale;
import java.util.TimeZone;

/** Output boundaries that affect the app, launcher widgets and notifications. */
public final class DisplayFormatTest {
    private static void equal(String expected, String actual, String reason) {
        if (!expected.equals(actual)) throw new AssertionError(reason + ": expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        equal("12:00 AM", DisplayFormat.time(LocalTime.MIDNIGHT, true, false, false), "midnight");
        equal("12:00 PM", DisplayFormat.time(LocalTime.NOON, true, false, false), "noon");
        equal("1:05 PM", DisplayFormat.time(LocalTime.of(13, 5), true, false, false), "afternoon and padded minutes");
        equal("١:٠٥ م", DisplayFormat.time(LocalTime.of(13, 5), true, true, true), "Arabic afternoon");
        equal("12:01 ص", DisplayFormat.time(LocalTime.of(0, 1), true, false, true), "Arabic language with Western digits");
        equal("٢٣:٥٩", DisplayFormat.time(LocalTime.of(23, 59), false, true, false), "Eastern digits independently of language");
        equal("123 09", DisplayFormat.digits("١۲3 ۰٩", false), "normalize both Arabic digit blocks");
        equal("v١.٤ · ٠٥:٠٢", DisplayFormat.digits("v1.4 · 05:02", true), "preserve punctuation");
        equal("00:00:00", DisplayFormat.countdown(-1, true, false), "negative countdown clamp");
        equal("01:01:01", DisplayFormat.countdown(3661, true, false), "hour/minute/second separation");
        equal("00:00", DisplayFormat.countdown(0, false, false), "zero minutes");
        equal("00:01", DisplayFormat.countdown(1, false, false), "round partial minute upward");
        equal("01:00", DisplayFormat.countdown(3599, false, false), "round across hour boundary");
        equal("٠١:٠١", DisplayFormat.countdown(3601, false, true), "rounded Eastern countdown");
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("ar", "AE"));
            equal("05:02", DisplayFormat.time(LocalTime.of(5, 2), false, false, false), "explicit Western choice overrides device locale");
        } finally { Locale.setDefault(previous); }
        equal("Thu · 15 Oct", DisplayFormat.widgetDate(Instant.parse("2026-10-15T19:59:59Z"), false), "before UAE midnight");
        equal("Fri · 16 Oct", DisplayFormat.widgetDate(Instant.parse("2026-10-15T20:00:00Z"), false), "UAE midnight changes weekday and date together");
        equal("Fri · 1 Jan", DisplayFormat.widgetDate(Instant.parse("2026-12-31T20:00:00Z"), false), "UAE year boundary");
        equal("Tue · 29 Feb", DisplayFormat.widgetDate(Instant.parse("2028-02-28T20:00:00Z"), false), "leap day");
        Instant friday = Instant.parse("2026-10-16T08:00:00Z");
        equal("الجمعة · 16 أكتوبر", DisplayFormat.digits(DisplayFormat.widgetDate(friday, true), false), "Arabic weekday and month with Western digits");
        equal("الجمعة · ١٦ أكتوبر", DisplayFormat.digits(DisplayFormat.widgetDate(friday, true), true), "Arabic date with Eastern digits");
        equal("Fri · ١٦ Oct", DisplayFormat.digits(DisplayFormat.widgetDate(friday, false), true), "English date respects Eastern numeral preference");
        TimeZone previousZone = TimeZone.getDefault();
        previous = Locale.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            Locale.setDefault(Locale.JAPAN);
            equal("Fri · 16 Oct", DisplayFormat.widgetDate(Instant.parse("2026-10-15T20:00:00Z"), false), "travel and device locale do not change the UAE header");
        } finally { TimeZone.setDefault(previousZone); Locale.setDefault(previous); }
        System.out.println("PASS: 15 time/digit/countdown checks and 8 widget date/locale/time-zone checks.");
    }
}
