package ae.prayertimes.abudhabi;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.SystemClock;
import android.text.Layout;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowAlertDialog;

/**
 * Native RemoteViews acceptance tests. Rendered geometry is checked after Android inflation,
 * RemoteViews actions, framework size selection, measurement and native Skia drawing. These tests
 * do not emulate an OEM launcher, Doze, battery use or physical notification delivery.
 */
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class WidgetUiTest {
  private static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
  private static final String[] EN = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
  private static final String[] AR = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
  private static final Instant SAMPLE = Instant.parse("2026-10-17T08:00:00Z");
  private Context context;
  private Schedule schedule;
  private ActivityController<Activity> controller;
  private FrameLayout host;
  private View root;
  private int width, height;

  @Before
  public void setUp() throws Exception {
    configure(false, "normal", 100, "dark", "green");
    schedule = AlertScheduler.schedule(context);
    controller = Robolectric.buildActivity(Activity.class).create().start().resume().visible();
    host = new FrameLayout(controller.get());
    controller.get().setContentView(host);
  }

  @After
  public void tearDown() {
    if (controller != null) controller.pause().stop().destroy();
  }

  private void configure(boolean arabic, String text, int opacity, String mode, String accent) {
    RuntimeEnvironment.setQualifiers((arabic ? "ar" : "en") + "-rUS-w360dp-h800dp-port-mdpi");
    RuntimeEnvironment.setFontScale(1f);
    context = RuntimeEnvironment.getApplication();
    AppPreferences.prefs(context)
        .edit()
        .clear()
        .putBoolean("arabic", arabic)
        .putString("widget_text", text)
        .putInt("widget_opacity", opacity)
        .putString("widget_mode", mode)
        .putString("widget_accent", accent)
        .putBoolean("countdown_seconds", true)
        .commit();
  }

  private int id(String name) {
    int id = context.getResources().getIdentifier(name, "id", context.getPackageName());
    assertTrue("Missing resource ID " + name, id != 0);
    return id;
  }

  private View view(String name) {
    return root.findViewById(id(name));
  }

  private TextView text(String name) {
    View v = view(name);
    assertTrue("Missing TextView " + name, v instanceof TextView);
    return (TextView) v;
  }

  private String value(String name) {
    return text(name).getText().toString();
  }

  private RemoteViews direct(boolean tall, boolean arabic, Schedule data, Instant now)
      throws Exception {
    Method method =
        PrayerWidget.class.getDeclaredMethod(
            "views", Context.class, boolean.class, boolean.class, Schedule.class, Instant.class);
    method.setAccessible(true);
    return (RemoteViews) method.invoke(null, context, tall, arabic, data, now);
  }

  private RemoteViews responsive(
      boolean tall, boolean arabic, Schedule data, Instant now, int w, int h) throws Exception {
    Bundle options = new Bundle();
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, w);
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, w);
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, h);
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, h);
    if (Build.VERSION.SDK_INT >= 31) {
      ArrayList<SizeF> sizes = new ArrayList<>();
      sizes.add(new SizeF(w, h));
      options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes);
    }
    Method method =
        PrayerWidget.class.getDeclaredMethod(
            "responsiveViews",
            Context.class,
            boolean.class,
            boolean.class,
            Schedule.class,
            Instant.class,
            Bundle.class);
    method.setAccessible(true);
    RemoteViews result =
        (RemoteViews) method.invoke(null, context, tall, arabic, data, now, options);
    // Use Android's actual sized-RemoteViews selector, rather than choosing a test-side layout.
    if (Build.VERSION.SDK_INT >= 31) {
      Method select =
          RemoteViews.class.getDeclaredMethod("getRemoteViewsToApply", Context.class, SizeF.class);
      select.setAccessible(true);
      result = (RemoteViews) select.invoke(result, context, new SizeF(w, h));
    }
    return result;
  }

  private void apply(RemoteViews views, int w, int h) {
    width = w;
    height = h;
    host.removeAllViews();
    root = views.apply(context, host);
    host.addView(root, new FrameLayout.LayoutParams(w, h));
    layout();
  }

  private void reapply(RemoteViews views) {
    views.reapply(context, root);
    layout();
  }

  private void layout() {
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    for (int i = 0; i < 3; i++) {
      host.measure(
          View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
      host.layout(0, 0, width, height);
    }
  }

  private List<View> all(View v) {
    List<View> result = new ArrayList<>();
    result.add(v);
    if (v instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) v;
      for (int i = 0; i < group.getChildCount(); i++) result.addAll(all(group.getChildAt(i)));
    }
    return result;
  }

  private boolean visible(View v) {
    if (v == null || v.getVisibility() != View.VISIBLE) return false;
    for (android.view.ViewParent p = v.getParent(); p instanceof View; p = p.getParent())
      if (((View) p).getVisibility() != View.VISIBLE) return false;
    return true;
  }

  private String name(View v) {
    try {
      return context.getResources().getResourceEntryName(v.getId());
    } catch (Exception ignored) {
      return v.getClass().getSimpleName();
    }
  }

  private void assertRenderedBounds(String label) throws Exception {
    List<String> errors = new ArrayList<>();
    int nonemptyText = 0;
    for (View v : all(root)) {
      if (!visible(v)) continue;
      if (v instanceof TextView && ((TextView) v).getText().length() > 0) {
        nonemptyText++;
        TextView t = (TextView) v;
        Rect box = new Rect(0, 0, t.getWidth(), t.getHeight());
        host.offsetDescendantRectToMyCoords(t, box);
        if (box.left < 0 || box.top < 0 || box.right > width || box.bottom > height)
          errors.add(name(t) + " view outside host: " + box);
        Rect visible = new Rect();
        if (!t.getGlobalVisibleRect(visible)
            || visible.width() != t.getWidth()
            || visible.height() != t.getHeight()) errors.add(name(t) + " view clipped by ancestor");
        Layout l = t.getLayout();
        if (l == null) {
          errors.add(name(t) + " missing text layout");
          continue;
        }
        int aw = t.getWidth() - t.getCompoundPaddingLeft() - t.getCompoundPaddingRight();
        int ah = t.getHeight() - t.getCompoundPaddingTop() - t.getCompoundPaddingBottom();
        if (aw <= 0 || ah <= 0) errors.add(name(t) + " has no text area");
        if (l.getHeight() > ah + 2)
          errors.add(name(t) + " text height " + l.getHeight() + ">" + ah);
        for (int line = 0; line < l.getLineCount(); line++) {
          if (l.getEllipsisCount(line) != 0) errors.add(name(t) + " text ellipsized");
          if (l.getLineWidth(line) > aw + 2)
            errors.add(name(t) + " text width " + l.getLineWidth(line) + ">" + aw);
        }
        assertEquals(label + ": text must not inherit background opacity", 1f, t.getAlpha(), .001f);
      }
    }
    assertTrue(label + ": insufficient rendered content", nonemptyText >= 5);
    screenshot(label);
    assertTrue(label + ": native rendered-content bounds " + errors, errors.isEmpty());
  }

  private void screenshot(String label) throws Exception {
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(bitmap));
    // A successful inflation is insufficient: native Skia must actually paint nontrivial content.
    int painted = 0;
    for (int y = 0; y < height; y++)
      for (int x = 0; x < width; x++) if (Color.alpha(bitmap.getPixel(x, y)) > 0) painted++;
    assertTrue(label + ": empty native render", painted > width * height / 50);
    File folder = new File(System.getProperty("qa.output", "screenshots"), "widgets");
    folder.mkdirs();
    try (FileOutputStream stream = new FileOutputStream(new File(folder, label + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
    }
    bitmap.recycle();
  }

  private boolean expanded() {
    return root.findViewById(id("widget_fajr_name")) != null;
  }

  private void assertRows(boolean arabic, Instant now) {
    if (!expanded()) return;
    LocalTime[] official = schedule.days.get(now.atZone(Schedule.ZONE).toLocalDate());
    for (int i = 0; i < KEYS.length; i++) {
      assertEquals(
          "Official prayer name " + KEYS[i],
          (arabic ? AR : EN)[i],
          value("widget_" + KEYS[i] + "_name"));
      assertEquals(
          "Official adhan time " + KEYS[i],
          official == null ? "—" : AppPreferences.time(context, official[Schedule.PRAYERS[i]]),
          value("widget_" + KEYS[i] + "_time"));
      View row = view("widget_" + KEYS[i] + "_row");
      assertTrue("Prayer row hidden " + KEYS[i], visible(row));
      PrayerPhase phase = AppPreferences.phase(context, schedule, now);
      boolean active =
          phase != null
              && phase.adhanAt.toLocalDate().equals(now.atZone(Schedule.ZONE).toLocalDate())
              && phase.index == Schedule.PRAYERS[i];
      assertTrue(
          "Row background must be explicit so reused highlights clear",
          row.getBackground() instanceof ColorDrawable);
      int background = ((ColorDrawable) row.getBackground()).getColor();
      assertEquals("Active row background " + KEYS[i], active, Color.alpha(background) > 0);
      int opacity = AppPreferences.prefs(context).getInt("widget_opacity", 100);
      int accent = opacity == 0 ? 0xffe5cb8c : AppPreferences.widgetPalette(context).highlight;
      int plain = opacity == 0 ? Color.WHITE : AppPreferences.widgetPalette(context).text;
      assertEquals(
          "Active prayer time color " + KEYS[i],
          active ? accent : plain,
          text("widget_" + KEYS[i] + "_time").getCurrentTextColor());
      assertNotNull("Prayer row accessibility label", row.getContentDescription());
      assertEquals(
          "Active prayer accessibility " + KEYS[i],
          active,
          row.getContentDescription()
              .toString()
              .contains(arabic ? "الصلاة الحالية" : "active prayer"));
    }
  }

  private void assertPhase(boolean arabic, Instant now) throws Exception {
    PrayerPhase expected = AppPreferences.phase(context, schedule, now);
    if (expected == null) {
      assertFalse("Expired countdown still visible", visible(view("widget_iqamah_countdown")));
      assertFalse(
          "Expired minute snapshot still visible", visible(view("widget_countdown_minutes")));
      return;
    }
    int ordinal = 0;
    while (Schedule.PRAYERS[ordinal] != expected.index) ordinal++;
    String prayer = (arabic ? AR : EN)[ordinal];
    assertTrue("Wrong next prayer", value("widget_next_name").contains(prayer));
    String time = AppPreferences.time(context, expected.targetAt.toLocalTime());
    View separateTime = root.findViewById(id("widget_next_time"));
    assertTrue(
        "Wrong phase target time",
        value("widget_next_name").contains(time)
            || separateTime instanceof TextView
                && ((TextView) separateTime).getText().toString().contains(time));
    assertEquals(
        "Wrong iqamah label",
        expected.iqamah,
        value("widget_label").toLowerCase(Locale.ROOT).contains(arabic ? "الإقامة" : "iqamah"));
    CharSequence description = view("widget_next_name").getContentDescription();
    assertNotNull("Next-prayer accessibility description", description);
    assertTrue(description.toString(), description.toString().contains(prayer));
    assertTrue(description.toString(), description.toString().contains(time));
    if (PrayerWidget.liveSeconds(context)) {
      Chronometer clock = (Chronometer) view("widget_iqamah_countdown");
      assertTrue("Live phase countdown hidden", visible(clock));
      assertTrue("Chronometer must count down", clock.isCountDown());
      long expectedMillis =
          Math.max(0, Duration.between(now, expected.targetAt.toInstant()).toMillis());
      assertEquals(
          "Countdown target drift",
          expectedMillis,
          clock.getBase() - SystemClock.elapsedRealtime(),
          1000);
      assertTrue("Native chronometer was not started", started(clock));
      assertTrue(
          "Requested update hint must be hidden",
          !visible(view("widget_countdown_hint")));
      assertTrue(
          "Native delayed-boundary refresh instruction",
          value("widget_countdown_hint").contains(arabic ? "الصفر" : "zero"));
      assertFalse(
          "Minute snapshot visible in seconds mode", visible(view("widget_countdown_minutes")));
    }
  }

  private boolean started(Chronometer clock) throws Exception {
    Field field = Chronometer.class.getDeclaredField("mStarted");
    field.setAccessible(true);
    return field.getBoolean(clock);
  }

  private int minimum(String provider, String dimension) throws Exception {
    int resource = context.getResources().getIdentifier(provider, "xml", context.getPackageName());
    try (XmlResourceParser parser = context.getResources().getXml(resource)) {
      while (parser.next() != XmlResourceParser.START_TAG) {}
      String value =
          parser.getAttributeValue("http://schemas.android.com/apk/res/android", dimension);
      return Math.round(Float.parseFloat(value.replaceAll("[^0-9.]", "")));
    }
  }

  @Test
  public void responsiveNativeContentAndTextSizes() throws Exception {
    List<String> failures = new ArrayList<>();
    for (boolean arabic : new boolean[] {false, true})
      for (boolean tall : new boolean[] {false, true})
        for (String size : new String[] {"small", "normal", "large"}) {
          configure(arabic, size, 100, "dark", "green");
          int minW = minimum(tall ? "widget_tall_info" : "widget_compact_info", "minResizeWidth");
          int minH = minimum(tall ? "widget_tall_info" : "widget_compact_info", "minResizeHeight");
          for (int[] dimensions :
              new int[][] {
                {110, 110},
                {minW, minH},
                {160, 160},
                {minW, 219},
                {minW, 220},
                {160, 240},
                {200, 300},
                {300, 300}
              }) {
            int w = dimensions[0], h = dimensions[1];
            String label =
                (arabic ? "ar" : "en")
                    + "-"
                    + (tall ? "tall" : "compact")
                    + "-"
                    + size
                    + "-"
                    + w
                    + "x"
                    + h;
            try {
              apply(responsive(tall, arabic, schedule, SAMPLE, w, h), w, h);
              assertEquals(
                  "Widget language direction",
                  arabic ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR,
                  root.getLayoutDirection());
              assertPhase(arabic, SAMPLE);
              assertRows(arabic, SAMPLE);
              if (w >= minW && h >= minH) {
                assertRenderedBounds(label);
                assertEquals(
                    "Every supported widget size must show all five prayers",
                    true,
                    expanded());
                System.out.println("PASS widget native bounds " + label);
              } else {
                screenshot(label + "-unsupported-shrink");
                System.out.println(
                    "SMOKE only: below declared launcher resize minimum "
                        + label
                        + " minimum="
                        + minW
                        + "x"
                        + minH);
              }
            } catch (AssertionError | RuntimeException e) {
              failures.add(label + ": " + e.getMessage());
              System.out.println("FAIL widget profile " + label + ": " + e.getMessage());
            }
          }
        }
    assertTrue(failures.toString(), failures.isEmpty());
  }

  @Test
  public void opacityPaletteAndTransparentText() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (boolean tall : new boolean[] {false, true})
        for (String mode : new String[] {"light", "dark"})
          for (String accent : new String[] {"green", "navy", "sand"})
            for (int opacity : new int[] {0, 65, 85, 100}) {
              configure(arabic, "normal", opacity, mode, accent);
              String label =
                  (arabic ? "ar" : "en")
                      + "-"
                      + (tall ? "tall" : "compact")
                      + "-"
                      + mode
                      + "-"
                      + accent
                      + "-opacity"
                      + opacity;
              apply(direct(tall, arabic, schedule, SAMPLE), tall ? 200 : 160, tall ? 300 : 160);
              ImageView background = (ImageView) view("widget_background");
              assertTrue(
                  "Background must be an independently translucent bitmap",
                  background.getDrawable() instanceof BitmapDrawable);
              Bitmap bitmap = ((BitmapDrawable) background.getDrawable()).getBitmap();
              int center = bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
              assertEquals(
                  label + ": background opacity",
                  Math.round(255f * opacity / 100),
                  Color.alpha(center),
                  1);
              if (opacity > 0) {
                int expected = AppPreferences.widgetPalette(context).surface;
                assertEquals(
                    label + ": background theme red", Color.red(expected), Color.red(center), 1);
                assertEquals(
                    label + ": background theme green",
                    Color.green(expected),
                    Color.green(center),
                    1);
                assertEquals(
                    label + ": background theme blue", Color.blue(expected), Color.blue(center), 1);
              }
              assertEquals(label + ": root opacity", 1f, root.getAlpha(), .001f);
              assertEquals(
                  label + ": text opacity",
                  255,
                  Color.alpha(text("widget_next_name").getCurrentTextColor()));
              assertRenderedBounds(label);
            }
    System.out.println(
        "PASS widget opacity 0/65/85/100, Green/Navy/Sand, Light/Dark, both sizes/languages");
  }

  @Test
  public void phaseBoundariesReapplyFridayAndMidnight() throws Exception {
    LocalDate day = LocalDate.of(2026, 10, 17);
    for (boolean arabic : new boolean[] {false, true})
      for (boolean tall : new boolean[] {false, true}) {
        configure(arabic, "normal", 100, "dark", "green");
        apply(direct(tall, arabic, schedule, SAMPLE), tall ? 200 : 160, tall ? 300 : 160);
        for (int prayer : Schedule.PRAYERS) {
          Instant adhan =
              day.atTime(schedule.days.get(day)[prayer]).atZone(Schedule.ZONE).toInstant();
          int offset = AppPreferences.iqamahMinutes(context)[prayer];
          for (long delta : new long[] {-1, 0, offset * 60L - 1, offset * 60L}) {
            Instant now = adhan.plusSeconds(delta);
            reapply(direct(tall, arabic, schedule, now));
            assertPhase(arabic, now);
            assertRows(arabic, now);
            assertRenderedBounds(
                (arabic ? "ar" : "en")
                    + "-"
                    + (tall ? "tall" : "compact")
                    + "-phase"
                    + prayer
                    + "-"
                    + delta);
          }
        }
        LocalDate friday = LocalDate.of(2026, 10, 16);
        Instant fridayDhuhr =
            friday.atTime(schedule.days.get(friday)[2]).atZone(Schedule.ZONE).toInstant();
        reapply(direct(tall, arabic, schedule, fridayDhuhr));
        assertPhase(arabic, fridayDhuhr);
        assertFalse(
            "Friday Dhuhr must never manufacture an estimated iqamah",
            AppPreferences.phase(context, schedule, fridayDhuhr).iqamah);
        assertTrue(
            "Friday adhan must advance to Asr",
            value("widget_next_name").contains(arabic ? "العصر" : "Asr"));
        for (Instant midnight :
            new Instant[] {
              Instant.parse("2026-10-17T19:59:59Z"), Instant.parse("2026-10-17T20:00:00Z")
            }) {
          reapply(direct(tall, arabic, schedule, midnight));
          assertPhase(arabic, midnight);
          assertRows(arabic, midnight);
          assertTrue(
              "UAE calendar date",
              value("widget_meta")
                  .contains(
                      AppPreferences.digits(context, DisplayFormat.widgetDate(midnight, arabic))));
        }
      }
    System.out.println(
        "PASS widget 80 adhan/iqamah boundary reapplications, Friday exclusion, UAE midnight and"
            + " official row times");
  }

  @Test
  public void hideSecondsSnapshotStopsOldClockAndUpdates() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (boolean tall : new boolean[] {false, true}) {
        configure(arabic, "normal", 100, "dark", "green");
        apply(direct(tall, arabic, schedule, SAMPLE), tall ? 200 : 160, tall ? 300 : 160);
        Chronometer clock = (Chronometer) view("widget_iqamah_countdown");
        assertTrue(started(clock));
        AppPreferences.prefs(context)
            .edit()
            .putBoolean("countdown_seconds", false)
            .putBoolean("eastern_digits", arabic)
            .commit();
        reapply(direct(tall, arabic, schedule, SAMPLE));
        assertFalse("Old native countdown must be stopped", started(clock));
        assertFalse("Seconds still visible", visible(clock));
        TextView minutes = text("widget_countdown_minutes");
        assertTrue("Minute snapshot missing", visible(minutes));
        String before = minutes.getText().toString();
        assertTrue(
            "Minutes snapshot updated-at timestamp missing",
            value("widget_countdown_hint")
                .contains(
                    AppPreferences.time(context, SAMPLE.atZone(Schedule.ZONE).toLocalTime())));
        assertTrue(
            "Snapshot updated-at label missing",
            value("widget_countdown_hint").contains(arabic ? "حُدّث" : "Updated"));
        assertTrue(
            "Minutes accessibility disclosure missing",
            minutes
                .getContentDescription()
                .toString()
                .contains(arabic ? "تقريباً" : "Approximate"));
        assertTrue(
            "Spoken snapshot must include its actual amount",
            minutes.getContentDescription().toString().contains(before.replace("≈", "").trim()));
        if (arabic)
          assertTrue(
              "Minute snapshot must honor Eastern Arabic digits", before.matches(".*[٠-٩].*"));
        assertEquals(
            "Minutes-only countdown must have exactly one colon",
            1,
            before.chars().filter(c -> c == ':').count());
        String allText = "";
        for (View v : all(root))
          if (visible(v) && v instanceof TextView) allText += ((TextView) v).getText() + " ";
        assertTrue(
            "Snapshot requires approximate or refresh disclosure: " + allText,
            allText.contains("≈")
                || allText.toLowerCase(Locale.ROOT).contains("approx")
                || allText.contains("تقريب"));
        reapply(direct(tall, arabic, schedule, SAMPLE.plusSeconds(61)));
        assertNotEquals(
            "Minute snapshot did not advance on refresh",
            before,
            value("widget_countdown_minutes"));
        assertRenderedBounds(
            (arabic ? "ar" : "en") + "-" + (tall ? "tall" : "compact") + "-hide-seconds");
        AppPreferences.prefs(context).edit().putBoolean("countdown_seconds", true).commit();
        reapply(direct(tall, arabic, schedule, SAMPLE.plusSeconds(61)));
        assertPhase(arabic, SAMPLE.plusSeconds(61));
      }
    System.out.println(
        "PASS widget hide-seconds snapshot, numeral preference, stopped prior clock and reapply"
            + " back to live seconds");
  }

  @Test
  public void missingAndExpiredScheduleClearLiveState() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (boolean tall : new boolean[] {false, true}) {
        configure(arabic, "normal", 100, "dark", "green");
        apply(direct(tall, arabic, schedule, SAMPLE), tall ? 200 : 160, tall ? 300 : 160);
        for (Instant now :
            new Instant[] {
              schedule.days.firstKey().minusDays(1).atStartOfDay(Schedule.ZONE).toInstant(),
              schedule.days.lastKey().plusDays(1).atStartOfDay(Schedule.ZONE).toInstant()
            }) {
          reapply(direct(tall, arabic, schedule, now));
          assertPhase(arabic, now);
          assertFalse(
              "Expired clock remains active",
              started((Chronometer) view("widget_iqamah_countdown")));
          assertRows(arabic, now);
          assertRenderedBounds(
              (arabic ? "ar" : "en")
                  + "-"
                  + (tall ? "tall" : "compact")
                  + "-outside-"
                  + now.atZone(Schedule.ZONE).toLocalDate());
        }
        reapply(direct(tall, arabic, null, SAMPLE));
        assertFalse(
            "Missing-schedule clock remains active",
            started((Chronometer) view("widget_iqamah_countdown")));
        assertFalse(visible(view("widget_iqamah_countdown")));
        assertFalse(visible(view("widget_countdown_minutes")));
        assertTrue(
            "Missing schedule recovery instruction",
            value("widget_next_name").contains(arabic ? "افتح" : "Open"));
      }
    System.out.println(
        "PASS widget missing timetable, before coverage, expiry and stale-clock cleanup");
  }

  @Test
  public void refreshSettingsAndAccessibility() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (boolean tall : new boolean[] {false, true}) {
        configure(arabic, "normal", 100, "dark", "green");
        apply(direct(tall, arabic, schedule, SAMPLE), tall ? 200 : 160, tall ? 300 : 160);
        assertNull(
            "Root description must not mask its spoken prayer/time children",
            root.getContentDescription());
        for (String name : new String[] {"widget_refresh"}) {
          View button = view(name);
          assertNotNull(name, button);
          assertTrue(name + " not clickable", button.isClickable());
          assertTrue(name + " invisible", visible(button));
          assertTrue(
              name + " undersized touch area " + button.getWidth() + "x" + button.getHeight(),
              button.getWidth() >= 40 && button.getHeight() >= 40);
          assertNotNull(name + " has no accessibility label", button.getContentDescription());
          assertTrue(
              name + " empty accessibility label", button.getContentDescription().length() >= 3);
          assertEquals(
              name + " accessibility click",
              true,
              button.createAccessibilityNodeInfo().isClickable());
        }
        int broadcasts =
            Shadows.shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents().size();
        assertTrue(view("widget_refresh").performClick());
        List<Intent> sent =
            Shadows.shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents();
        assertTrue("Refresh must send a broadcast", sent.size() > broadcasts);
        Intent refresh = sent.get(sent.size() - 1);
        assertNotNull("Refresh must target an explicit app receiver", refresh.getComponent());
        assertEquals(context.getPackageName(), refresh.getComponent().getPackageName());
        assertEquals("Refresh action", PrayerWidget.ACTION_REFRESH, refresh.getAction());
        assertEquals(
            "Refresh receiver",
            PrayerWidget.class.getName(),
            refresh.getComponent().getClassName());
        assertFalse("Requested settings icon must be hidden",visible(view("widget_settings")));
        assertFalse("Duplicate target must be hidden",visible(view("widget_next_time")));
        Intent settings = new Intent(context,MainActivity.class).putExtra(PrayerWidget.OPEN_WIDGET_SETTINGS,true);
        assertNotNull("Settings click must open activity", settings);
        assertEquals(MainActivity.class.getName(), settings.getComponent().getClassName());
        assertTrue(
            "Direct widget-settings extra missing",
            settings.getBooleanExtra(PrayerWidget.OPEN_WIDGET_SETTINGS, false));
        ActivityController<MainActivity> settingsActivity =
            Robolectric.buildActivity(MainActivity.class, settings)
                .create()
                .start()
                .resume()
                .visible();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("Settings shortcut did not open widget preferences", dialog);
        assertTrue("Widget preferences dialog is hidden", dialog.isShowing());
        assertTrue(
            "Direct shortcut opened wrong settings",
            Shadows.shadowOf(dialog).getTitle().toString().contains(arabic ? "الأدوات" : "Widget"));
        assertTrue("Widget preference options", dialog.getListView().getAdapter().getCount() >= 5);
        dialog.dismiss();
        settingsActivity.pause().stop().destroy();
      }
    System.out.println(
        "PASS widget refresh broadcast, direct widget-settings launch, localized labels and 40dp+"
            + " click targets");
  }

  private AlertDialog latestShowing(String label) {
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
    assertNotNull(label + ": missing dialog", dialog);
    assertTrue(label + ": dialog hidden", dialog.isShowing());
    return dialog;
  }

  private void chooseItem(AlertDialog dialog, int index) {
    android.widget.ListView list = dialog.getListView();
    assertNotNull("Expected choice dialog", list);
    list.performItemClick(list.getAdapter().getView(index, null, list), index, index);
    Shadows.shadowOf(Looper.getMainLooper()).idle();
  }

  private String title(AlertDialog dialog) {
    return Shadows.shadowOf(dialog).getTitle().toString();
  }

  @Test
  public void widgetSettingsIntentAndDialogLifecycle() throws Exception {
    for (boolean arabic : new boolean[] {false, true}) {
      configure(arabic, "normal", 100, "dark", "green");
      Intent shortcut =
          new Intent(context, MainActivity.class).putExtra(PrayerWidget.OPEN_WIDGET_SETTINGS, true);
      ActivityController<MainActivity> activity =
          Robolectric.buildActivity(MainActivity.class, shortcut)
              .create()
              .start()
              .resume()
              .visible();
      try {
        AlertDialog menu = latestShowing("Cold widget-settings shortcut");
        assertTrue(
            title(menu), title(menu).contains(arabic ? "إعدادات الأدوات" : "Widget settings"));
        assertFalse(
            "Shortcut is one-shot, not reopened on every resume",
            activity.get().getIntent().hasExtra(PrayerWidget.OPEN_WIDGET_SETTINGS));
        activity.pause().resume();
        assertSame(
            "Resume must not stack another settings dialog",
            menu,
            latestShowing("Resumed widget settings"));
        activity.recreate();
        menu = latestShowing("Recreated root widget settings");
        assertTrue(
            title(menu), title(menu).contains(arabic ? "إعدادات الأدوات" : "Widget settings"));
        String[] enTitles = {
          "Widget background",
          "Widget colour",
          "Background opacity",
          "Widget text size",
          "Countdown seconds",
          "Countdown & refresh"
        };
        String[] arTitles = {
          "خلفية الأداة",
          "لون الأداة",
          "تعتيم الخلفية",
          "حجم نص الأداة",
          "ثواني العد التنازلي",
          "العد التنازلي والتحديث"
        };
        for (int page = 0; page < enTitles.length; page++) {
          chooseItem(menu, page);
          AlertDialog child = latestShowing("Widget submenu " + page);
          String expected = arabic ? arTitles[page] : enTitles[page];
          assertEquals("Widget submenu title", expected, title(child));
          activity.recreate();
          child = latestShowing("Recreated widget submenu " + page);
          assertEquals("Recreation must restore the current settings page", expected, title(child));
          View back =
              child.getButton(
                  page == 5 ? DialogInterface.BUTTON_POSITIVE : DialogInterface.BUTTON_NEGATIVE);
          assertNotNull("Submenu Back button", back);
          assertTrue(back.performClick());
          menu = latestShowing("Back to root widget settings");
          assertTrue(
              title(menu), title(menu).contains(arabic ? "إعدادات الأدوات" : "Widget settings"));
        }
        // Exercise each actual preference callback, not only opening the settings UI.
        for (int[] selection : new int[][] {{0, 1}, {1, 2}, {2, 1}, {3, 0}, {4, 0}}) {
          chooseItem(menu, selection[0]);
          chooseItem(latestShowing("Widget preference choice"), selection[1]);
          menu = latestShowing("Widget menu after preference change");
        }
        assertEquals(
            "Widget mode choice",
            "light",
            AppPreferences.prefs(context).getString("widget_mode", ""));
        assertEquals(
            "Widget accent choice",
            "navy",
            AppPreferences.prefs(context).getString("widget_accent", ""));
        assertEquals(
            "Widget opacity choice", 85, AppPreferences.prefs(context).getInt("widget_opacity", 0));
        assertEquals(
            "Widget text-size choice",
            "small",
            AppPreferences.prefs(context).getString("widget_text", ""));
        assertFalse(
            "Widget seconds choice",
            AppPreferences.prefs(context).getBoolean("countdown_seconds", true));
        // Warm singleTop delivery replaces the open widget page and consumes each new intent.
        chooseItem(menu, 3);
        AlertDialog old = latestShowing("Widget text-size submenu before warm intent");
        activity
            .get()
            .onNewIntent(
                new Intent(context, MainActivity.class)
                    .putExtra(PrayerWidget.OPEN_WIDGET_SETTINGS, true));
        menu = latestShowing("Warm widget-settings intent");
        assertFalse("Warm shortcut must dismiss the old submenu", old.isShowing());
        assertTrue(
            title(menu), title(menu).contains(arabic ? "إعدادات الأدوات" : "Widget settings"));
        old = menu;
        activity
            .get()
            .onNewIntent(
                new Intent(context, MainActivity.class)
                    .putExtra(PrayerWidget.OPEN_WIDGET_SETTINGS, true));
        menu = latestShowing("Repeated widget-settings intent");
        assertFalse("Repeated shortcut must not stack its prior dialog", old.isShowing());
        assertFalse(
            "Warm shortcut extra not consumed",
            activity.get().getIntent().hasExtra(PrayerWidget.OPEN_WIDGET_SETTINGS));
        // Explicit cancellation should be remembered as closed across recreation.
        menu.cancel();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        activity.recreate();
        assertFalse(
            "Cancelled widget settings reopened after recreation",
            ShadowAlertDialog.getLatestAlertDialog().isShowing());
        activity
            .get()
            .onNewIntent(
                new Intent(context, MainActivity.class)
                    .putExtra(PrayerWidget.OPEN_WIDGET_SETTINGS, true));
        menu = latestShowing("Widget settings after cancel");
        chooseItem(menu, 3);
        latestShowing("Text-size submenu before cancel").cancel();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        activity.recreate();
        assertFalse(
            "Cancelled widget submenu reopened after recreation",
            ShadowAlertDialog.getLatestAlertDialog().isShowing());
        activity
            .get()
            .onNewIntent(
                new Intent(context, MainActivity.class)
                    .putExtra(PrayerWidget.OPEN_WIDGET_SETTINGS, true));
        menu = latestShowing("Widget settings before root Back");
        menu.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        AlertDialog home = latestShowing("Root Back returns to app settings");
        assertTrue(
            "Root Back opened wrong menu",
            home.getListView() != null && home.getListView().getAdapter().getCount() >= 9);
        home.dismiss();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        activity.recreate();
        assertFalse(
            "Widget menu reopened after leaving via Back",
            ShadowAlertDialog.getLatestAlertDialog().isShowing());
      } finally {
        activity.pause().stop().destroy();
      }
    }
    System.out.println(
        "PASS widget settings cold/warm/repeated intents, root + all six submenu recreations, Back"
            + " and Cancel in both languages");
  }

  @Test
  public void restoredWidgetPageConsumesOriginalShortcutIntent() throws Exception {
    for (boolean arabic : new boolean[] {false, true}) {
      configure(arabic, "normal", 100, "dark", "green");
      Intent original =
          new Intent(context, MainActivity.class).putExtra(PrayerWidget.OPEN_WIDGET_SETTINGS, true);
      ActivityController<MainActivity> activity =
          Robolectric.buildActivity(MainActivity.class, new Intent(original))
              .create()
              .start()
              .resume()
              .visible();
      chooseItem(latestShowing("Initial widget settings"), 3);
      Bundle saved = new Bundle();
      activity.saveInstanceState(saved);
      activity.pause().stop().destroy();
      assertEquals("Saved current widget page", "text", saved.getString("widget_page"));
      activity =
          Robolectric.buildActivity(MainActivity.class, new Intent(original))
              .create(saved)
              .start()
              .resume()
              .visible();
      try {
        AlertDialog restored = latestShowing("Process-restored widget text-size page");
        assertEquals(arabic ? "حجم نص الأداة" : "Widget text size", title(restored));
        assertFalse(
            "Restored original shortcut flag must still be consumed",
            activity.get().getIntent().hasExtra(PrayerWidget.OPEN_WIDGET_SETTINGS));
        restored.cancel();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        activity.recreate();
        assertFalse(
            "Dismissed restored page must not reopen from the old shortcut",
            ShadowAlertDialog.getLatestAlertDialog().isShowing());
        Bundle dismissed = new Bundle();
        activity.saveInstanceState(dismissed);
        assertFalse(
            "Dismissed state must contain no widget page", dismissed.containsKey("widget_page"));
        activity.pause().stop().destroy();
        activity =
            Robolectric.buildActivity(MainActivity.class, new Intent(original))
                .create(dismissed)
                .start()
                .resume()
                .visible();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(
            "Process recreation with no open widget page must not replay original shortcut",
            ShadowAlertDialog.getLatestAlertDialog().isShowing());
        assertFalse(
            "Dismissed-state restoration must consume original shortcut flag",
            activity.get().getIntent().hasExtra(PrayerWidget.OPEN_WIDGET_SETTINGS));
      } finally {
        activity.pause().stop().destroy();
      }
    }
    System.out.println(
        "PASS process-restored widget page consumes original shortcut and remains dismissed on next"
            + " recreation");
  }

  @Test
  public void numeralTimeFormatAndLargeFontProfiles() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (boolean eastern : new boolean[] {false, true})
        for (boolean tall : new boolean[] {false, true})
          for (float scale : new float[] {1f, 1.3f, 2f}) {
            configure(arabic, "large", 0, "dark", "sand");
            RuntimeEnvironment.setFontScale(scale);
            AppPreferences.prefs(context)
                .edit()
                .putBoolean("twelve_hour", true)
                .putBoolean("eastern_digits", eastern)
                .commit();
            apply(direct(tall, arabic, schedule, SAMPLE), tall ? 160 : 140, tall ? 220 : 160);
            assertPhase(arabic, SAMPLE);
            assertRows(arabic, SAMPLE);
            assertEquals(
                "Numbers use an explicit LTR time field",
                View.LAYOUT_DIRECTION_LTR,
                view("widget_next_time").getLayoutDirection());
            String label =
                (arabic ? "ar" : "en")
                    + "-"
                    + (tall ? "tall" : "compact")
                    + "-12hour-eastern"
                    + eastern
                    + "-font"
                    + scale;
            assertRenderedBounds(label);
          }
    System.out.println(
        "PASS widget 24 twelve-hour/Eastern-numeral/font1.0/1.3/2.0 rendered profiles at minimum"
            + " compact/expanded sizes");
  }

  @Test
  public void providerLifecycleAndRepeatedResizeDoesNotCrash() throws Exception {
    for (Class<? extends android.appwidget.AppWidgetProvider> provider :
        java.util.Arrays.<Class<? extends android.appwidget.AppWidgetProvider>>asList(
            PrayerWidget.class, PrayerWidgetTall.class)) {
      int widgetId =
          Shadows.shadowOf(AppWidgetManager.getInstance(context))
              .createWidget(provider, R.layout.widget_prayer_compact);
      android.appwidget.AppWidgetProvider instance =
          Shadows.shadowOf(AppWidgetManager.getInstance(context)).getAppWidgetProviderFor(widgetId);
      instance.onEnabled(context);
      for (int[] size : new int[][] {{140, 160}, {160, 240}, {300, 300}, {110, 110}, {160, 160}}) {
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, size[0]);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, size[0]);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, size[1]);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, size[1]);
        AppWidgetManager.getInstance(context).updateAppWidgetOptions(widgetId, options);
        instance.onAppWidgetOptionsChanged(
            context, AppWidgetManager.getInstance(context), widgetId, options);
        instance.onUpdate(context, AppWidgetManager.getInstance(context), new int[] {widgetId});
        assertNotNull(
            "Provider update lost rendered RemoteViews",
            Shadows.shadowOf(AppWidgetManager.getInstance(context)).getViewFor(widgetId));
      }
      instance.onReceive(context, new Intent(PrayerWidget.ACTION_REFRESH));
      instance.onReceive(context, new Intent(PrayerWidget.ACTION_MINUTE));
      instance.onDeleted(context, new int[] {widgetId});
      instance.onDisabled(context);
    }
    System.out.println(
        "PASS actual providers enable/update/options-change/resize/refresh/minute/delete/disable"
            + " callbacks without a crash");
  }

  @Test
  public void nativeChronometerTicksWithoutReapplying() throws Exception {
    for (boolean arabic : new boolean[] {false, true}) {
      configure(arabic, "normal", 100, "dark", "green");
      apply(direct(false, arabic, schedule, SAMPLE), 160, 160);
      Chronometer clock = (Chronometer) view("widget_iqamah_countdown");
      String before = clock.getText().toString();
      long base = clock.getBase();
      Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
      layout();
      assertNotEquals("Host-side native clock did not tick", before, clock.getText().toString());
      assertEquals("Ticking must keep the same absolute countdown target", base, clock.getBase());
      assertRenderedBounds((arabic ? "ar" : "en") + "-native-live-tick");
    }
    System.out.println(
        "PASS native Chronometer ticks for three seconds with no new RemoteViews/app refresh");
  }

  @Test
  public void countdownAndRowsUseChosenNumeralsAcrossSystemLanguages() throws Exception {
    Locale original = Locale.getDefault();
    try {
      for (String system : new String[]{"en-US", "ar-AE"})
        for (boolean arabic : new boolean[]{false, true})
          for (boolean eastern : new boolean[]{false, true})
            for (boolean tall : new boolean[]{false, true}) {
              configure(arabic, "normal", 100, "dark", "green");
              Locale.setDefault(Locale.forLanguageTag(system));
              AppPreferences.prefs(context).edit().putBoolean("eastern_digits", eastern).commit();
              apply(responsive(tall, arabic, schedule, SAMPLE, 140, tall ? 220 : 160), 140, tall ? 220 : 160);
              assertTrue("All five prayers must remain present", expanded());
              assertRows(arabic, SAMPLE);
              // Locale names alone do not guarantee the timer's numeral repertoire across
              // Android/JDK/ICU versions. Check the native formatter, then independently
              // assert the actual rendered counter contains only the chosen digits below.
              String nativeCounter = android.text.format.DateUtils.formatElapsedTime(12345L);
              boolean match = nativeCounter.equals(AppPreferences.digits(context, nativeCounter));
              assertEquals("Only matching launcher numerals may use native seconds", match,
                  visible(view("widget_iqamah_countdown")));
              TextView counter = text(match ? "widget_iqamah_countdown" : "widget_countdown_minutes");
              assertTrue("Chosen numeral countdown must be visible", visible(counter));
              for (String key : new String[]{"widget_next_time", "widget_fajr_time", "widget_dhuhr_time", "widget_asr_time", "widget_maghrib_time", "widget_isha_time"}) {
                String value = value(key);
                assertTrue("Prayer times must use chosen numerals: " + value,
                    value.matches(eastern ? ".*[٠-٩].*" : ".*[0-9].*"));
                assertFalse("Prayer times must not mix numerals: " + value,
                    value.matches(eastern ? ".*[0-9].*" : ".*[٠-٩].*"));
              }
              String before = counter.getText().toString();
              assertTrue("Countdown numeral mismatch: " + before,
                  before.matches(eastern ? ".*[٠-٩].*" : ".*[0-9].*"));
              assertFalse("Mixed countdown numerals: " + before,
                  before.matches(eastern ? ".*[0-9].*" : ".*[٠-٩].*"));
              if (!match) {
                assertFalse("Stopped native timer must not restart", started((Chronometer)view("widget_iqamah_countdown")));
                assertTrue("Fallback must disclose approximation", before.contains("≈"));
                reapply(direct(tall, arabic, schedule, SAMPLE.plusSeconds(61)));
                assertNotEquals("Localized snapshot must advance on refresh", before, counter.getText().toString());
              }
              assertRenderedBounds("numerals-"+system+"-ar"+arabic+"-eastern"+eastern+"-tall"+tall);
            }
    } finally { Locale.setDefault(original); }
    System.out.println("PASS 16 widget system/app-language/numeral/size combinations, all five rows, localized countdown and recovery");
  }

  @Test
  public void mismatchedNumeralsScheduleOneNonWakeupMinuteRefresh() throws Exception {
    Locale original = Locale.getDefault();
    try {
      configure(false, "normal", 100, "dark", "green");
      Locale.setDefault(Locale.US);
      AppWidgetProviderInfo info = new AppWidgetProviderInfo();
      info.provider = new ComponentName(context, PrayerWidget.class);
      Shadows.shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(81, info);
      ShadowAlarmManager alarms = Shadows.shadowOf(context.getSystemService(AlarmManager.class));
      AppPreferences.prefs(context).edit().putBoolean("eastern_digits", true).commit();
      PrayerWidget.scheduleMinute(context, SAMPLE);
      assertEquals(1, minuteAlarms(alarms).size());
      assertEquals(AlarmManager.RTC, minuteAlarms(alarms).get(0).getType());
      assertFalse(minuteAlarms(alarms).get(0).isAllowWhileIdle());
      PrayerWidget.scheduleMinute(context, SAMPLE.plusSeconds(1));
      assertEquals(1, minuteAlarms(alarms).size());
      AppPreferences.prefs(context).edit().putBoolean("eastern_digits", false).commit();
      PrayerWidget.scheduleMinute(context, SAMPLE);
      assertTrue("Matching numerals restore live seconds without snapshot alarms", minuteAlarms(alarms).isEmpty());
    } finally { Locale.setDefault(original); }
  }

  private List<ShadowAlarmManager.ScheduledAlarm> minuteAlarms(ShadowAlarmManager manager) {
    List<ShadowAlarmManager.ScheduledAlarm> result = new ArrayList<>();
    for (ShadowAlarmManager.ScheduledAlarm alarm : manager.getScheduledAlarms())
      if (alarm.operation != null
          && PrayerWidget.ACTION_MINUTE.equals(
              Shadows.shadowOf(alarm.operation).getSavedIntent().getAction())) result.add(alarm);
    return result;
  }

  @Test
  public void minuteSnapshotAlarmIsBoundedNonWakeupAndIsolated() {
    AlarmManager alarms = context.getSystemService(AlarmManager.class);
    ShadowAlarmManager shadow = Shadows.shadowOf(alarms);
    PrayerWidget.scheduleMinute(context, SAMPLE);
    assertTrue("No refresh work without a widget", minuteAlarms(shadow).isEmpty());
    AppWidgetProviderInfo info = new AppWidgetProviderInfo();
    info.provider = new ComponentName(context, PrayerWidget.class);
    Shadows.shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(81, info);
    assertTrue(PrayerWidget.hasWidgets(context));
    PrayerWidget.scheduleMinute(context, SAMPLE);
    assertTrue(
        "Live seconds must not create per-minute app alarms", minuteAlarms(shadow).isEmpty());
    List<PendingIntent> sentinels = new ArrayList<>();
    for (String action :
        new String[] {
          AlertScheduler.ACTION, AlertScheduler.REMINDER_ACTION, AlertScheduler.IQAMAH_WIDGET_ACTION
        }) {
      PendingIntent intent =
          PendingIntent.getBroadcast(
              context,
              900 + sentinels.size(),
              new Intent(context, AlarmReceiver.class).setAction(action),
              PendingIntent.FLAG_IMMUTABLE);
      sentinels.add(intent);
      alarms.set(AlarmManager.RTC_WAKEUP, SAMPLE.plusSeconds(3600).toEpochMilli(), intent);
    }
    AppPreferences.prefs(context).edit().putBoolean("countdown_seconds", false).commit();
    PrayerWidget.scheduleMinute(context, SAMPLE);
    assertEquals("Exactly one minute refresh", 1, minuteAlarms(shadow).size());
    ShadowAlarmManager.ScheduledAlarm minute = minuteAlarms(shadow).get(0);
    assertEquals(
        "Snapshot updates must never wake a sleeping phone", AlarmManager.RTC, minute.getType());
    assertFalse("Snapshot update must not consume idle alarm quota", minute.isAllowWhileIdle());
    assertNotEquals("Snapshot updates must be inexact", 0, minute.getWindowLengthMs());
    assertEquals(
        "Snapshot target is next wall-clock minute",
        (SAMPLE.toEpochMilli() / 60000 + 1) * 60000,
        minute.getTriggerAtMs());
    assertEquals("Do not create repeating alarms", 0, minute.getIntervalMs());
    assertTrue(
        "Snapshot PendingIntent must be immutable",
        Shadows.shadowOf(minute.operation).isImmutable());
    PrayerWidget.scheduleMinute(context, SAMPLE.plusSeconds(5));
    assertEquals("Refresh scheduling must replace, not accumulate", 1, minuteAlarms(shadow).size());
    AppPreferences.prefs(context).edit().putBoolean("countdown_seconds", true).commit();
    PrayerWidget.scheduleMinute(context, SAMPLE.plusSeconds(5));
    assertTrue(
        "Returning to live seconds must cancel minute update", minuteAlarms(shadow).isEmpty());
    AppPreferences.prefs(context).edit().putBoolean("countdown_seconds", false).commit();
    PrayerWidget.scheduleMinute(
        context, schedule.days.lastKey().plusDays(1).atStartOfDay(Schedule.ZONE).toInstant());
    assertTrue(
        "Expired timetable must not keep a minute refresh alive", minuteAlarms(shadow).isEmpty());
    for (PendingIntent sentinel : sentinels) {
      boolean found = false;
      for (ShadowAlarmManager.ScheduledAlarm alarm : shadow.getScheduledAlarms())
        if (sentinel.equals(alarm.operation)) found = true;
      assertTrue("Widget snapshot maintenance must preserve prayer/reminder/iqamah alarms", found);
    }
    System.out.println(
        "PASS minute snapshots use one bounded inexact non-wakeup RTC alarm, preserve prayer"
            + " alarms, and cancel for seconds/expiry/no widgets");
  }

  @Test
  @Config(sdk = {26, 29})
  public void olderAndroidResponsiveParcelAndReapply() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (boolean tall : new boolean[] {false, true}) {
        configure(arabic, "normal", 85, "dark", "navy");
        for (int[] size : new int[][] {{160, 160}, {160, 240}, {200, 300}, {300, 300}}) {
          RemoteViews views = responsive(tall, arabic, schedule, SAMPLE, size[0], size[1]);
          Parcel parcel = Parcel.obtain();
          views.writeToParcel(parcel, 0);
          parcel.setDataPosition(0);
          RemoteViews restored = RemoteViews.CREATOR.createFromParcel(parcel);
          parcel.recycle();
          apply(restored, size[0], size[1]);
          reapply(restored);
          assertPhase(arabic, SAMPLE);
          assertRows(arabic, SAMPLE);
          assertRenderedBounds(
              (arabic ? "ar" : "en")
                  + "-"
                  + (tall ? "tall" : "compact")
                  + "-api"
                  + Build.VERSION.SDK_INT
                  + "-"
                  + size[0]
                  + "x"
                  + size[1]);
        }
      }
    System.out.println(
        "PASS widget API" + Build.VERSION.SDK_INT + " responsive parcel/apply/reapply");
  }
}
