package ae.prayertimes.abudhabi;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.SizeF;
import android.view.Gravity;
import android.view.View;
import android.widget.RemoteViews;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Offline home-screen views. All displayed dates and times use Abu Dhabi's zone. */
public class PrayerWidget extends AppWidgetProvider {
    private static final String[] EN = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private static final String[] AR = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    private static final int[] NAMES = {R.id.widget_fajr_name, R.id.widget_dhuhr_name,
        R.id.widget_asr_name, R.id.widget_maghrib_name, R.id.widget_isha_name};
    private static final int[] TIMES = {R.id.widget_fajr_time, R.id.widget_dhuhr_time,
        R.id.widget_asr_time, R.id.widget_maghrib_time, R.id.widget_isha_time};

    protected boolean tall() { return false; }

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        Schedule schedule = load(context);
        for (int id : ids) update(context, manager, id, tall(), schedule);
        AlertScheduler.reschedule(context);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
            int id, Bundle options) {
        update(context, manager, id, tall(), load(context));
    }

    @Override public void onEnabled(Context context) { AlertScheduler.reschedule(context); }
    @Override public void onDisabled(Context context) { AlertScheduler.reschedule(context); }

    public static boolean hasWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        return manager.getAppWidgetIds(new ComponentName(context, PrayerWidget.class)).length > 0
            || manager.getAppWidgetIds(new ComponentName(context, PrayerWidgetTall.class)).length > 0;
    }

    /** Safe for the alarm receiver and language switch: does not schedule another alarm. */
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        Schedule schedule = load(context);
        for (int id : manager.getAppWidgetIds(new ComponentName(context, PrayerWidget.class)))
            update(context, manager, id, false, schedule);
        for (int id : manager.getAppWidgetIds(new ComponentName(context, PrayerWidgetTall.class)))
            update(context, manager, id, true, schedule);
    }

    private static Schedule load(Context context) {
        try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("times.csv"), "UTF-8")) {
            return new Schedule(reader);
        } catch (Exception error) {
            return null;
        }
    }

    private static void update(Context context, AppWidgetManager manager, int id,
            boolean useTall, Schedule schedule) {
        boolean arabic = context.getSharedPreferences("MainActivity", 0).getBoolean("arabic",
            Locale.getDefault().getLanguage().equals("ar"));
        Instant now = Instant.now();
        RemoteViews compact = views(context, false, arabic, schedule, now);
        RemoteViews result = compact;
        if (useTall) {
            RemoteViews expanded = views(context, true, arabic, schedule, now);
            if (Build.VERSION.SDK_INT >= 31) {
                Map<SizeF, RemoteViews> sizes = new LinkedHashMap<>();
                sizes.put(new SizeF(110, 90), compact);
                sizes.put(new SizeF(110, 180), expanded);
                result = new RemoteViews(sizes);
            } else {
                // The old launcher reports portrait and landscape ranges, not exact sizes.
                Bundle options = manager.getAppWidgetOptions(id);
                int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180);
                int maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight);
                result = new RemoteViews(minHeight >= 180 ? expanded : compact,
                    maxHeight >= 180 ? expanded : compact);
            }
        }
        manager.updateAppWidget(id, result);
    }

    private static String t(boolean arabic, String en, String ar) { return arabic ? ar : en; }

    private static int layout(Context context, boolean tall, boolean arabic) {
        String size = AlertScheduler.prefs(context).getString("widget_text", "normal");
        boolean transparent = AlertScheduler.prefs(context).getInt("widget_opacity", 100) == 0;
        if (tall && arabic) {
            if (transparent) {
                if ("small".equals(size)) return R.layout.widget_prayer_tall_ar_small_transparent;
                if ("large".equals(size)) return R.layout.widget_prayer_tall_ar_large_transparent;
                return R.layout.widget_prayer_tall_ar_transparent;
            }
            if ("small".equals(size)) return R.layout.widget_prayer_tall_ar_small;
            if ("large".equals(size)) return R.layout.widget_prayer_tall_ar_large;
            return R.layout.widget_prayer_tall_ar;
        }
        if (transparent) {
            if ("small".equals(size)) return tall ? R.layout.widget_prayer_tall_small_transparent : R.layout.widget_prayer_compact_small_transparent;
            if ("large".equals(size)) return tall ? R.layout.widget_prayer_tall_large_transparent : R.layout.widget_prayer_compact_large_transparent;
            return tall ? R.layout.widget_prayer_tall_transparent : R.layout.widget_prayer_compact_transparent;
        }
        if ("small".equals(size)) return tall ? R.layout.widget_prayer_tall_small : R.layout.widget_prayer_compact_small;
        if ("large".equals(size)) return tall ? R.layout.widget_prayer_tall_large : R.layout.widget_prayer_compact_large;
        return tall ? R.layout.widget_prayer_tall : R.layout.widget_prayer_compact;
    }

    /** Only the bitmap is translucent: labels remain solid and readable. */
    private static Bitmap background(boolean tall, AppPreferences.Palette palette, int opacity) {
        int width = 200, height = tall ? 300 : 200;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        if (opacity == 0) return bitmap;
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int alpha = Math.round(255f * opacity / 100f);
        int fill = palette.dark ? palette.surface : palette.tint;
        paint.setColor((fill & 0x00ffffff) | (alpha << 24));
        RectF rect = new RectF(1f, 1f, width - 1f, height - 1f);
        canvas.drawRoundRect(rect, 24f, 24f, paint);
        // A faint, matching border keeps the card distinct from busy wallpapers.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor((palette.border & 0x00ffffff) | ((alpha / 2) << 24));
        canvas.drawRoundRect(rect, 24f, 24f, paint);
        return bitmap;
    }

    private static RemoteViews views(Context context, boolean tall, boolean arabic,
            Schedule schedule, Instant now) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layout(context, tall, arabic));
        AppPreferences.Palette palette = AppPreferences.widgetPalette(context);
        int opacity = Math.max(0, Math.min(100, AlertScheduler.prefs(context).getInt("widget_opacity", 100)));
        int text = opacity == 0 ? Color.WHITE : palette.text;
        int muted = opacity < 100 ? text : palette.muted;
        int accent = opacity < 100 ? text : palette.accent;
        views.setImageViewBitmap(R.id.widget_background, background(tall, palette, opacity));
        views.setTextColor(R.id.widget_label, accent);
        views.setTextColor(R.id.widget_next_name, text);
        views.setTextColor(R.id.widget_meta, muted);
        views.setTextColor(R.id.widget_source, muted);
        views.setTextColor(R.id.widget_iqamah_countdown, accent);
        if (!tall) views.setTextColor(R.id.widget_next_time, text);
        views.setInt(R.id.widget_root, "setLayoutDirection", arabic ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        Intent open = new Intent(context, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent click = PendingIntent.getActivity(context, 400, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, click);
        views.setTextViewText(R.id.widget_label, t(arabic, "NEXT · ABU DHABI", "القادمة · أبوظبي"));
        views.setTextViewText(R.id.widget_source, t(arabic, "AWQAF · ABU DHABI", "الأوقاف · أبوظبي"));
        views.setInt(R.id.widget_iqamah_countdown, "setGravity",
            (arabic ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        if (!tall) views.setInt(R.id.widget_next_time, "setGravity",
            (arabic ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);

        LocalDate today = now.atZone(Schedule.ZONE).toLocalDate();
        PrayerPhase phase = schedule == null ? null : AppPreferences.phase(context, schedule, now);
        boolean iqamah = phase != null && phase.iqamah;
        // Stop a previously running chronometer when RemoteViews reapplies the same layout.
        views.setChronometer(R.id.widget_iqamah_countdown, SystemClock.elapsedRealtime(), null, false);
        views.setViewVisibility(R.id.widget_iqamah_countdown, iqamah ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.widget_meta, iqamah && !tall ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.widget_source, iqamah ? View.GONE : View.VISIBLE);
        LocalTime[] times = schedule == null ? null : schedule.days.get(today);
        Locale locale = arabic ? new Locale("ar") : Locale.ENGLISH;
        String date = AppPreferences.digits(context, today.format(DateTimeFormatter.ofPattern("d MMM", locale)));

        if (phase != null) {
            String name = arabic ? AR[phase.index] : EN[phase.index];
            String time = AppPreferences.time(context, phase.targetAt.toLocalTime());
            views.setTextViewText(R.id.widget_next_name,
                tall ? name + "  ·  \u2066" + time + "\u2069" : name);
            if (!tall) views.setTextViewText(R.id.widget_next_time, time);
            String day = phase.targetAt.toLocalDate().isAfter(today)
                ? t(arabic, "Tomorrow", "غداً") : t(arabic, "Today", "اليوم");
            views.setTextViewText(R.id.widget_meta, tall
                ? t(arabic, "TODAY · ", "اليوم · ") + date
                : day + t(arabic, " · UAE time", " · توقيت الإمارات"));
            views.setContentDescription(R.id.widget_next_name,
                name + " " + time + " " + day + t(arabic, ", Abu Dhabi time", "، بتوقيت أبوظبي"));
            if (tall && phase.targetAt.toLocalDate().isAfter(today))
                views.setTextViewText(R.id.widget_label, t(arabic, "NEXT · TOMORROW", "القادمة · غداً"));
            if (iqamah) {
                views.setTextViewText(R.id.widget_label,
                    t(arabic, tall ? "ESTIMATED IQAMAH" : "Est. iqamah", "الإقامة التقديرية"));
                if (tall) views.setTextViewText(R.id.widget_meta,
                    t(arabic, "TODAY'S ADHAN", "أذان اليوم"));
                views.setContentDescription(R.id.widget_next_name,
                    name + t(arabic, ", estimated iqamah ", "، الإقامة التقديرية ") + time
                    + t(arabic, ", Abu Dhabi time. Tap to refresh.", "، بتوقيت أبوظبي. اضغط للتحديث."));
                long until = Math.max(0L, Duration.between(now, phase.targetAt.toInstant()).toMillis());
                views.setChronometerCountDown(R.id.widget_iqamah_countdown, true);
                views.setChronometer(R.id.widget_iqamah_countdown,
                    SystemClock.elapsedRealtime() + until, null, true);
            }
        } else {
            String status;
            if (schedule == null) status = t(arabic, "Open app", "افتح التطبيق");
            else if (today.isAfter(schedule.days.lastKey())) status = t(arabic, "Timetable ended", "انتهى الجدول");
            else if (today.isBefore(schedule.days.firstKey())) status = t(arabic, "Not yet available", "غير متاح بعد");
            else status = t(arabic, "No further times", "لا مواقيت لاحقة");
            views.setTextViewText(R.id.widget_next_name, status);
            if (!tall) views.setTextViewText(R.id.widget_next_time, "—");
            views.setTextViewText(R.id.widget_meta, t(arabic, "Tap to open timetable", "اضغط لفتح الجدول"));
            views.setTextViewText(R.id.widget_label, t(arabic, "ABU DHABI", "أبوظبي"));
        }

        if (tall) {
            for (int row = 0; row < Schedule.PRAYERS.length; row++) {
                int index = Schedule.PRAYERS[row];
                views.setTextViewText(NAMES[row], arabic ? AR[index] : EN[index]);
                views.setTextViewText(TIMES[row], times == null ? "—" : AppPreferences.time(context, times[index]));
                // In RTL the time column is on the left: align its text to the outer edge,
                // not toward the centre. The prayer name stays at the opposite, right edge.
                views.setInt(TIMES[row], "setGravity",
                    (arabic ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL);
                boolean upcoming = phase != null && phase.adhanAt.toLocalDate().equals(today) && phase.index == index;
                views.setTextColor(NAMES[row], upcoming ? accent : text);
                views.setTextColor(TIMES[row], upcoming ? accent : text);
            }
        }
        return views;
    }
}
