package ae.prayertimes.abudhabi;

import static org.junit.Assert.*;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.SystemClock;
import android.text.Layout;
import android.text.format.DateUtils;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowAppWidgetManager;

/**
 * Native Android regression coverage for the single, continuously resizable Salah widget.
 * Geometry is checked after real RemoteViews parceling, framework selection and View measurement.
 * Native Skia PNGs supplement the assertions; this does not claim OEM-launcher/device coverage.
 */
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class AdaptiveWidgetTest {
  private static final Instant NOW = Instant.parse("2026-10-17T08:00:00Z");
  private static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
  private static final String[] EN = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
  private static final String[] AR = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
  private static final int[][] SIZES = {
    {140, 80}, {140, 96}, {140, 120}, {140, 160}, {180, 240}, {250, 80},
    {300, 100}, {400, 80}, {400, 200}, {320, 500}, {600, 600},
    // Both sides of the orientation and chrome thresholds, and non-preset launcher sizes.
    {249, 80}, {251, 80}, {250, 124}, {250, 125}, {250, 126},
    {140, 159}, {141, 161}, {177, 213}, {301, 151}, {399, 201},
    {255, 128}, {256, 128}, {257, 128}, {256, 129}, {140, 111}, {140, 112}, {140, 113}
  };
  private Context context;
  private Schedule schedule;
  private ActivityController<Activity> controller;
  private FrameLayout host;
  private View root;
  private int width, height;
  private Locale originalLocale;

  @Before public void setUp() throws Exception {
    originalLocale = Locale.getDefault();
    configure(false, false, false, 1f, "normal", false);
    schedule = AlertScheduler.schedule(context);
    controller = Robolectric.buildActivity(Activity.class).setup().visible();
    host = new FrameLayout(controller.get());
    controller.get().setContentView(host);
  }

  @After public void tearDown() {
    if (host != null) host.removeAllViews();
    if (controller != null) controller.pause().stop().destroy();
    Locale.setDefault(originalLocale);
    RuntimeEnvironment.setFontScale(1f);
  }

  private void configure(boolean arabic, boolean eastern, boolean twelve, float scale,
      String text, boolean landscape) {
    qualifiers(arabic, landscape);
    RuntimeEnvironment.setFontScale(scale);
    context = RuntimeEnvironment.getApplication();
    // Keep the launcher locale independent of the in-app language preference.
    Locale.setDefault(Locale.US);
    AppPreferences.prefs(context).edit().clear()
        .putBoolean("arabic", arabic).putBoolean("eastern_digits", eastern)
        .putBoolean("twelve_hour", twelve).putBoolean("countdown_seconds", true)
        .putString("widget_text", text).putInt("widget_opacity", 100)
        .putString("widget_mode", "dark").putString("widget_accent", "green").commit();
  }

  private void qualifiers(boolean arabic, boolean landscape) {
    RuntimeEnvironment.setQualifiers((arabic ? "ar" : "en")
        + (landscape ? "-rUS-w1000dp-h800dp-land-mdpi" : "-rUS-w800dp-h1000dp-port-mdpi"));
  }

  private Bundle options(int w, int h) {
    Bundle options = new Bundle();
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, w);
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, w);
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, h);
    options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, h);
    if (Build.VERSION.SDK_INT >= 31) {
      ArrayList<SizeF> exact = new ArrayList<>();
      exact.add(new SizeF(w, h));
      options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, exact);
    }
    return options;
  }

  private RemoteViews parcel(RemoteViews views) {
    Parcel parcel = Parcel.obtain();
    try {
      views.writeToParcel(parcel, 0);
      parcel.setDataPosition(0);
      return RemoteViews.CREATOR.createFromParcel(parcel);
    } finally { parcel.recycle(); }
  }

  private RemoteViews select(RemoteViews views, float w, float h) throws Exception {
    if (Build.VERSION.SDK_INT < 31) return views;
    Method method = RemoteViews.class.getDeclaredMethod("getRemoteViewsToApply", Context.class, SizeF.class);
    method.setAccessible(true);
    return (RemoteViews) method.invoke(views, context, new SizeF(w, h));
  }

  private void responsive(Schedule data, Instant now, int w, int h) throws Exception {
    apply(select(parcel(PrayerWidgetAdaptive.responsive(context, data, now, options(w, h))), w, h), w, h);
  }

  private void direct(Schedule data, Instant now, int w, int h) {
    apply(PrayerWidgetAdaptive.render(context, data, now, w, h), w, h);
  }

  private void apply(RemoteViews views, int w, int h) {
    width = w;
    height = h;
    host.removeAllViews();
    root = views.apply(context, host);
    host.addView(root, new FrameLayout.LayoutParams(w, h));
    layout();
  }

  private void reapply(Schedule data, Instant now) {
    PrayerWidgetAdaptive.render(context, data, now, width, height).reapply(context, root);
    layout();
  }

  private void layout() {
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    for (int i = 0; i < 2; i++) {
      host.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
      host.layout(0, 0, width, height);
    }
  }

  private View view(String name) {
    int id = context.getResources().getIdentifier(name, "id", context.getPackageName());
    assertTrue("Missing resource " + name, id != 0);
    return root.findViewById(id);
  }

  private TextView text(String name) {
    View view = view(name);
    assertTrue("Missing TextView " + name, view instanceof TextView);
    return (TextView) view;
  }

  private boolean visible(View view) {
    if (view == null || view.getVisibility() != View.VISIBLE) return false;
    for (ViewParent p = view.getParent(); p instanceof View; p = p.getParent())
      if (((View) p).getVisibility() != View.VISIBLE) return false;
    return true;
  }

  private List<View> all(View view) {
    List<View> result = new ArrayList<>();
    result.add(view);
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) result.addAll(all(group.getChildAt(i)));
    }
    return result;
  }

  private String name(View view) {
    try { return context.getResources().getResourceEntryName(view.getId()); }
    catch (Exception ignored) { return view.getClass().getSimpleName(); }
  }

  private Rect rect(View view) {
    Rect rect = new Rect(0, 0, view.getWidth(), view.getHeight());
    host.offsetDescendantRectToMyCoords(view, rect);
    return rect;
  }

  private void assertBoundsAndUniformText(String profile) {
    Float common = null;
    Typeface commonTypeface = null;
    int texts = 0;
    for (View view : all(root)) {
      if (!visible(view) || !(view instanceof TextView)) continue;
      TextView text = (TextView) view;
      if (text.getText().length() == 0) continue;
      texts++;
      String label = profile + " " + name(text) + " [" + text.getText() + "]";
      if (common == null) common = text.getTextSize();
      if (commonTypeface == null) commonTypeface = text.getTypeface();
      assertEquals(label + " must use the shared visible font size", common, text.getTextSize(), .02f);
      assertEquals(label + " must use the shared typeface", commonTypeface, text.getTypeface());
      assertTrue(label + " has a nonpositive font size", text.getTextSize() > 0);
      if (width >= 140 && height >= 96)
        assertTrue(label + " falls below the supported 10dp reading floor", text.getTextSize() >= 9.99f);
      assertEquals(label + " must not fade with background opacity", 1f, text.getAlpha(), .001f);
      Rect box = rect(text);
      assertTrue(label + " lies outside host: " + box,
          box.left >= 0 && box.top >= 0 && box.right <= width && box.bottom <= height);
      Rect actual = new Rect();
      assertTrue(label + " is fully clipped", text.getGlobalVisibleRect(actual));
      assertEquals(label + " clipped width", text.getWidth(), actual.width());
      assertEquals(label + " clipped height", text.getHeight(), actual.height());
      Layout lines = text.getLayout();
      assertNotNull(label + " lacks native text layout", lines);
      int availableWidth = text.getWidth() - text.getCompoundPaddingLeft() - text.getCompoundPaddingRight();
      int availableHeight = text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom();
      assertTrue(label + " has no text area", availableWidth > 0 && availableHeight > 0);
      assertTrue(label + " text height " + lines.getHeight() + " > " + availableHeight,
          lines.getHeight() <= availableHeight + 1);
      for (int line = 0; line < lines.getLineCount(); line++) {
        assertEquals(label + " ellipsis", 0, lines.getEllipsisCount(line));
        assertTrue(label + " text width " + lines.getLineWidth(line) + " > " + availableWidth,
            lines.getLineWidth(line) <= availableWidth + 1);
      }
      assertTrue(label + " loses the end of its text",
          lines.getLineCount() > 0 && lines.getLineEnd(lines.getLineCount() - 1) == text.length());
    }
    assertTrue(profile + " must display all ten prayer name/time TextViews", texts >= 10);
  }

  private void assertRows(Schedule data, Instant now, boolean arabic) {
    LocalDate today = now.atZone(Schedule.ZONE).toLocalDate();
    LocalTime[] saved = data == null ? null : data.days.get(today);
    PrayerPhase phase = data == null ? null : AppPreferences.phase(context, data, now);
    boolean horizontal = width >= 250 && (long) width >= 2L * height;
    Rect prior = null;
    for (int i = 0; i < KEYS.length; i++) {
      String key = "widget_" + KEYS[i];
      TextView prayerName = text(key + "_name"), prayerTime = text(key + "_time");
      View row = view(key + "_row");
      assertTrue("Hidden prayer row " + key, visible(row));
      assertTrue("Hidden prayer name " + key, visible(prayerName));
      assertTrue("Hidden prayer time " + key, visible(prayerTime));
      assertEquals("Prayer name " + key, (arabic ? AR : EN)[i], prayerName.getText().toString());
      String expected = saved == null ? "—" : AppPreferences.time(context, saved[Schedule.PRAYERS[i]]);
      assertEquals("Exact saved time " + key, expected, prayerTime.getText().toString());
      if (saved != null) assertNumerals(prayerTime.getText().toString(), AppPreferences.eastern(context));
      assertEquals("Time field must stay LTR", View.LAYOUT_DIRECTION_LTR, prayerTime.getLayoutDirection());
      assertNotNull("Accessible prayer row " + key, row.getContentDescription());
      assertTrue(row.getContentDescription().toString().contains(expected));
      boolean active = phase != null && phase.index == Schedule.PRAYERS[i]
          && phase.adhanAt.toLocalDate().equals(today);
      assertTrue("Explicit row highlight reset " + key, row.getBackground() instanceof ColorDrawable);
      assertEquals("Active prayer highlight " + key, active,
          Color.alpha(((ColorDrawable) row.getBackground()).getColor()) != 0);
      Rect current = rect(row);
      if (prior != null) {
        if (horizontal) {
          assertEquals("Horizontal prayer columns must align vertically", prior.top, current.top);
          assertTrue("Horizontal prayer columns overlap or have wrong reading order",
              arabic ? current.right <= prior.left : prior.right <= current.left);
        } else assertTrue("Vertical prayer rows overlap or reorder", prior.bottom <= current.top);
      }
      prior = current;
    }
    assertEquals("App language controls root direction", arabic ? View.LAYOUT_DIRECTION_RTL
        : View.LAYOUT_DIRECTION_LTR, root.getLayoutDirection());
    assertTrue("Widget body must open the app", root.isClickable());
  }

  private void assertNumerals(String value, boolean eastern) {
    assertTrue("Requested numerals absent: " + value, value.matches(eastern ? ".*[٠-٩].*" : ".*[0-9].*"));
    assertFalse("Mixed/wrong numerals: " + value, value.matches(eastern ? ".*[0-9۰-۹].*" : ".*[٠-٩۰-۹].*"));
  }

  private boolean countdownVisible() {
    return visible(view("widget_iqamah_countdown")) || visible(view("widget_countdown_minutes"));
  }

  private boolean started(Chronometer timer) throws Exception {
    Field field = Chronometer.class.getDeclaredField("mStarted");
    field.setAccessible(true);
    return field.getBoolean(timer);
  }

  private void assertChromeAndPhase(Schedule data, Instant now, boolean arabic) throws Exception {
    PrayerPhase phase = data == null ? null : AppPreferences.phase(context, data, now);
    boolean horizontal = width >= 250 && (long) width >= 2L * height;
    boolean room = width >= 140 && height >= (horizontal ? 128 : 160);
    boolean expectedCountdown = phase != null && room;
    assertEquals("Countdown visibility at " + width + "x" + height, expectedCountdown, countdownVisible());
    int refreshes = 0, buttons = 0;
    for (View view : all(root)) if (visible(view)) {
      if (view.getId() == R.id.widget_refresh) refreshes++;
      if (view instanceof ImageButton) buttons++;
    }
    assertEquals("Exactly one refresh when a countdown is shown", expectedCountdown ? 1 : 0, refreshes);
    assertEquals("No redundant visible action buttons", refreshes, buttons);
    if (height <= 80) {
      assertFalse("Compact strips omit next-prayer chrome", visible(view("widget_next_name")));
      assertFalse("Compact strips omit phase chrome", visible(view("widget_label")));
    }
    if (room && data != null && data.days.containsKey(now.atZone(Schedule.ZONE).toLocalDate())) {
      assertTrue("Roomy widget should show today's date", visible(view("widget_meta")));
      assertEquals(AppPreferences.digits(context, now.atZone(Schedule.ZONE).format(
          DateTimeFormatter.ofPattern("d MMM", arabic ? new Locale("ar") : Locale.ENGLISH))),
          text("widget_meta").getText().toString());
    }
    Chronometer timer = (Chronometer) view("widget_iqamah_countdown");
    if (!expectedCountdown) {
      assertFalse("Hidden/expired timer must stop", started(timer));
      return;
    }
    String prayer = (arabic ? AR : EN)[Arrays.binarySearch(Schedule.PRAYERS, phase.index)];
    assertTrue("Next prayer must identify the phase", text("widget_next_name").getText().toString().contains(prayer));
    if (phase.iqamah) {
      assertTrue("Estimated iqamah must be visibly labeled", visible(view("widget_label")));
      assertTrue("Iqamah label lost", text("widget_label").getText().toString().toLowerCase(Locale.ROOT)
          .contains(arabic ? "إقامة" : "iqamah"));
    }
    assertEquals("Choose native seconds only when its numerals match", PrayerWidget.liveSeconds(context), visible(timer));
    assertEquals("Choose localized minute snapshot otherwise", !PrayerWidget.liveSeconds(context),
        visible(view("widget_countdown_minutes")));
    long until = Math.max(0, Duration.between(now, phase.targetAt.toInstant()).toMillis());
    if (PrayerWidget.liveSeconds(context)) {
      assertTrue("Chronometer must count down", timer.isCountDown());
      assertTrue("Visible live timer must start", started(timer));
      assertEquals("Chronometer target", until, timer.getBase() - SystemClock.elapsedRealtime(), 1000);
      assertNumerals(timer.getText().toString(), AppPreferences.eastern(context));
    } else {
      assertFalse("Snapshot must stop its hidden native timer", started(timer));
      long minutes = (until + 59999L) / 60000L;
      String expected = AppPreferences.digits(context,
          String.format(Locale.US, "≈ %02d:%02d", minutes / 60, minutes % 60));
      assertEquals("Ceiling-minute snapshot", expected, text("widget_countdown_minutes").getText().toString());
      assertNumerals(expected, AppPreferences.eastern(context));
    }
    assertTrue("Refresh action must be clickable", view("widget_refresh").isClickable());
    assertNotNull("Refresh needs an accessibility label", view("widget_refresh").getContentDescription());
  }

  @Test public void fullNativeSizeLanguageNumeralAndFontMatrix() throws Exception {
    int profiles = 0;
    List<String> failures = new ArrayList<>();
    for (boolean arabic : new boolean[] {false, true})
      for (boolean eastern : new boolean[] {false, true})
        for (boolean twelve : new boolean[] {false, true})
          for (float scale : new float[] {.85f, 1f, 1.5f, 2f})
            for (String textSize : new String[] {"small", "normal", "large"}) {
              configure(arabic, eastern, twelve, scale, textSize, false);
              Map<String, ?> prefs = new HashMap<String, Object>(AppPreferences.prefs(context).getAll());
              for (int[] size : SIZES) {
                String label = (arabic ? "ar" : "en") + "-eastern" + eastern + "-12h" + twelve
                    + "-font" + scale + "-" + textSize + "-" + size[0] + "x" + size[1];
                try {
                  responsive(schedule, NOW, size[0], size[1]);
                  assertRows(schedule, NOW, arabic);
                  assertChromeAndPhase(schedule, NOW, arabic);
                  assertBoundsAndUniformText(label);
                  assertEquals("Rendering must not modify saved preferences", prefs, AppPreferences.prefs(context).getAll());
                  profiles++;
                } catch (AssertionError | RuntimeException e) {
                  if (failures.size() < 30) {
                    failures.add(label + ": " + e.getMessage());
                    if (failures.size() <= 5) System.out.println("Adaptive profile failure: " + label + ": " + e.getMessage());
                  }
                }
              }
            }
    int expected = 2 * 2 * 2 * 4 * 3 * SIZES.length;
    System.out.println("Adaptive full matrix: " + profiles + "/" + expected + " native profiles passed");
    assertTrue("Adaptive matrix failures: " + failures, failures.isEmpty());
    assertEquals(expected, profiles);
  }

  private List<String> geometrySignature() {
    List<String> result = new ArrayList<>();
    for (View view : all(root)) if (visible(view) && view.getId() != View.NO_ID) {
      String value = name(view) + ":" + rect(view) + ":" + view.getLayoutDirection();
      if (view instanceof TextView) {
        TextView text = (TextView) view;
        value += ":" + text.getTextSize() + ":" + text.getCurrentTextColor();
        if (!(text instanceof Chronometer)) value += ":" + text.getText();
      }
      result.add(value);
    }
    return result;
  }

  @Test public void exactLauncherSizesSurviveParcelAndUseFrameworkSelection() throws Exception {
    int[][] dimensions = {{140,80},{177,213},{250,125},{250,126},{301,151},{400,80},{600,600}};
    for (boolean arabic : new boolean[] {false, true}) {
      configure(arabic, true, true, 2f, "large", false);
      Bundle options = options(140, 80);
      // Deliberately contradictory legacy bounds prove that OPTION_APPWIDGET_SIZES wins.
      options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 142);
      options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 82);
      ArrayList<SizeF> sizes = new ArrayList<>();
      for (int[] size : dimensions) sizes.add(new SizeF(size[0], size[1]));
      options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes);
      RemoteViews restored = parcel(PrayerWidgetAdaptive.responsive(context, schedule, NOW, options));
      for (int[] size : dimensions) {
        direct(schedule, NOW, size[0], size[1]);
        List<String> direct = geometrySignature();
        apply(select(restored, size[0], size[1]), size[0], size[1]);
        assertEquals("Framework selector must use exact launcher geometry " + Arrays.toString(size),
            direct, geometrySignature());
        assertRows(schedule, NOW, arabic);
        assertBoundsAndUniformText("parcel exact " + Arrays.toString(size));
      }
      assertEquals("Provider must not mutate launcher size options", sizes,
          options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES));
    }
  }

  @Test public void sixteenExactSizesShareBoundedBitmapPayloadAndKeepFractionalKeys() throws Exception {
    Bundle options = options(140, 80);
    ArrayList<SizeF> sizes = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      sizes.add(new SizeF(140.5f + i * 7, 160.5f + i * 11));
      sizes.add(new SizeF(300.5f + i * 9, 80.5f + i * 3));
    }
    options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes);
    RemoteViews restored = parcel(PrayerWidgetAdaptive.responsive(context, schedule, NOW, options));
    Method memory = RemoteViews.class.getDeclaredMethod("estimateMemoryUsage");
    memory.setAccessible(true);
    int bytes = ((Number) memory.invoke(restored)).intValue();
    // AppWidgetService permits 1.5 full-screen ARGB bitmaps. Even a modest 480x800 launcher
    // must be able to accept every orientation/fold variant in one RemoteViews transaction.
    assertTrue("Sixteen sizes must not duplicate bitmap payload: " + bytes,
        bytes <= 480 * 800 * 4 * 3 / 2);
    Field ideal = RemoteViews.class.getDeclaredField("mIdealSize");
    ideal.setAccessible(true);
    for (SizeF size : sizes) {
      RemoteViews selected = select(restored, size.getWidth(), size.getHeight());
      assertEquals("Fractional launcher key must survive parcel exactly", size, ideal.get(selected));
      int w = (int) size.getWidth(), h = (int) size.getHeight();
      direct(schedule, NOW, w, h);
      List<String> expected = geometrySignature();
      apply(selected, w, h);
      assertEquals("Fractional option uses conservative pixel fit", expected, geometrySignature());
      assertBoundsAndUniformText("fractional size " + size);
    }
    System.out.println("PASS sixteen-size adaptive RemoteViews bitmap memory " + bytes + " bytes");
  }

  @Test public void arabicFallbackGlyphsFitAtIntermediateAccessibilityScale() throws Exception {
    for (boolean eastern : new boolean[] {false, true}) {
      configure(true, eastern, true, 1.3f, "large", false);
      for (int[] size : new int[][] {{140,80},{140,160},{250,125},{256,128},{177,213}}) {
        responsive(schedule, NOW, size[0], size[1]);
        assertRows(schedule, NOW, true);
        assertChromeAndPhase(schedule, NOW, true);
        assertBoundsAndUniformText("Arabic fallback font1.3 " + Arrays.toString(size));
      }
    }
  }

  @Test public void absentEmptyAndOversizedHostOptionsKeepConservativeFallback() throws Exception {
    for (boolean arabic : new boolean[] {false, true}) {
      configure(arabic, true, true, 1.5f, "normal", false);
      List<Bundle> inputs = new ArrayList<>();
      inputs.add(null);
      inputs.add(new Bundle());
      Bundle empty = options(140, 160);
      empty.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, new ArrayList<SizeF>());
      inputs.add(empty);
      Bundle excessive = options(140, 160);
      ArrayList<SizeF> tooMany = new ArrayList<>();
      for (int i = 0; i < 17; i++) tooMany.add(new SizeF(140 + i, 160 + i));
      excessive.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, tooMany);
      inputs.add(excessive);
      Bundle invalid = options(140, 160);
      invalid.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,
          new ArrayList<>(Arrays.asList(null, new SizeF(0, 0), new SizeF(-1, 160), new SizeF(5000, 5000))));
      inputs.add(invalid);
      direct(schedule, NOW, 140, 160);
      List<String> expected = geometrySignature();
      for (Bundle input : inputs) {
        RemoteViews responsive = parcel(PrayerWidgetAdaptive.responsive(context, schedule, NOW, input));
        apply(select(responsive, 140, 160), 140, 160);
        assertEquals("Missing/malformed size lists fall back to legacy/default dimensions", expected, geometrySignature());
        assertRows(schedule, NOW, arabic);
        assertChromeAndPhase(schedule, NOW, arabic);
        assertBoundsAndUniformText("launcher options fallback");
      }
    }
  }

  @Test public void physicalPixelBoundsStayValidAtHighLauncherDensities() throws Exception {
    for (String density : new String[] {"hdpi", "xhdpi", "xxxhdpi"})
      for (boolean arabic : new boolean[] {false, true}) {
        configure(arabic, true, true, 2f, "large", false);
        RuntimeEnvironment.setQualifiers((arabic ? "ar" : "en")
            + "-rUS-w800dp-h1000dp-port-" + density);
        RuntimeEnvironment.setFontScale(2f);
        Locale.setDefault(Locale.US);
        host.removeAllViews();
        controller.pause().stop().destroy();
        controller = Robolectric.buildActivity(Activity.class).setup().visible();
        host = new FrameLayout(controller.get());
        controller.get().setContentView(host);
        float scale = context.getResources().getDisplayMetrics().density;
        assertTrue("High-density test must use physical pixels", scale > 1f);
        for (int[] dp : new int[][] {{140,96},{140,160},{400,128},{320,500}}) {
          int pixelsWide = Math.round(dp[0] * scale), pixelsHigh = Math.round(dp[1] * scale);
          RemoteViews views = select(parcel(PrayerWidgetAdaptive.responsive(context, schedule, NOW,
              options(dp[0], dp[1]))), dp[0], dp[1]);
          apply(views, pixelsWide, pixelsHigh);
          assertRows(schedule, NOW, arabic);
          assertBoundsAndUniformText(density + " " + Arrays.toString(dp) + "dp");
          for (View view : all(root)) if (visible(view) && view instanceof TextView)
            assertTrue("10dp floor must scale to launcher density", ((TextView) view).getTextSize() >= 9.99f * scale);
          // Phase thresholds are specified in dp, while native bounds above are physical pixels.
          width = dp[0];
          height = dp[1];
          assertChromeAndPhase(schedule, NOW, arabic);
          width = pixelsWide;
          height = pixelsHigh;
        }
      }
    System.out.println("PASS adaptive hdpi/xhdpi/xxxhdpi physical-pixel bounds in both languages");
  }

  @Test @Config(sdk = {26, 29, 35})
  public void portraitLandscapeOptionsUseBothWidthsAndPreservePreferences() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (float scale : new float[] {.85f, 1f, 1.5f, 2f}) {
        configure(arabic, true, true, scale, "large", false);
        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 400);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 240);
        if (Build.VERSION.SDK_INT >= 31) {
          options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,
              new ArrayList<>(Arrays.asList(new SizeF(140, 240), new SizeF(400, 80))));
        }
        RemoteViews restored = parcel(PrayerWidgetAdaptive.responsive(context, schedule, NOW, options));
        for (boolean landscape : new boolean[] {false, true, false}) {
          qualifiers(arabic, landscape);
          RuntimeEnvironment.setFontScale(scale);
          int w = landscape ? 400 : 140, h = landscape ? 80 : 240;
          direct(schedule, NOW, w, h);
          List<String> expected = geometrySignature();
          apply(select(restored, w, h), w, h);
          assertEquals("Both legacy width/height pairs must be used on API" + Build.VERSION.SDK_INT,
              expected, geometrySignature());
          assertRows(schedule, NOW, arabic);
          assertChromeAndPhase(schedule, NOW, arabic);
          assertBoundsAndUniformText("API" + Build.VERSION.SDK_INT + " " + w + "x" + h + " font" + scale);
          select(restored, w, h).reapply(context, root);
          layout();
          assertEquals("Reapply preserves exact orientation geometry", expected, geometrySignature());
          assertTrue(AppPreferences.prefs(context).getBoolean("eastern_digits", false));
          assertEquals("large", AppPreferences.prefs(context).getString("widget_text", ""));
        }
      }
    System.out.println("PASS adaptive portrait/landscape/portrait parcel+reapply API" + Build.VERSION.SDK_INT);
  }

  @Test public void everyAdhanIqamahBoundaryAndMidnightUpdatesWithoutStaleRows() throws Exception {
    LocalDate date = NOW.atZone(Schedule.ZONE).toLocalDate();
    for (boolean arabic : new boolean[] {false, true}) {
      configure(arabic, false, true, 2f, "large", false);
      direct(schedule, NOW, 180, 240);
      for (int index : Schedule.PRAYERS) {
        Instant adhan = ZonedDateTime.of(date, schedule.days.get(date)[index], Schedule.ZONE).toInstant();
        Instant iqamah = adhan.plusSeconds(AppPreferences.iqamahMinutes(context)[index] * 60L);
        for (Instant now : new Instant[] {adhan.minusSeconds(1), adhan, adhan.plusSeconds(1),
            iqamah.minusSeconds(1), iqamah, iqamah.plusSeconds(1)}) {
          reapply(schedule, now);
          assertRows(schedule, now, arabic);
          assertChromeAndPhase(schedule, now, arabic);
          assertBoundsAndUniformText("phase " + now + " ar" + arabic);
        }
      }
      Instant midnight = date.plusDays(1).atStartOfDay(Schedule.ZONE).toInstant();
      for (Instant now : new Instant[] {midnight.minusSeconds(1), midnight, midnight.plusSeconds(1)}) {
        reapply(schedule, now);
        assertRows(schedule, now, arabic);
        assertChromeAndPhase(schedule, now, arabic);
        assertBoundsAndUniformText("UAE midnight " + now);
      }
      AppPreferences.prefs(context).edit().putBoolean("iqamah_enabled", false).commit();
      Instant adhan = ZonedDateTime.of(date, schedule.days.get(date)[0], Schedule.ZONE).toInstant();
      reapply(schedule, adhan);
      assertFalse(AppPreferences.phase(context, schedule, adhan).iqamah);
      assertRows(schedule, adhan, arabic);
      assertChromeAndPhase(schedule, adhan, arabic);
    }
  }

  @Test public void missingFutureExpiredAndRecoveredDataNeverRetainTimesOrTimer() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (int[] size : new int[][] {{140,80},{140,160},{300,100},{400,200}}) {
        configure(arabic, true, true, 2f, "large", false);
        direct(schedule, NOW, size[0], size[1]);
        for (Instant now : new Instant[] {
            Instant.parse("2025-01-01T00:00:00Z"),
            schedule.days.firstKey().minusDays(1).atStartOfDay(Schedule.ZONE).toInstant(),
            schedule.days.lastKey().plusDays(1).atStartOfDay(Schedule.ZONE).toInstant()}) {
          reapply(schedule, now);
          assertRows(schedule, now, arabic);
          assertChromeAndPhase(schedule, now, arabic);
          assertBoundsAndUniformText("unavailable " + now + " " + Arrays.toString(size));
          assertNotNull("Missing-date message must remain accessible", root.getContentDescription());
          reapply(schedule, NOW);
          assertRows(schedule, NOW, arabic);
          assertChromeAndPhase(schedule, NOW, arabic);
        }
        reapply(null, NOW);
        assertRows(null, NOW, arabic);
        assertChromeAndPhase(null, NOW, arabic);
        assertBoundsAndUniformText("null timetable " + Arrays.toString(size));
        assertNotNull("Missing-data recovery route must be accessible", root.getContentDescription());
        reapply(schedule, NOW);
        assertRows(schedule, NOW, arabic);
        assertChromeAndPhase(schedule, NOW, arabic);
      }
  }

  @Test public void syntheticLateIshaAndFridayIqamahRemainCorrect() throws Exception {
    Schedule fixture = new Schedule(new StringReader("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n"
        + "2026-10-16,05:00,06:00,12:00,15:00,18:00,23:50\n"
        + "2026-10-17,05:01,06:01,12:01,15:01,18:01,23:51\n"));
    for (boolean arabic : new boolean[] {false, true}) {
      configure(arabic, true, false, 1.5f, "normal", false);
      direct(fixture, NOW, 180, 240);
      for (String time : new String[] {"2026-10-16T08:00:00Z", "2026-10-16T19:50:00Z",
          "2026-10-16T20:00:00Z", "2026-10-16T20:10:00Z", "2026-10-17T20:05:00Z",
          "2026-10-17T20:11:00Z"}) {
        Instant now = Instant.parse(time);
        reapply(fixture, now);
        assertRows(fixture, now, arabic);
        assertChromeAndPhase(fixture, now, arabic);
        assertBoundsAndUniformText("Friday/cross-midnight " + time);
      }
      PrayerPhase friday = AppPreferences.phase(context, fixture, Instant.parse("2026-10-16T08:00:00Z"));
      assertFalse("Friday Dhuhr must not synthesize estimated congregation time", friday.iqamah);
      PrayerPhase nextDay = AppPreferences.phase(context, fixture, Instant.parse("2026-10-16T20:00:00Z"));
      assertTrue("Pending Isha estimate survives UAE midnight", nextDay.iqamah);
      assertEquals(5, nextDay.index);
    }
  }

  @Test public void launcherNumeralMismatchAndSecondsPreferenceSwitchWithoutStaleTimer() throws Exception {
    for (String system : new String[] {"en-US", "ar-AE"})
      for (boolean arabic : new boolean[] {false, true})
        for (boolean eastern : new boolean[] {false, true}) {
          configure(arabic, eastern, true, 2f, "large", false);
          Locale.setDefault(Locale.forLanguageTag(system));
          direct(schedule, NOW, 180, 240);
          assertRows(schedule, NOW, arabic);
          assertChromeAndPhase(schedule, NOW, arabic);
          assertBoundsAndUniformText("system " + system + " arabic" + arabic + " eastern" + eastern);
          String nativeFormat = DateUtils.formatElapsedTime(12345L);
          boolean nativeMatches = nativeFormat.matches(eastern ? ".*[٠-٩].*" : ".*[0-9].*")
              && !nativeFormat.matches(eastern ? ".*[0-9۰-۹].*" : ".*[٠-٩۰-۹].*");
          assertEquals("Actual launcher formatter must honor requested digits before native timer is used",
              nativeMatches, visible(view("widget_iqamah_countdown")));
          AppPreferences.prefs(context).edit().putBoolean("countdown_seconds", false).commit();
          reapply(schedule, NOW);
          assertChromeAndPhase(schedule, NOW, arabic);
          String before = text("widget_countdown_minutes").getText().toString();
          reapply(schedule, NOW.plusSeconds(61));
          assertChromeAndPhase(schedule, NOW.plusSeconds(61), arabic);
          assertNotEquals("Minute snapshot advances on refresh", before,
              text("widget_countdown_minutes").getText().toString());
          AppPreferences.prefs(context).edit().putBoolean("countdown_seconds", true)
              .putBoolean("eastern_digits", nativeFormat.matches(".*[٠-٩].*")).commit();
          reapply(schedule, NOW);
          assertChromeAndPhase(schedule, NOW, arabic);
          Chronometer timer = (Chronometer) view("widget_iqamah_countdown");
          String timerBefore = timer.getText().toString();
          long target = timer.getBase();
          Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3));
          layout();
          assertNotEquals("Native timer ticks without reapplying RemoteViews", timerBefore, timer.getText().toString());
          assertEquals("Native tick retains target", target, timer.getBase());
        }
  }

  @Test public void opacityThemesAndTextPreferencesStayUniformAndPersistent() throws Exception {
    for (boolean arabic : new boolean[] {false, true})
      for (String mode : new String[] {"light", "dark"})
        for (String accent : new String[] {"green", "navy", "sand"})
          for (int opacity : new int[] {0, 65, 100}) {
            configure(arabic, true, true, 2f, "large", false);
            AppPreferences.prefs(context).edit().putString("widget_mode", mode)
                .putString("widget_accent", accent).putInt("widget_opacity", opacity).commit();
            Map<String, ?> original = new HashMap<String, Object>(AppPreferences.prefs(context).getAll());
            for (int[] size : new int[][] {{140,80},{180,240},{400,200}}) {
              responsive(schedule, NOW, size[0], size[1]);
              assertRows(schedule, NOW, arabic);
              assertBoundsAndUniformText(mode + " " + accent + " opacity" + opacity);
              reapply(schedule, NOW.plusSeconds(61));
              assertEquals("Resizing/reapplying preserves saved settings", original, AppPreferences.prefs(context).getAll());
            }
          }
  }

  @Test public void nativePreviewImagesCoverMinimumIntermediateWideAndLargeShapes() throws Exception {
    for (boolean arabic : new boolean[] {false, true}) {
      configure(arabic, arabic, true, 1f, "normal", false);
      for (int[] size : new int[][] {{140,80},{140,96},{140,160},{180,240},{300,100},{400,200},{600,600}}) {
        responsive(schedule, NOW, size[0], size[1]);
        assertRows(schedule, NOW, arabic);
        assertBoundsAndUniformText("preview " + Arrays.toString(size));
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(image));
        int painted = 0;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
          if (Color.alpha(image.getPixel(x, y)) != 0) painted++;
        assertTrue("Native renderer must paint content", painted > width * height / 20);
        File folder = new File(System.getProperty("qa.output", "screenshots"), "adaptive-widgets");
        assertTrue("Create preview directory", folder.isDirectory() || folder.mkdirs());
        File file = new File(folder, "adaptive-" + (arabic ? "ar" : "en") + "-" + width + "x" + height
            + (height < 96 ? "-below-minimum-defensive" : "") + ".png");
        try (FileOutputStream output = new FileOutputStream(file)) {
          assertTrue("Native PNG encode", image.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        image.recycle();
        System.out.println("Adaptive native preview: " + file.getAbsolutePath());
      }
    }
  }

  @Test @Config(sdk = {26, 29, 35})
  public void providerManifestGlobalRefreshAndResizeLifecycleAreIntegrated() throws Exception {
    AppWidgetManager manager = AppWidgetManager.getInstance(context);
    ActivityInfo receiver = context.getPackageManager().getReceiverInfo(
        new ComponentName(context, PrayerWidgetAdaptive.class), PackageManager.GET_META_DATA);
    assertNotNull("Adaptive provider manifest metadata", receiver.metaData);
    int resource = receiver.metaData.getInt(AppWidgetManager.META_DATA_APPWIDGET_PROVIDER);
    assertTrue("Adaptive provider metadata resource", resource != 0);
    try (XmlResourceParser xml = context.getResources().getXml(resource)) {
      while (xml.next() != XmlResourceParser.START_TAG) {}
      String ns = "http://schemas.android.com/apk/res/android";
      assertEquals("140.0dip", xml.getAttributeValue(ns, "minResizeWidth"));
      assertEquals("96.0dip", xml.getAttributeValue(ns, "minResizeHeight"));
      assertEquals("Both axes must resize", 3, xml.getAttributeIntValue(ns, "resizeMode", 0));
    }
    int initial = PrayerWidgetAdaptive.render(context, schedule, NOW, 140, 160).getLayoutId();
    int id = Shadows.shadowOf(manager).createWidget(PrayerWidgetAdaptive.class, initial);
    assertTrue("Adaptive-only widget must count as installed", PrayerWidget.hasWidgets(context));
    AppWidgetProvider provider = Shadows.shadowOf(manager).getAppWidgetProviderFor(id);
    provider.onEnabled(context);
    for (int[] size : new int[][] {{140,80},{180,240},{400,80},{177,213},{600,600},{140,160}}) {
      Bundle options = options(size[0], size[1]);
      manager.updateAppWidgetOptions(id, options);
      provider.onAppWidgetOptionsChanged(context, manager, id, options);
      provider.onUpdate(context, manager, new int[] {id});
      PrayerWidget.updateAll(context);
      View updated = Shadows.shadowOf(manager).getViewFor(id);
      assertNotNull("Global update retains adaptive instance", updated);
      for (String key : KEYS) {
        int timeId = context.getResources().getIdentifier("widget_" + key + "_time", "id", context.getPackageName());
        assertTrue("Global refresh populates " + key,
            ((TextView) updated.findViewById(timeId)).getText().length() > 0);
      }
    }
    provider.onReceive(context, new Intent(PrayerWidget.ACTION_REFRESH));
    provider.onReceive(context, new Intent(PrayerWidget.ACTION_MINUTE));
    provider.onDeleted(context, new int[] {id});
    provider.onDisabled(context);
  }

  private List<ShadowAlarmManager.ScheduledAlarm> minuteAlarms(ShadowAlarmManager manager) {
    List<ShadowAlarmManager.ScheduledAlarm> result = new ArrayList<>();
    for (ShadowAlarmManager.ScheduledAlarm alarm : manager.getScheduledAlarms())
      if (alarm.operation != null && PrayerWidget.ACTION_MINUTE.equals(
          Shadows.shadowOf(alarm.operation).getSavedIntent().getAction())) result.add(alarm);
    return result;
  }

  @Test public void adaptiveOnlyMinuteFallbackIsNonWakeupAndPreservesPrayerAlarms() {
    AppWidgetManager manager = AppWidgetManager.getInstance(context);
    AppWidgetProviderInfo info = new AppWidgetProviderInfo();
    info.provider = new ComponentName(context, PrayerWidgetAdaptive.class);
    Shadows.shadowOf(manager).addBoundWidget(901, info);
    AlarmManager alarms = context.getSystemService(AlarmManager.class);
    ShadowAlarmManager shadow = Shadows.shadowOf(alarms);
    List<PendingIntent> prayerAlarms = new ArrayList<>();
    for (String action : new String[] {AlertScheduler.ACTION, AlertScheduler.REMINDER_ACTION,
        AlertScheduler.IQAMAH_WIDGET_ACTION}) {
      PendingIntent sentinel = PendingIntent.getBroadcast(context, 1200 + prayerAlarms.size(),
          new Intent(context, AlarmReceiver.class).setAction(action), PendingIntent.FLAG_IMMUTABLE);
      prayerAlarms.add(sentinel);
      alarms.set(AlarmManager.RTC_WAKEUP, NOW.plusSeconds(3600).toEpochMilli(), sentinel);
    }
    AppPreferences.prefs(context).edit().putBoolean("eastern_digits", true).commit();
    for (int second : new int[] {0, 1, 20}) {
      PrayerWidget.scheduleMinute(context, NOW.plusSeconds(second));
      assertEquals("One replaceable minute refresh for adaptive-only installation", 1, minuteAlarms(shadow).size());
      ShadowAlarmManager.ScheduledAlarm refresh = minuteAlarms(shadow).get(0);
      assertEquals(AlarmManager.RTC, refresh.getType());
      assertFalse(refresh.isAllowWhileIdle());
      assertEquals(0, refresh.getIntervalMs());
      assertEquals((NOW.toEpochMilli() / 60000 + 1) * 60000, refresh.getTriggerAtMs());
    }
    AppPreferences.prefs(context).edit().putBoolean("eastern_digits", false).commit();
    PrayerWidget.scheduleMinute(context, NOW);
    assertTrue("Live numerals cancel snapshot work", minuteAlarms(shadow).isEmpty());
    AppPreferences.prefs(context).edit().putBoolean("countdown_seconds", false).commit();
    PrayerWidget.scheduleMinute(context, schedule.days.lastKey().plusDays(1).atStartOfDay(Schedule.ZONE).toInstant());
    assertTrue("Timetable expiry cancels snapshot work", minuteAlarms(shadow).isEmpty());
    for (PendingIntent sentinel : prayerAlarms) {
      boolean retained = false;
      for (ShadowAlarmManager.ScheduledAlarm alarm : shadow.getScheduledAlarms())
        if (sentinel.equals(alarm.operation)) retained = true;
      assertTrue("Adaptive snapshot maintenance must preserve prayer/reminder alarms", retained);
    }
  }

  @Implements(AppWidgetManager.class)
  public static class RejectingAdaptiveManager extends ShadowAppWidgetManager {
    static int rejected = 501;
    static boolean rejectOptions;
    static int providerQueries;
    static final List<Integer> updated = new ArrayList<>();
    @Implementation public int[] getAppWidgetIds(ComponentName provider) {
      providerQueries++;
      if (provider.getClassName().equals(PrayerWidgetAdaptive.class.getName())) return new int[] {501, 502};
      if (provider.getClassName().equals(PrayerWidget.class.getName())) return new int[] {101};
      return new int[0];
    }
    @Implementation public Bundle getAppWidgetOptions(int id) {
      if (id == rejected && rejectOptions) throw new IllegalArgumentException("Injected stale launcher options");
      return new Bundle();
    }
    @Implementation public void updateAppWidget(int id, RemoteViews views) {
      if (id == rejected) throw new IllegalStateException("Injected rejected adaptive update");
      updated.add(id);
    }
    @Implementation public void updateAppWidget(int[] ids, RemoteViews views) {
      for (int id : ids) updateAppWidget(id, views);
    }
  }

  @Test @Config(shadows = RejectingAdaptiveManager.class)
  public void rejectedAdaptiveInstanceDoesNotBlockOthersAndRecoversOnNextRefresh() {
    RejectingAdaptiveManager.rejected = 501;
    RejectingAdaptiveManager.rejectOptions = false;
    RejectingAdaptiveManager.updated.clear();
    PrayerWidget.updateAll(context);
    assertTrue(RejectingAdaptiveManager.updated.containsAll(Arrays.asList(101, 502)));
    assertFalse(RejectingAdaptiveManager.updated.contains(501));
    RejectingAdaptiveManager.rejectOptions = true;
    RejectingAdaptiveManager.updated.clear();
    PrayerWidget.updateAll(context);
    assertTrue("Stale options cannot block other providers/instances",
        RejectingAdaptiveManager.updated.containsAll(Arrays.asList(101, 502)));
    AppPreferences.prefs(context).edit().putBoolean("schedule_error", true).commit();
    new PrayerWidgetAdaptive().onUpdate(context, AppWidgetManager.getInstance(context), new int[] {501, 502});
    assertFalse("Scheduling must run despite rejected update", AppPreferences.prefs(context).contains("schedule_error"));
    RejectingAdaptiveManager.providerQueries = 0;
    AppPreferences.prefs(context).edit().putBoolean("countdown_seconds", false).commit();
    new PrayerWidgetAdaptive().onAppWidgetOptionsChanged(context, AppWidgetManager.getInstance(context), 501, new Bundle());
    assertTrue("Resize failure must still reconcile minute work", RejectingAdaptiveManager.providerQueries > 0);
    RejectingAdaptiveManager.rejected = -1;
    RejectingAdaptiveManager.updated.clear();
    PrayerWidget.updateAll(context);
    assertTrue("Next normal refresh recovers without reinstalling widgets",
        RejectingAdaptiveManager.updated.containsAll(Arrays.asList(101, 501, 502)));
  }
}
