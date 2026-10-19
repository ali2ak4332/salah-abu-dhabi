package ae.prayertimes.abudhabi;

import static org.junit.Assert.*;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
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
import android.text.Layout;
import android.util.SizeF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
 * Native Android regression tests for the two deliberate widget types.
 * Real RemoteViews are parceled, selected by the Android framework, inflated and measured.
 * Text layout assertions use physical pixels, including fallback-font line heights. These
 * host tests do not claim physical-device/OEM-launcher or notification-delivery coverage.
 */
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class TwoWidgetTest {
  private static final Instant NOW = Instant.parse("2026-10-17T08:00:00Z");
  private static final String[] KEYS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
  private static final String[] EN = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
  private static final String[] AR = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
  // Advertised minima, non-preset/intermediate sizes, ordinary larger launcher cells, and
  // intentionally unusual aspect ratios that must never change the chosen widget type.
  private static final int[][] COMPACT_SIZES = {
    {110,110}, {130,117}, {140,140}, {141,141}, {160,160}, {177,213}, {180,240}, {240,180},
    {300,140}, {140,300}, {400,400}, {600,140}, {140,600}, {600,600}
  };
  private static final int[][] STRIP_SIZES = {
    {250,40}, {276,51}, {300,55}, {260,56}, {261,57}, {300,64}, {360,80}, {399,99}, {400,128},
    {600,56}, {260,200}, {400,400}, {260,600}, {600,600}
  };
  private Context context;
  private Schedule schedule;
  private ActivityController<Activity> controller;
  private FrameLayout host;
  private View root;
  private int width, height, widthDp, heightDp, currentLayoutId;
  private boolean horizontal;
  private Locale originalLocale;

  @Before public void setUp() throws Exception {
    originalLocale = Locale.getDefault();
    configure(false, false, false, 1f, "normal", false);
    schedule = AlertScheduler.schedule(context);
    createHost();
  }

  @After public void tearDown() {
    if (host != null) host.removeAllViews();
    if (controller != null) controller.pause().stop().destroy();
    Locale.setDefault(originalLocale);
    RuntimeEnvironment.setFontScale(1f);
  }

  private void createHost() {
    if (host != null) host.removeAllViews();
    if (controller != null) controller.pause().stop().destroy();
    controller = Robolectric.buildActivity(Activity.class).setup().visible();
    host = new FrameLayout(controller.get());
    controller.get().setContentView(host);
  }

  private void configure(boolean arabic, boolean eastern, boolean twelve, float scale,
      String text, boolean landscape) {
    qualifiers(arabic, landscape, "mdpi");
    RuntimeEnvironment.setFontScale(scale);
    context = RuntimeEnvironment.getApplication();
    // A launcher/system locale may differ from both saved language and numeral settings.
    Locale.setDefault(Locale.US);
    AppPreferences.prefs(context).edit().clear()
        .putBoolean("arabic", arabic).putBoolean("eastern_digits", eastern)
        .putBoolean("twelve_hour", twelve).putBoolean("countdown_seconds", true)
        .putString("widget_text", text).putInt("widget_opacity", 100)
        .putString("widget_mode", "dark").putString("widget_accent", "green").commit();
  }

  private void qualifiers(boolean arabic, boolean landscape, String density) {
    RuntimeEnvironment.setQualifiers((arabic ? "ar" : "en")
        + (landscape ? "-rUS-w1000dp-h800dp-land-" : "-rUS-w800dp-h1000dp-port-") + density);
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

  private void responsive(boolean strip, Schedule data, Instant now, int w, int h) throws Exception {
    horizontal = strip;
    widthDp = w;
    heightDp = h;
    apply(select(parcel(PrayerWidget.responsive(context, strip, data, now, options(w, h))), w, h),
        px(w), px(h));
  }

  private void direct(boolean strip, Schedule data, Instant now, int w, int h) {
    horizontal = strip;
    widthDp = w;
    heightDp = h;
    apply(PrayerWidget.render(context, strip, data, now, w, h), px(w), px(h));
  }

  private int px(int dp) { return Math.round(dp * context.getResources().getDisplayMetrics().density); }

  private void apply(RemoteViews views, int w, int h) {
    width = w;
    height = h;
    host.removeAllViews();
    currentLayoutId = views.getLayoutId();
    root = views.apply(context, host);
    host.addView(root, new FrameLayout.LayoutParams(w, h));
    layout();
  }

  private void reapply(Schedule data, Instant now) {
    RemoteViews next = PrayerWidget.render(context, horizontal, data, now, widthDp, heightDp);
    // AppWidgetHost reinflates when a language-specific layout ID changes. Reapply is
    // only legal for the same resource; recreate here to mirror that host behavior.
    if (next.getLayoutId() != currentLayoutId) apply(next, width, height);
    else { next.reapply(context, root); layout(); }
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
    return id == 0 ? null : root.findViewById(id);
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
    // offsetDescendantRectToMyCoords subtracts a TextView's own horizontal scroll,
    // reporting content-space rather than view-space for right-aligned single lines.
    int[] viewLocation = new int[2], hostLocation = new int[2];
    view.getLocationInWindow(viewLocation);
    host.getLocationInWindow(hostLocation);
    int left = viewLocation[0] - hostLocation[0], top = viewLocation[1] - hostLocation[1];
    return new Rect(left, top, left + view.getWidth(), top + view.getHeight());
  }

  private void assertBoundsAndUniformText(String profile) {
    Float common = null;
    Typeface commonTypeface = null, nameTypeface = null;
    int prayerTexts = 0;
    for (View view : all(root)) {
      if (!visible(view) || !(view instanceof TextView)) continue;
      TextView text = (TextView) view;
      if (text.getText().length() == 0) continue;
      String label = profile + " " + name(text) + " [" + text.getText() + "]";
      boolean prayer = false;
      for (String key : KEYS) if (name(text).equals("widget_" + key + "_name")
          || name(text).equals("widget_" + key + "_time")) prayer = true;
      if (prayer) {
        prayerTexts++;
        boolean isName = name(text).endsWith("_name");
        float timeSize=text("widget_fajr_time").getTextSize();
        float expected=horizontal&&isName?Math.max(9f*context.getResources().getDisplayMetrics().density,
            timeSize*PrayerWidget.STRIP_NAME_SCALE):timeSize;
        assertEquals(label + " shares the fitted size, with a readable floor for smaller strip names",expected,text.getTextSize(),.05f);
        // v2.5 hierarchy: times share the medium face; names share the regular face.
        if (isName) { if (nameTypeface == null) nameTypeface = text.getTypeface();
          assertEquals(label + " names share one face", nameTypeface, text.getTypeface()); }
        else { if (commonTypeface == null) commonTypeface = text.getTypeface();
          assertEquals(label + " times share one face", commonTypeface, text.getTypeface()); }
      }
      assertTrue(label + " has a nonpositive font size", text.getTextSize() > 0);
      if (prayer) assertTrue(label + " is below the 9dp reading floor at a supported size",
          text.getTextSize() >= 8.99f * context.getResources().getDisplayMetrics().density);
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
      assertEquals(label + " must remain one line", 1, lines.getLineCount());
      for (int line = 0; line < lines.getLineCount(); line++) {
        assertEquals(label + " ellipsis", 0, lines.getEllipsisCount(line));
        assertTrue(label + " text width " + lines.getLineWidth(line) + " > " + availableWidth,
            lines.getLineWidth(line) <= availableWidth + 1);
        assertTrue(label + " native fallback line exceeds vertical space",
            lines.getLineBottom(line) - lines.getLineTop(line) <= availableHeight + 1);
        float left = text.getCompoundPaddingLeft() + lines.getLineLeft(line) - text.getScrollX();
        float right = text.getCompoundPaddingLeft() + lines.getLineRight(line) - text.getScrollX();
        assertTrue(label + " actual scrolled text starts outside its visible area: " + left,
            left >= text.getCompoundPaddingLeft() - 1);
        assertTrue(label + " actual scrolled text ends outside its visible area: " + right,
            right <= text.getWidth() - text.getCompoundPaddingRight() + 1);
      }
      assertTrue(label + " loses the end of its text",
          lines.getLineCount() > 0 && lines.getLineEnd(lines.getLineCount() - 1) == text.length());
    }
    assertEquals(profile + " must display all ten prayer name/time TextViews", 10, prayerTexts);
  }

  private void assertRows(Schedule data, Instant now, boolean arabic) {
    LocalDate today = now.atZone(Schedule.ZONE).toLocalDate();
    LocalTime[] saved = data == null ? null : data.days.get(today);
    Schedule.Next next = data == null ? null : data.next(now);
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
      assertEquals("Time alignment matches its language-specific layout", horizontal ? Gravity.CENTER_HORIZONTAL
          : arabic ? Gravity.LEFT : Gravity.RIGHT, prayerTime.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK);
      assertNotNull("Accessible prayer row " + key, row.getContentDescription());
      assertTrue(row.getContentDescription().toString().contains(expected));
      assertTrue(row.getContentDescription().toString().contains((arabic ? AR : EN)[i]));
      // v2.5: the called prayer stays lit during its estimated-iqamah window; otherwise the
      // next adhan today is lit. Nothing is lit when today's times are unavailable.
      PrayerPhase phase = data == null ? null : PrayerWidget.iqamahPhase(context, data, now);
      int lit = saved == null ? -1 : phase != null && phase.adhanAt.toLocalDate().equals(today) ? phase.index
          : next != null && next.at.toLocalDate().equals(today) ? next.index : -1;
      boolean active = lit == Schedule.PRAYERS[i];
      assertEquals("Exactly the lit prayer carries the rounded highlight " + key, active, row.getBackground() != null);
      if (lit >= 0 && Schedule.PRAYERS[i] < lit)
        assertTrue("Earlier prayers fade back " + key, Color.alpha(prayerTime.getCurrentTextColor()) < 255);
      Rect current = rect(row), n = rect(prayerName), t = rect(prayerTime);
      assertTrue("Prayer row outside host " + key + " " + current,
          current.left >= 0 && current.top >= 0 && current.right <= width && current.bottom <= height);
      assertFalse("Prayer name and time overlap " + key, Rect.intersects(n, t));
      if (horizontal) {
        assertTrue("Strip keeps name above time " + key, n.bottom <= t.top);
      } else {
        assertTrue("Compact keeps name/time in one row " + key,
            n.top < t.bottom && t.top < n.bottom);
        assertTrue("Compact language reading order " + key + " ar=" + arabic + " " + widthDp + "x" + heightDp + " name=" + n + " time=" + t, arabic ? t.right <= n.left : n.right <= t.left);
      }
      if (prior != null) {
        if (horizontal) {
          assertEquals("Strip columns align vertically", prior.top, current.top);
          assertTrue("Strip columns overlap or have wrong reading order",
              arabic ? current.right <= prior.left : prior.right <= current.left);
        } else assertTrue("Compact prayer rows overlap or reorder", prior.bottom <= current.top);
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

  private void assertNoTimerOrControls() {
    for (View view : all(root)) {
      // v2.5: Compact's header may hold one live countdown; Horizontal never has a timer.
      if (view instanceof Chronometer) assertEquals("Only Compact's header countdown is a timer",
          "widget_countdown", name(view));
      assertFalse("Control must not exist in either new widget: " + name(view),
          view instanceof ImageButton || view instanceof Button);
    }
    for (String removed : new String[] {"widget_iqamah_countdown", "widget_countdown_minutes",
        "widget_countdown_hint", "widget_refresh", "widget_settings", "widget_next_name", "widget_next_time"})
      assertNull("Obsolete chrome must not occupy either new layout: " + removed, view(removed));
    assertNotNull("Header container is addressable for space-aware visibility", view("widget_header"));
    assertNotNull("Date TextView is addressable for space-aware visibility", view("widget_meta"));
    if (visible(view("widget_meta"))) {
      assertTrue("A visible date belongs to the visible header", visible(view("widget_header")));
      assertTrue("A visible date may not be blank", text("widget_meta").length() > 0);
      assertNumerals(text("widget_meta").getText().toString(), AppPreferences.eastern(context));
    }
  }

  private void assertProfile(Schedule data, Instant now, boolean arabic, String label) {
    assertRows(data, now, arabic);
    assertNoTimerOrControls();
    assertEquals("Date is the current UAE day in the selected language/numerals",
        AppPreferences.digits(context, now.atZone(Schedule.ZONE).format(DateTimeFormatter.ofPattern("d MMM",
            arabic ? new Locale("ar") : Locale.ENGLISH))), text("widget_meta").getText().toString());
    assertBoundsAndUniformText(label);
  }

  @Test public void completeNativeSizeLanguageNumeralTimeAndFontMatrix() throws Exception {
    int passed = 0;
    List<String> failures = new ArrayList<>();
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true})
        for (boolean eastern : new boolean[] {false, true})
          for (boolean twelve : new boolean[] {false, true})
            for (float scale : new float[] {.85f, 1f, 1.3f, 1.5f, 2f})
              for (String textSize : new String[] {"small", "normal", "large"}) {
                configure(arabic, eastern, twelve, scale, textSize, false);
                Map<String, ?> prefs = new HashMap<String, Object>(AppPreferences.prefs(context).getAll());
                for (int[] size : strip ? STRIP_SIZES : COMPACT_SIZES) {
                  String label = (strip ? "strip" : "compact") + (arabic ? "-ar" : "-en")
                      + "-eastern" + eastern + "-12h" + twelve + "-font" + scale + "-" + textSize
                      + "-" + size[0] + "x" + size[1];
                  try {
                    responsive(strip, schedule, NOW, size[0], size[1]);
                    assertProfile(schedule, NOW, arabic, label);
                    assertEquals("Rendering may not alter saved settings", prefs, AppPreferences.prefs(context).getAll());
                    passed++;
                  } catch (AssertionError | RuntimeException e) {
                    if (failures.size() < 40) {
                      failures.add(label + ": " + e.getMessage());
                      if (failures.size() <= 5) System.out.println("Two-widget profile failure: " + label + ": " + e.getMessage());
                    }
                  }
                }
              }
    int expected = 2 * 2 * 2 * 5 * 3 * (COMPACT_SIZES.length + STRIP_SIZES.length);
    System.out.println("Two-widget native matrix: " + passed + "/" + expected + " profiles passed");
    assertTrue("Two-widget matrix failures: " + failures, failures.isEmpty());
    assertEquals(expected, passed);
  }

  @Test public void tightLegacyCellSizesKeepAllTextInsideNativeBounds() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (int[] dimensions : strip ? new int[][] {{250,40},{276,51}} : new int[][] {{110,110},{130,117}}) {
        float minimumFont = Float.MAX_VALUE;
        String smallestProfile = "";
        for (boolean arabic : new boolean[] {false, true})
          for (boolean eastern : new boolean[] {false, true})
            for (boolean twelve : new boolean[] {false, true})
              for (float scale : new float[] {.85f,1f,1.3f,2f})
                for (String pref : new String[] {"small","normal","large"}) {
                  configure(arabic, eastern, twelve, scale, pref, false);
                  responsive(strip, schedule, NOW, dimensions[0], dimensions[1]);
                  String profile = (strip ? "strip" : "compact") + " " + Arrays.toString(dimensions)
                      + " ar=" + arabic + " east=" + eastern + " 12h=" + twelve + " font=" + scale + " " + pref;
                  assertProfile(schedule, NOW, arabic, profile);
                  float font = text("widget_fajr_name").getTextSize();
                  if (font < minimumFont) { minimumFont = font; smallestProfile = profile; }
                }
        System.out.println("TIGHT MINIMUM " + (strip ? "strip" : "compact") + " " + Arrays.toString(dimensions)
            + " smallest=" + minimumFont + "dp profile=" + smallestProfile);
      }
  }

  private List<String> geometrySignature() {
    List<String> result = new ArrayList<>();
    for (View view : all(root)) if (visible(view) && view.getId() != View.NO_ID) {
      String value = name(view) + ":" + rect(view) + ":" + view.getLayoutDirection();
      if (view instanceof TextView) {
        TextView text = (TextView) view;
        // Live seconds legitimately change while separate parcel variants are inflated.
        // Compare its stable formatting/countdown mode here; target bases and ticking are
        // tested separately. Every static label/time still compares exact visible text.
        String content=text instanceof Chronometer?((Chronometer)text).getFormat()+":"+((Chronometer)text).isCountDown():text.getText().toString();
        value += ":" + text.getTextSize() + ":" + text.getCurrentTextColor() + ":" + content;
      }
      result.add(value);
    }
    return result;
  }

  @Test public void exactSizesSurviveParcelAndCannotSwitchWidgetType() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true}) {
        configure(arabic, true, true, 2f, "large", false);
        int[][] dimensions = strip ? STRIP_SIZES : COMPACT_SIZES;
        Bundle options = options(dimensions[0][0], dimensions[0][1]);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, dimensions[0][0] + 1);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, dimensions[0][1] + 1);
        ArrayList<SizeF> sizes = new ArrayList<>();
        for (int[] size : dimensions) sizes.add(new SizeF(size[0], size[1]));
        options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes);
        RemoteViews restored = parcel(PrayerWidget.responsive(context, strip, schedule, NOW, options));
        for (int[] size : dimensions) {
          direct(strip, schedule, NOW, size[0], size[1]);
          List<String> direct = geometrySignature();
          RemoteViews selected = select(restored, size[0], size[1]);
          int expectedLayout = context.getResources().getIdentifier(strip ? "widget_strip" : arabic ? "widget_compact_ar" : "widget_compact",
              "layout", context.getPackageName());
          assertEquals("Chosen widget type stays fixed at " + Arrays.toString(size), expectedLayout, selected.getLayoutId());
          apply(selected, px(size[0]), px(size[1]));
          assertEquals("Exact launcher sizes override contradictory legacy geometry " + Arrays.toString(size),
              direct, geometrySignature());
          assertProfile(schedule, NOW, arabic, "exact parcel " + Arrays.toString(size));
          selected.reapply(context, root);
          layout();
          assertEquals("Parcel/reapply retains exact dimensions", direct, geometrySignature());
        }
        assertEquals("Provider does not mutate launcher exact sizes", sizes,
            options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES));
      }
  }

  @Test public void sixteenFractionalSizesShareBoundedBitmapPayload() throws Exception {
    for (boolean strip : new boolean[] {false, true}) {
      int minW = strip ? 260 : 140, minH = strip ? 56 : 140;
      Bundle options = options(minW, minH);
      ArrayList<SizeF> sizes = new ArrayList<>();
      for (int i = 0; i < 16; i++) sizes.add(new SizeF(minW + .5f + i * 9, minH + .5f + i * 7));
      options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes);
      RemoteViews restored = parcel(PrayerWidget.responsive(context, strip, schedule, NOW, options));
      Method memory = RemoteViews.class.getDeclaredMethod("estimateMemoryUsage");
      memory.setAccessible(true);
      int bytes = ((Number) memory.invoke(restored)).intValue();
      assertEquals("Drawable backgrounds add no bitmap payload, including sixteen variants", 0, bytes);
      Field ideal = RemoteViews.class.getDeclaredField("mIdealSize");
      ideal.setAccessible(true);
      for (SizeF size : sizes) {
        RemoteViews selected = select(restored, size.getWidth(), size.getHeight());
        assertEquals("Fractional key survives parcel exactly", size, ideal.get(selected));
        int w = (int) size.getWidth(), h = (int) size.getHeight();
        direct(strip, schedule, NOW, w, h);
        List<String> expected = geometrySignature();
        apply(selected, px(w), px(h));
        assertEquals("Fractional geometry fits conservatively", expected, geometrySignature());
        assertProfile(schedule, NOW, false, "fractional " + size);
      }
      System.out.println("PASS " + (strip ? "strip" : "compact") + " sixteen-size bitmap payload " + bytes + " bytes");
    }
  }

  @Test public void physicalPixelAndFallbackFontBoundsAtLauncherDensities() throws Exception {
    for (String density : new String[] {"hdpi", "xhdpi", "xxhdpi", "xxxhdpi"})
      for (boolean strip : new boolean[] {false, true})
        for (boolean arabic : new boolean[] {false, true})
          for (boolean eastern : new boolean[] {false, true}) {
            configure(arabic, eastern, true, 2f, "large", false);
            qualifiers(arabic, false, density);
            RuntimeEnvironment.setFontScale(2f);
            Locale.setDefault(Locale.US);
            createHost();
            float scale = context.getResources().getDisplayMetrics().density;
            assertTrue("Density test must measure physical pixels", scale > 1f);
            for (int[] dp : strip ? new int[][] {{250,40},{276,51},{260,56},{300,64},{400,128},{260,300}}
                : new int[][] {{110,110},{130,117},{140,140},{177,213},{300,140},{320,500}}) {
              responsive(strip, schedule, NOW, dp[0], dp[1]);
              assertEquals(Math.round(dp[0] * scale), width);
              assertProfile(schedule, NOW, arabic, density + " " + Arrays.toString(dp) + "dp");
            }
          }
    System.out.println("PASS both widgets at hdpi/xhdpi/xxhdpi/xxxhdpi with native pixel bounds");
  }

  @Test @Config(sdk = {26, 29, 35})
  public void portraitLandscapeOptionsKeepBothTypesAndAllPreferences() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true})
        for (float scale : new float[] {1f, 1.3f, 2f}) {
          configure(arabic, true, true, scale, "large", false);
          int portraitW = strip ? 260 : 140, portraitH = strip ? 128 : 240;
          int landscapeW = strip ? 600 : 400, landscapeH = strip ? 56 : 140;
          Bundle options = new Bundle();
          options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, portraitW);
          options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, landscapeW);
          options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, landscapeH);
          options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, portraitH);
          if (Build.VERSION.SDK_INT >= 31) options.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,
              new ArrayList<>(Arrays.asList(new SizeF(portraitW, portraitH), new SizeF(landscapeW, landscapeH))));
          RemoteViews restored = parcel(PrayerWidget.responsive(context, strip, schedule, NOW, options));
          Map<String, ?> prefs = new HashMap<String, Object>(AppPreferences.prefs(context).getAll());
          for (boolean landscape : new boolean[] {false, true, false}) {
            qualifiers(arabic, landscape, "mdpi");
            RuntimeEnvironment.setFontScale(scale);
            int w = landscape ? landscapeW : portraitW, h = landscape ? landscapeH : portraitH;
            direct(strip, schedule, NOW, w, h);
            List<String> expected = geometrySignature();
            RemoteViews selected = select(restored, w, h);
            apply(selected, px(w), px(h));
            assertEquals("Legacy portrait/landscape pair on API" + Build.VERSION.SDK_INT,
                expected, geometrySignature());
            assertProfile(schedule, NOW, arabic, "API" + Build.VERSION.SDK_INT + " " + w + "x" + h);
            selected.reapply(context, root);
            layout();
            assertEquals(expected, geometrySignature());
            assertEquals(prefs, AppPreferences.prefs(context).getAll());
          }
        }
  }

  @Test public void phaseChangesMidnightMissingDataAndRecoveryNeverKeepStaleTimes() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true}) {
        configure(arabic, true, true, 2f, "large", false);
        direct(strip, schedule, NOW, strip ? 360 : 180, strip ? 80 : 240);
        LocalDate date = NOW.atZone(Schedule.ZONE).toLocalDate();
        List<Instant> boundaries = new ArrayList<>();
        for (int index : Schedule.PRAYERS) {
          Instant adhan = ZonedDateTime.of(date, schedule.days.get(date)[index], Schedule.ZONE).toInstant();
          Instant iqamah = adhan.plusSeconds(AppPreferences.iqamahMinutes(context)[index] * 60L);
          boundaries.addAll(Arrays.asList(adhan.minusSeconds(1), adhan, adhan.plusSeconds(1),
              iqamah.minusSeconds(1), iqamah, iqamah.plusSeconds(1)));
        }
        Instant midnight = date.plusDays(1).atStartOfDay(Schedule.ZONE).toInstant();
        boundaries.addAll(Arrays.asList(midnight.minusSeconds(1), midnight, midnight.plusSeconds(1),
            schedule.days.firstKey().minusDays(1).atStartOfDay(Schedule.ZONE).toInstant(),
            schedule.days.lastKey().plusDays(1).atStartOfDay(Schedule.ZONE).toInstant()));
        for (Instant now : boundaries) {
          reapply(schedule, now);
          assertProfile(schedule, now, arabic, "boundary " + now);
          if (!schedule.days.containsKey(now.atZone(Schedule.ZONE).toLocalDate()))
            assertNotNull("Unavailable-date recovery is accessible", root.getContentDescription());
        }
        reapply(null, NOW);
        assertProfile(null, NOW, arabic, "missing timetable");
        assertNotNull("Null-data recovery is accessible", root.getContentDescription());
        reapply(schedule, NOW);
        assertProfile(schedule, NOW, arabic, "recovered timetable");
      }
  }

  @Test public void syntheticAllDayTimesDoNotLoseDigitsOrMeridiems() throws Exception {
    Schedule fixture = new Schedule(new StringReader("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n"
        + "2026-10-16,00:00,06:00,12:00,15:08,18:09,23:59\n"
        + "2026-10-17,00:01,06:01,12:01,15:18,18:19,23:51\n"));
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true})
        for (boolean eastern : new boolean[] {false, true})
          for (boolean twelve : new boolean[] {false, true}) {
            configure(arabic, eastern, twelve, 2f, "large", false);
            direct(strip, fixture, Instant.parse("2026-10-16T08:00:00Z"), strip ? 260 : 140, strip ? 56 : 140);
            assertProfile(fixture, Instant.parse("2026-10-16T08:00:00Z"), arabic, "midnight/noon/meridiem");
            String noon = text("widget_dhuhr_time").getText().toString();
            assertTrue("Noon retains 12", noon.startsWith(AppPreferences.digits(context, "12:")));
            if (twelve) assertTrue("Noon retains PM marker", noon.endsWith(arabic ? "م" : "PM"));
          }
  }

  @Test public void languageNumeralAndTextPreferencesReapplyWithoutStaleLayout() throws Exception {
    for (boolean strip : new boolean[] {false, true}) {
      configure(false, false, false, 1f, "normal", false);
      direct(strip, schedule, NOW, strip ? 400 : 200, strip ? 100 : 240);
      for (String system : new String[] {"en-US", "ar-AE"})
        for (boolean arabic : new boolean[] {false, true, false})
          for (boolean eastern : new boolean[] {false, true})
            for (boolean twelve : new boolean[] {false, true}) {
              Locale.setDefault(Locale.forLanguageTag(system));
              AppPreferences.prefs(context).edit().putBoolean("arabic", arabic)
                  .putBoolean("eastern_digits", eastern).putBoolean("twelve_hour", twelve)
                  .putBoolean("countdown_seconds", !eastern).commit();
              reapply(schedule, NOW);
              assertProfile(schedule, NOW, arabic, "system " + system + " preference switch");
            }
      float previous = 0;
      for (String size : new String[] {"small", "normal", "large"}) {
        AppPreferences.prefs(context).edit().putString("widget_text", size).commit();
        reapply(schedule, NOW);
        float current = text("widget_fajr_name").getTextSize();
        assertTrue("Larger preference may not shrink fitted text", current >= previous);
        previous = current;
        assertProfile(schedule, NOW, false, "text preference " + size);
      }
    }
  }

  @Test public void backgroundOpacityAndThemesNeverFadePrayerTextOrChangePreferences() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true})
        for (String mode : new String[] {"light", "dark"})
          for (String accent : new String[] {"green", "navy", "sand"})
            for (int opacity : new int[] {0, 65, 100}) {
              configure(arabic, true, true, 2f, "large", false);
              AppPreferences.prefs(context).edit().putString("widget_mode", mode)
                  .putString("widget_accent", accent).putInt("widget_opacity", opacity).commit();
              Map<String, ?> original = new HashMap<String, Object>(AppPreferences.prefs(context).getAll());
              responsive(strip, schedule, NOW, strip ? 360 : 180, strip ? 80 : 240);
              assertProfile(schedule, NOW, arabic, mode + " " + accent + " opacity" + opacity);
              assertTrue("Background remains a drawable ImageView", view("widget_background") instanceof ImageView);
              assertEquals("Only background image opacity changes", Math.round(255f * opacity / 100f),
                  ((ImageView) view("widget_background")).getImageAlpha());
              reapply(schedule, NOW.plusSeconds(61));
              assertEquals("Reapply preserves theme/text preferences", original, AppPreferences.prefs(context).getAll());
            }
  }

  @Test public void nativePreviewImagesCoverBothMinimumAndLargerTypes() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true}) {
        configure(arabic, arabic, true, 1f, "normal", false);
        for (int[] size : strip ? new int[][] {{250,40},{276,51},{260,56},{300,64},{360,80},{400,128},{260,300}}
            : new int[][] {{110,110},{130,117},{140,140},{160,160},{177,213},{240,240},{400,400}}) {
          responsive(strip, schedule, NOW, size[0], size[1]);
          assertProfile(schedule, NOW, arabic, "preview " + Arrays.toString(size));
          Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
          root.draw(new Canvas(image));
          int painted = 0;
          for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
            if (Color.alpha(image.getPixel(x, y)) != 0) painted++;
          assertTrue("Native renderer paints actual content", painted > width * height / 20);
          File folder = new File(System.getProperty("qa.output", "screenshots"), "two-widgets");
          assertTrue("Create preview directory", folder.isDirectory() || folder.mkdirs());
          File file = new File(folder, (strip ? "strip" : "compact") + (arabic ? "-ar-" : "-en-")
              + width + "x" + height + ".png");
          try (FileOutputStream output = new FileOutputStream(file)) {
            assertTrue("Native PNG encode", image.compress(Bitmap.CompressFormat.PNG, 100, output));
          }
          image.recycle();
          System.out.println("Two-widget native preview: " + file.getAbsolutePath());
        }
      }
  }

  @Test public void highResolutionNativeDeliveryPreviews() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true}) {
        configure(arabic, arabic, arabic, 1f, "normal", false);
        qualifiers(arabic, false, "xxxhdpi");
        RuntimeEnvironment.setFontScale(1f);
        createHost();
        int w = strip ? 320 : 160, h = strip ? 64 : 160;
        responsive(strip, schedule, NOW, w, h);
        assertEquals("Delivery preview uses real 4x-density pixels", w * 4, width);
        assertEquals(h * 4, height);
        assertProfile(schedule, NOW, arabic, "delivery preview " + w + "x" + h + "dp");
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(image));
        File folder = new File(System.getProperty("qa.output", "screenshots"), "two-widgets-hires");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        File file = new File(folder, (strip ? "strip" : "compact") + (arabic ? "-ar-eastern-12h-" : "-en-western-24h-")
            + w + "x" + h + "dp-xxxhdpi.png");
        try (FileOutputStream output = new FileOutputStream(file)) {
          assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        image.recycle();
        System.out.println("DELIVERY PREVIEW " + file.getAbsolutePath());
      }
  }

  @Test public void absentEmptyMalformedAndOversizedOptionsUseSafeDefaults() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true}) {
        configure(arabic, true, true, 1.3f, "large", false);
        int w = strip ? 300 : 140, h = strip ? 64 : 160;
        List<Bundle> inputs = new ArrayList<>();
        inputs.add(null);
        inputs.add(new Bundle());
        Bundle empty = new Bundle();
        empty.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, new ArrayList<SizeF>());
        inputs.add(empty);
        Bundle excessive = new Bundle();
        ArrayList<SizeF> tooMany = new ArrayList<>();
        for (int i = 0; i < 17; i++) tooMany.add(new SizeF(w + i, h + i));
        excessive.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, tooMany);
        inputs.add(excessive);
        Bundle invalid = new Bundle();
        invalid.putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,
            new ArrayList<>(Arrays.asList(null, new SizeF(0, 0), new SizeF(-1, h), new SizeF(5000, 5000))));
        invalid.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -2);
        invalid.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 5000);
        invalid.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        invalid.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1);
        inputs.add(invalid);
        Bundle wrongType = new Bundle();
        wrongType.putString(AppWidgetManager.OPTION_APPWIDGET_SIZES, "malformed launcher value");
        inputs.add(wrongType);
        direct(strip, schedule, NOW, w, h);
        List<String> expected = geometrySignature();
        for (Bundle input : inputs) {
          apply(select(parcel(PrayerWidget.responsive(context, strip, schedule, NOW, input)), w, h), px(w), px(h));
          assertEquals("Null/malformed options retain safe provider-specific defaults", expected, geometrySignature());
          assertProfile(schedule, NOW, arabic, "default options");
        }
      }
  }

  @Test public void optionalDateGivesSpaceToPrayersAndChangesAtUaeMidnight() throws Exception {
    for (boolean strip : new boolean[] {false, true})
      for (boolean arabic : new boolean[] {false, true}) {
        configure(arabic, true, true, 1f, "normal", false);
        responsive(strip, schedule, NOW, strip ? 260 : 140, strip ? 56 : 140);
        assertProfile(schedule, NOW, arabic, "minimum with optional date");
        if (strip) assertFalse("Minimum-height strip allocates space to prayer fields", visible(view("widget_header")));
        responsive(strip, schedule, NOW, 400, 400);
        assertTrue("Roomy widget includes useful date", visible(view("widget_header")));
        assertTrue(visible(view("widget_meta")));
        assertProfile(schedule, NOW, arabic, "roomy date");
        Instant midnight = NOW.atZone(Schedule.ZONE).toLocalDate().plusDays(1).atStartOfDay(Schedule.ZONE).toInstant();
        reapply(schedule, midnight.minusSeconds(1));
        String previous = text("widget_meta").getText().toString();
        reapply(schedule, midnight);
        assertNotEquals("Date changes at UAE midnight", previous, text("widget_meta").getText().toString());
        assertProfile(schedule, midnight, arabic, "new UAE date");
      }
  }

  private Class<? extends AppWidgetProvider> providerClass(boolean strip) {
    return strip ? PrayerWidgetStrip.class : PrayerWidgetCompact.class;
  }

  @Test @Config(sdk = {26, 29, 35})
  public void manifestRegistersExactlyTwoProvidersWithHonestLauncherMetadata() throws Exception {
    PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
        PackageManager.GET_RECEIVERS | PackageManager.GET_META_DATA);
    Set<String> providers = new HashSet<>();
    for (ActivityInfo receiver : info.receivers)
      if (receiver.metaData != null && receiver.metaData.containsKey(AppWidgetManager.META_DATA_APPWIDGET_PROVIDER))
        providers.add(receiver.name);
    assertEquals("Picker contains exactly Compact and Strip, never the shared base/obsolete types",
        new HashSet<>(Arrays.asList(PrayerWidgetCompact.class.getName(), PrayerWidgetStrip.class.getName())), providers);
    for (String obsolete : new String[] {"PrayerWidget", "PrayerWidgetTall", "PrayerWidgetFlexible",
        "PrayerWidgetHorizontal", "PrayerWidgetAdaptive"}) {
      try {
        context.getPackageManager().getReceiverInfo(new ComponentName(context.getPackageName(),
            context.getPackageName() + "." + obsolete), PackageManager.GET_META_DATA);
        fail("Obsolete/shared provider must not remain registered: " + obsolete);
      } catch (PackageManager.NameNotFoundException expected) { /* Correct: no legacy entry. */ }
    }
    for (boolean strip : new boolean[] {false, true}) {
      ActivityInfo receiver = context.getPackageManager().getReceiverInfo(
          new ComponentName(context, providerClass(strip)), PackageManager.GET_META_DATA);
      int resource = receiver.metaData.getInt(AppWidgetManager.META_DATA_APPWIDGET_PROVIDER);
      assertTrue("Provider has metadata resource", resource != 0);
      try (XmlResourceParser xml = context.getResources().getXml(resource)) {
        while (xml.next() != XmlResourceParser.START_TAG) {}
        String ns = "http://schemas.android.com/apk/res/android";
        assertEquals(strip ? "250.0dip" : "110.0dip", xml.getAttributeValue(ns, "minWidth"));
        assertEquals(strip ? "40.0dip" : "110.0dip", xml.getAttributeValue(ns, "minHeight"));
        assertEquals(strip ? "250.0dip" : "110.0dip", xml.getAttributeValue(ns, "minResizeWidth"));
        assertEquals(strip ? "40.0dip" : "110.0dip", xml.getAttributeValue(ns, "minResizeHeight"));
        assertEquals("Both axes may resize without changing type", 3, xml.getAttributeIntValue(ns, "resizeMode", 0));
        assertEquals(strip ? 4 : 2, xml.getAttributeIntValue(ns, "targetCellWidth", 0));
        assertEquals(strip ? 1 : 2, xml.getAttributeIntValue(ns, "targetCellHeight", 0));
        int expectedLayout = context.getResources().getIdentifier(strip ? "widget_strip" : "widget_compact",
            "layout", context.getPackageName());
        assertEquals("Initial layout matches chosen type", expectedLayout, xml.getAttributeResourceValue(ns, "initialLayout", 0));
      }
    }
  }

  @Test @Config(sdk = {26, 29, 35})
  public void bothProvidersParticipateInUpdateOptionsRefreshAndRemovalLifecycle() throws Exception {
    AppWidgetManager manager = AppWidgetManager.getInstance(context);
    assertFalse("Empty launcher initially has no installed widget", PrayerWidget.hasWidgets(context));
    for (boolean strip : new boolean[] {false, true}) {
      int w = strip ? 260 : 140, h = strip ? 56 : 140;
      int layout = PrayerWidget.render(context, strip, schedule, NOW, w, h).getLayoutId();
      int id = Shadows.shadowOf(manager).createWidget(providerClass(strip), layout);
      assertTrue("Either widget type alone counts as installed", PrayerWidget.hasWidgets(context));
      AppWidgetProvider provider = Shadows.shadowOf(manager).getAppWidgetProviderFor(id);
      provider.onEnabled(context);
      for (int[] size : strip ? new int[][] {{260,56},{360,80},{260,300},{600,56}}
          : new int[][] {{140,140},{177,213},{400,140},{140,400}}) {
        Bundle options = options(size[0], size[1]);
        manager.updateAppWidgetOptions(id, options);
        provider.onAppWidgetOptionsChanged(context, manager, id, options);
        provider.onUpdate(context, manager, new int[] {id});
        PrayerWidget.updateAll(context);
        View updated = Shadows.shadowOf(manager).getViewFor(id);
        assertNotNull("Global update retains chosen provider", updated);
        for (String key : KEYS) {
          int timeId = context.getResources().getIdentifier("widget_" + key + "_time", "id", context.getPackageName());
          assertTrue("Global update populates " + key,
              ((TextView) updated.findViewById(timeId)).getText().length() > 0);
        }
      }
      provider.onReceive(context, new Intent(PrayerWidget.ACTION_REFRESH));
      provider.onReceive(context, new Intent(PrayerWidget.ACTION_MINUTE));
      provider.onDeleted(context, new int[] {id});
      provider.onDisabled(context);
    }
  }

  private List<ShadowAlarmManager.ScheduledAlarm> minuteAlarms(ShadowAlarmManager manager) {
    List<ShadowAlarmManager.ScheduledAlarm> result = new ArrayList<>();
    for (ShadowAlarmManager.ScheduledAlarm alarm : manager.getScheduledAlarms())
      if (alarm.operation != null && PrayerWidget.ACTION_MINUTE.equals(
          Shadows.shadowOf(alarm.operation).getSavedIntent().getAction())) result.add(alarm);
    return result;
  }

  @Implements(AppWidgetManager.class)
  public static class ControlledManager extends ShadowAppWidgetManager {
    static int rejected;
    static boolean rejectOptions, installed, stripOnly;
    static int providerQueries;
    static final List<Integer> updated = new ArrayList<>();
    static final List<String> queried = new ArrayList<>();
    @Implementation public int[] getAppWidgetIds(ComponentName provider) {
      providerQueries++;
      queried.add(provider.getClassName());
      if (!installed) return new int[0];
      if (provider.getClassName().equals(PrayerWidgetCompact.class.getName())) return stripOnly?new int[0]:new int[] {101, 102};
      if (provider.getClassName().equals(PrayerWidgetStrip.class.getName())) return new int[] {201, 202};
      return new int[0];
    }
    @Implementation public Bundle getAppWidgetOptions(int id) {
      if (id == rejected && rejectOptions) throw new IllegalArgumentException("Injected stale launcher options");
      return new Bundle();
    }
    @Implementation public void updateAppWidget(int id, RemoteViews views) {
      if (id == rejected) throw new IllegalStateException("Injected rejected widget update");
      updated.add(id);
    }
    @Implementation public void updateAppWidget(int[] ids, RemoteViews views) {
      for (int id : ids) updateAppWidget(id, views);
    }
  }

  private void resetControlledManager() {
    ControlledManager.rejected = -1;
    ControlledManager.rejectOptions = false;
    ControlledManager.installed = true;
    ControlledManager.stripOnly = false;
    ControlledManager.providerQueries = 0;
    ControlledManager.updated.clear();
    ControlledManager.queried.clear();
  }

  @Test @Config(shadows = ControlledManager.class)
  public void rejectedInstanceOptionsAndCallbacksRecoverWithoutBlockingEitherType() {
    resetControlledManager();
    for (int rejected : new int[] {101, 201}) {
      ControlledManager.rejected = rejected;
      for (boolean rejectOptions : new boolean[] {false, true}) {
        ControlledManager.rejectOptions = rejectOptions;
        ControlledManager.updated.clear();
        PrayerWidget.updateAll(context);
        for (int id : new int[] {101,102,201,202})
          assertEquals("One bad instance never blocks another provider/instance", id != rejected,
              ControlledManager.updated.contains(id));
      }
      AppPreferences.prefs(context).edit().putBoolean("schedule_error", true).commit();
      AppWidgetProvider provider = rejected == 101 ? new PrayerWidgetCompact() : new PrayerWidgetStrip();
      provider.onUpdate(context, AppWidgetManager.getInstance(context), new int[] {rejected, rejected + 1});
      assertFalse("Scheduling still runs despite rejected render/options", AppPreferences.prefs(context).contains("schedule_error"));
      AlarmManager alarms = context.getSystemService(AlarmManager.class);
      PendingIntent old = PendingIntent.getBroadcast(context, 705,
          new Intent(context, PrayerWidget.class).setAction(PrayerWidget.ACTION_MINUTE), PendingIntent.FLAG_IMMUTABLE);
      alarms.set(AlarmManager.RTC, NOW.plusSeconds(60).toEpochMilli(), old);
      provider.onAppWidgetOptionsChanged(context, AppWidgetManager.getInstance(context), rejected, new Bundle());
      List<ShadowAlarmManager.ScheduledAlarm> display=minuteAlarms(Shadows.shadowOf(alarms));
      assertTrue("Failed resize leaves at most one current display refresh",display.size()<=1);
      for(ShadowAlarmManager.ScheduledAlarm alarm:display)assertNotEquals("Obsolete shared-base receiver is removed",
          PrayerWidget.class.getName(),Shadows.shadowOf(alarm.operation).getSavedIntent().getComponent().getClassName());
      ControlledManager.rejected = -1;
      ControlledManager.updated.clear();
      PrayerWidget.updateAll(context);
      assertTrue("Normal refresh recovers without reinstall", ControlledManager.updated.containsAll(Arrays.asList(101,102,201,202)));
    }
    for (String type : ControlledManager.queried)
      assertTrue("Global update must never address removed providers: " + type,
          type.equals(PrayerWidgetCompact.class.getName()) || type.equals(PrayerWidgetStrip.class.getName()));
  }

  public static class FixedActivity extends MainActivity {
    @Override protected Instant now() { return NOW; }
  }

  @Test @Config(shadows = ControlledManager.class)
  public void mainActivityStillOpensAndResumesWhenEitherWidgetTypeIsRejected() {
    resetControlledManager();
    for (int rejected : new int[] {101, 201})
      for (boolean arabic : new boolean[] {false, true}) {
        ControlledManager.rejected = rejected;
        ControlledManager.rejectOptions = true;
        AppPreferences.prefs(context).edit().putBoolean("arabic", arabic).commit();
        ActivityController<FixedActivity> activity = Robolectric.buildActivity(FixedActivity.class);
        try {
          activity.create().start().resume().visible();
          for (boolean resume : new boolean[] {false, true}) {
            if (resume) activity.pause().resume();
            View content = activity.get().findViewById(android.R.id.content);
            for (int i = 0; i < Schedule.PRAYERS.length; i++) {
              TextView prayer = content.findViewWithTag("prayer_name_" + Schedule.PRAYERS[i]);
              assertNotNull("Main activity retains every prayer", prayer);
              assertEquals((arabic ? AR : EN)[i], prayer.getText().toString());
            }
          }
        } finally { activity.pause().stop().destroy(); }
      }
  }

  @Test @Config(shadows = ControlledManager.class)
  public void installedWidgetsReplaceLegacyAlarmsWithOneCurrentDisplayRefresh() {
    resetControlledManager();
    AlarmManager alarms = context.getSystemService(AlarmManager.class);
    for (Class<?> provider : new Class<?>[] {PrayerWidget.class, PrayerWidgetCompact.class, PrayerWidgetStrip.class}) {
      PendingIntent old = PendingIntent.getBroadcast(context, 705,
          new Intent(context, provider).setAction(PrayerWidget.ACTION_MINUTE), PendingIntent.FLAG_IMMUTABLE);
      alarms.set(AlarmManager.RTC, NOW.plusSeconds(60).toEpochMilli(), old);
    }
    assertEquals(3, minuteAlarms(Shadows.shadowOf(alarms)).size());
    for (boolean eastern : new boolean[] {false, true})
      for (boolean seconds : new boolean[] {false, true}) {
        AppPreferences.prefs(context).edit().putBoolean("eastern_digits", eastern)
            .putBoolean("countdown_seconds", seconds).commit();
        PrayerWidget.scheduleMinute(context, NOW);
        List<ShadowAlarmManager.ScheduledAlarm> ticks=minuteAlarms(Shadows.shadowOf(alarms));
        assertEquals("Installed Compact has one next-adhan display refresh",1,ticks.size());
        long target=schedule.next(NOW).at.toInstant().toEpochMilli();
        assertEquals("Live timers schedule their final-minute guard; numeral snapshots schedule next minute",
            PrayerWidget.liveTimer(context)?target-60_000L:(NOW.toEpochMilli()/60_000L+1)*60_000L,ticks.get(0).triggerAtTime);
        assertEquals(AlarmManager.RTC,ticks.get(0).type);
        assertFalse(ticks.get(0).allowWhileIdle);
        assertEquals(0,ticks.get(0).interval);
        assertEquals(PrayerWidgetCompact.class.getName(),Shadows.shadowOf(ticks.get(0).operation).getSavedIntent().getComponent().getClassName());
        PrayerWidget.updateAll(context,NOW);
        assertEquals("Global refresh replaces rather than duplicates display work",1,minuteAlarms(Shadows.shadowOf(alarms)).size());
      }
  }

  @Test @Config(shadows = ControlledManager.class)
  public void obsoleteIqamahExpiryIsCancelledAndNeverRescheduledWhilePrayerAlarmsRemain() {
    resetControlledManager();
    Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
    AlertScheduler.createChannel(context);
    AppPreferences.prefs(context).edit().putBoolean(AlertScheduler.ENABLED, true)
        .putInt(AlertScheduler.REMINDER_MINUTES, 15).putInt(AlertScheduler.REMINDER_MASK, AlertPolicy.PRAYER_MASK)
        .putBoolean("iqamah_enabled", true).commit();
    assertTrue("Notification permission fixture is active", AlertScheduler.notificationsAllowed(context));
    AlarmManager alarms = context.getSystemService(AlarmManager.class);
    LocalDate date = NOW.atZone(Schedule.ZONE).toLocalDate();
    for (int index : Schedule.PRAYERS) {
      Instant adhan = ZonedDateTime.of(date, schedule.days.get(date)[index], Schedule.ZONE).toInstant();
      Instant after = adhan.plusSeconds(1);
      PrayerPhase phase = AppPreferences.phase(context, schedule, after);
      assertNotNull("Estimated iqamah remains available to the main screen", phase);
      assertTrue("Fixture is inside an estimated iqamah window", phase.iqamah);
      long target = phase.targetAt.toInstant().toEpochMilli();
      PendingIntent legacy = PendingIntent.getBroadcast(context, 704,
          new Intent(context, AlarmReceiver.class).setAction(AlertScheduler.IQAMAH_WIDGET_ACTION)
              .putExtra("at", target), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      alarms.set(AlarmManager.RTC_WAKEUP, target, legacy);
      AppPreferences.prefs(context).edit().putLong("scheduled_iqamah_at", target).commit();
      AlertScheduler.reschedule(context, after);
      assertFalse("Upgrade cleans stale iqamah-display state", AppPreferences.prefs(context).contains("scheduled_iqamah_at"));
      Set<String> actions = new HashSet<>();
      for (ShadowAlarmManager.ScheduledAlarm alarm : Shadows.shadowOf(alarms).getScheduledAlarms())
        if (alarm.operation != null) actions.add(Shadows.shadowOf(alarm.operation).getSavedIntent().getAction());
      assertFalse("Widgets without countdowns never wake the device at iqamah expiry",
          actions.contains(AlertScheduler.IQAMAH_WIDGET_ACTION));
      assertTrue("Prayer/date maintenance remains scheduled", actions.contains(AlertScheduler.ACTION));
      assertTrue("Authorized advance reminders remain scheduled", actions.contains(AlertScheduler.REMINDER_ACTION));
      AlertPolicy.Event expectedPrayer = AlertPolicy.next(schedule, after, true, true);
      AlertPolicy.Reminder expectedReminder = AlertPolicy.nextReminder(schedule, after, 15, AlertPolicy.PRAYER_MASK);
      assertEquals("Prayer timestamp remains exact", expectedPrayer.epochMillis,
          AppPreferences.prefs(context).getLong("scheduled_at", 0));
      assertEquals("Advance reminder remains exact", expectedReminder.epochMillis,
          AppPreferences.prefs(context).getLong("scheduled_reminder_at", 0));
      assertTrue("Main-screen iqamah calculation remains enabled", AppPreferences.phase(context, schedule, after).iqamah);
    }
  }

  @Test @Config(shadows = ControlledManager.class)
  public void removingLastWidgetCancelsMinuteWorkAndPreservesPrayerAlarms() {
    resetControlledManager();
    AlarmManager alarms = context.getSystemService(AlarmManager.class);
    ShadowAlarmManager shadow = Shadows.shadowOf(alarms);
    List<PendingIntent> sentinels = new ArrayList<>();
    for (String action : new String[] {AlertScheduler.ACTION, AlertScheduler.REMINDER_ACTION,
        AlertScheduler.IQAMAH_WIDGET_ACTION}) {
      PendingIntent sentinel = PendingIntent.getBroadcast(context, 1200 + sentinels.size(),
          new Intent(context, AlarmReceiver.class).setAction(action), PendingIntent.FLAG_IMMUTABLE);
      sentinels.add(sentinel);
      alarms.set(AlarmManager.RTC_WAKEUP, NOW.plusSeconds(3600).toEpochMilli(), sentinel);
    }
    // Simulate a pending refresh from the prior released widget implementation.
    PendingIntent legacy = PendingIntent.getBroadcast(context, 705,
        new Intent(context, PrayerWidget.class).setAction(PrayerWidget.ACTION_MINUTE), PendingIntent.FLAG_IMMUTABLE);
    alarms.set(AlarmManager.RTC, NOW.plusSeconds(60).toEpochMilli(), legacy);
    assertFalse(minuteAlarms(shadow).isEmpty());
    ControlledManager.installed = false;
    assertFalse(PrayerWidget.hasWidgets(context));
    PrayerWidget.scheduleMinute(context, NOW);
    assertTrue("No widgets means no orphan minute refresh, including old base-provider work", minuteAlarms(shadow).isEmpty());
    for (PendingIntent sentinel : sentinels) {
      boolean retained = false;
      for (ShadowAlarmManager.ScheduledAlarm alarm : shadow.getScheduledAlarms())
        if (sentinel.equals(alarm.operation)) retained = true;
      assertTrue("Widget maintenance leaves existing prayer/reminder/iqamah alarms untouched", retained);
    }
    new PrayerWidgetCompact().onDeleted(context, new int[] {101,102});
    new PrayerWidgetStrip().onDeleted(context, new int[] {201,202});
    new PrayerWidgetStrip().onDisabled(context);
    assertTrue("Removal/disabled callbacks do not revive minute work", minuteAlarms(shadow).isEmpty());
  }
  /** The visible Compact header timer: live Chronometer, or a minute snapshot when the
   * device's native timer digits differ from the saved numeral choice. */
  private View timer() {
    View live = view("widget_countdown"), snap = view("widget_iqamah_minutes");
    if (visible(live)) { assertFalse("Only one timer form at a time", visible(snap)); return live; }
    return visible(snap) ? snap : null;
  }
  private String timerLabel() {
    View t = timer();
    if (t == null) return null;
    return t instanceof Chronometer ? ((Chronometer) t).getFormat() : ((TextView) t).getText().toString();
  }
  private void assertTimer(Instant now, int index, boolean iqamah, Instant target, boolean ar) {
    View t = timer();
    assertNotNull("Header timer visible at "+widthDp+"x"+heightDp+" fontScale="+context.getResources().getConfiguration().fontScale+" settings="+AppPreferences.prefs(context).getAll(), t);
    String label = timerLabel();
    String name = ar ? new String[] {"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"}[index]
        : new String[] {"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"}[index];
    assertTrue("Timer names its target: " + label, iqamah ? label.contains(ar ? "الإقامة" : "Iqamah") : label.contains(name));
    assertTrue("Timer is described accessibly", t.getContentDescription() != null && t.getContentDescription().length() > 0);
    if(target.toEpochMilli()-now.toEpochMilli()<=60_000L){
      assertFalse("Final-minute fallback is static",t instanceof Chronometer);
      assertTrue("Final-minute fallback carries the absolute target time",label.contains(AppPreferences.time(context,target.atZone(Schedule.ZONE).toLocalTime())));
    } else if (t instanceof Chronometer) {
      Chronometer c = (Chronometer) t;
      assertTrue("Live timer counts down", c.isCountDown());
      long remaining = c.getBase() - android.os.SystemClock.elapsedRealtime();
      assertEquals("Live timer targets the right instant", target.toEpochMilli() - now.toEpochMilli(), remaining, 2000);
    } else {
      assertTrue("Snapshot carries the rounded-up minutes: " + label,
          label.contains(PrayerWidget.snapshot(context, target.toEpochMilli() - now.toEpochMilli())));
      assertNumerals(label, AppPreferences.eastern(context));
    }
  }

  @Test public void compactCountdownFitsWithoutShrinkingPrayerFonts() throws Exception {
    LocalDate day=NOW.atZone(Schedule.ZONE).toLocalDate();
    LocalTime[] saved=schedule.days.get(day);
    Instant adhan=ZonedDateTime.of(day,saved[2],Schedule.ZONE).toInstant();
    Instant iqamah=adhan.plusSeconds(AppPreferences.iqamahMinutes(context)[2]*60L);
    int profiles=0,hidden=0;
    java.util.Set<String> hiddenSizes=new java.util.TreeSet<>();
    for(boolean ar:new boolean[]{false,true})for(boolean eastern:new boolean[]{false,true})
      for(boolean twelve:new boolean[]{false,true})for(float scale:new float[]{.85f,1f,1.3f,1.5f,2f})
        for(String size:new String[]{"small","normal","large"})for(int[] dimensions:COMPACT_SIZES){
          configure(ar,eastern,twelve,scale,size,false);
          direct(false,schedule,adhan.minusSeconds(600),dimensions[0],dimensions[1]);
          float font=text("widget_fajr_time").getTextSize();
          if(dimensions[0]==160&&dimensions[1]==160)assertTimer(adhan.minusSeconds(600),2,false,adhan,ar);
          reapply(schedule,adhan);
          assertEquals("Timer never changes prayer font",font,text("widget_fajr_time").getTextSize(),0);
          if(timer()==null){hidden++;hiddenSizes.add(dimensions[0]+"x"+dimensions[1]);}
          if(dimensions[0]==160&&dimensions[1]==160)assertTimer(adhan,2,true,iqamah,ar);
          if(timer()!=null&&visible(view("widget_meta"))){
            Rect a=rect(timer()),b=rect(view("widget_meta"));
            assertFalse("Header fields do not overlap",Rect.intersects(a,b));
            assertTrue("Timer leads in reading order",ar?a.left>=b.right:a.right<=b.left);
          }
          assertProfile(schedule,adhan,ar,"countdown "+dimensions[0]+"x"+dimensions[1]+" ar="+ar+" eastern="+eastern+" twelve="+twelve+" scale="+scale);
          reapply(schedule,iqamah);
          if(timer()!=null)assertFalse("At iqamah the timer moves on to the next adhan",timerLabel().contains(ar?"الإقامة":"Iqamah"));
          assertEquals("Expiry never changes prayer font",font,text("widget_fajr_time").getTextSize(),0);
          profiles++;
        }
    System.out.println("PASS compact countdown layout/unchanged prayer font profiles: "+profiles+"; protected-font header omissions: "+hidden+"; "+hiddenSizes);
  }

  @Test @Config(sdk={26,29,35}) public void countdownBoundariesSettingsFridayAndMissingDataStayCorrect() throws Exception {
    LocalDate day=NOW.atZone(Schedule.ZONE).toLocalDate();
    LocalTime[] saved=schedule.days.get(day);
    for(int p=0;p<Schedule.PRAYERS.length;p++){
      int index=Schedule.PRAYERS[p];
      Instant adhan=ZonedDateTime.of(day,saved[index],Schedule.ZONE).toInstant();
      int minutes=AppPreferences.iqamahMinutes(context)[index];
      Instant target=adhan.plusSeconds(minutes*60L);
      direct(false,schedule,adhan.minusNanos(1),160,160);assertTimer(adhan.minusNanos(1),index,false,adhan,false);
      direct(false,schedule,adhan.minusMillis(1),160,160);assertTimer(adhan.minusMillis(1),index,false,adhan,false);
      reapply(schedule,adhan);assertTimer(adhan,index,true,target,false);
      reapply(schedule,target.minusMillis(1));assertTimer(target.minusMillis(1),index,true,target,false);
      reapply(schedule,target);assertFalse("Iqamah countdown ends at iqamah",timerLabel().contains("Iqamah"));
      direct(true,schedule,adhan,360,80);assertNull("Horizontal has no timer",view("widget_countdown"));
      assertNull("Horizontal has no snapshot timer",view("widget_iqamah_minutes"));
      AppPreferences.prefs(context).edit().putBoolean("iqamah_enabled",false).commit();
      direct(false,schedule,adhan,160,160);assertFalse("Disabled estimate shows the next adhan instead",timerLabel().contains("Iqamah"));
      AppPreferences.prefs(context).edit().putBoolean("iqamah_enabled",true).putInt("iqamah_offset_"+index,0).commit();
      reapply(schedule,adhan);assertFalse(timerLabel().contains("Iqamah"));
      AppPreferences.prefs(context).edit().putInt("iqamah_offset_"+index,60).commit();
      reapply(schedule,adhan);assertTimer(adhan,index,true,adhan.plusSeconds(3600),false);
      AppPreferences.prefs(context).edit().remove("iqamah_offset_"+index).commit();
    }
    LocalDate friday=LocalDate.of(2026,10,16);
    Instant jumuah=ZonedDateTime.of(friday,schedule.days.get(friday)[2],Schedule.ZONE).toInstant();
    direct(false,schedule,jumuah,160,160);assertFalse("Friday Dhuhr excluded",timerLabel().contains("Iqamah"));
    Schedule crossMidnight=new Schedule(new StringReader("date,fajr,sunrise,dhuhr,asr,maghrib,isha\n2026-10-17,05:00,06:00,12:00,15:00,18:00,23:50\n"));
    Instant midnight=LocalDate.of(2026,10,18).atStartOfDay(Schedule.ZONE).toInstant();
    direct(false,crossMidnight,midnight,160,160);
    assertNull("Outside saved data no timer is fabricated",timer());
    reapply(null,midnight);assertNull("Missing data clears timer",timer());
  }

  @Test @Config(shadows=ControlledManager.class) public void minuteWorkIsBoundedNonWakeupAndCannotConsumeIdleQuota() throws Exception {
    resetControlledManager();AlarmManager am=context.getSystemService(AlarmManager.class);
    LocalDate day=NOW.atZone(Schedule.ZONE).toLocalDate();
    Instant adhan=ZonedDateTime.of(day,schedule.days.get(day)[4],Schedule.ZONE).toInstant();
    Instant target=adhan.plusSeconds(AppPreferences.iqamahMinutes(context)[4]*60L);
    boolean live=PrayerWidget.liveTimer(context);
    PrayerWidget.scheduleMinute(context,adhan.minusMillis(1));
    assertEquals("Last-minute display fallback refreshes at adhan",adhan.toEpochMilli(),minuteAlarms(Shadows.shadowOf(am)).get(0).triggerAtTime);
    for(int m=0;m<5;m++){
      PrayerWidget.scheduleMinute(context,adhan.plusSeconds(m*60L+1));
      List<ShadowAlarmManager.ScheduledAlarm> ticks=minuteAlarms(Shadows.shadowOf(am));
      assertEquals("Only one one-shot display refresh exists",1,ticks.size());
      ShadowAlarmManager.ScheduledAlarm tick=ticks.get(0);
      assertEquals("Display refresh never wakes the device",AlarmManager.RTC,tick.type);
      assertFalse("Display refresh cannot use the prayer idle quota",tick.allowWhileIdle);
      assertEquals("One-shot display alarm",0,tick.interval);
      assertNull("No alarm-clock icon/service for widget display",am.getNextAlarmClock());
      assertEquals("Live timers get a final-minute guard then expiry; snapshots advance by minute",
          live?(m<4?target.minusSeconds(60).toEpochMilli():target.toEpochMilli()):adhan.plusSeconds((m+1)*60L).toEpochMilli(),tick.triggerAtTime);
    }
    AppPreferences.prefs(context).edit().putBoolean("iqamah_enabled",false).commit();
    PrayerWidget.scheduleMinute(context,adhan.plusSeconds(1));
    assertEquals("Disabled estimate still maintains the next-adhan timer",1,minuteAlarms(Shadows.shadowOf(am)).size());
    if(live)assertEquals(schedule.next(adhan.plusSeconds(1)).at.toInstant().minusSeconds(60).toEpochMilli(),minuteAlarms(Shadows.shadowOf(am)).get(0).triggerAtTime);
    AppPreferences.prefs(context).edit().putBoolean("iqamah_enabled",true).commit();
    PrayerWidget.scheduleMinute(context,adhan);assertEquals(1,minuteAlarms(Shadows.shadowOf(am)).size());
    ShadowAlarmManager.setCanScheduleExactAlarms(false);PrayerWidget.scheduleMinute(context,adhan);
    assertEquals("Exact permission denial uses ordinary display alarm",1,minuteAlarms(Shadows.shadowOf(am)).size());
    ShadowAlarmManager.setCanScheduleExactAlarms(true);
    ControlledManager.stripOnly=true;PrayerWidget.scheduleMinute(context,adhan);
    assertTrue("Horizontal remains installed",PrayerWidget.hasWidgets(context));
    assertEquals("Horizontal alone refreshes once, when its amber iqamah highlight ends",1,minuteAlarms(Shadows.shadowOf(am)).size());
    assertEquals(target.toEpochMilli(),minuteAlarms(Shadows.shadowOf(am)).get(0).triggerAtTime);
    ControlledManager.installed=false;PrayerWidget.scheduleMinute(context,adhan);
    assertTrue("Removing widgets cancels display work",minuteAlarms(Shadows.shadowOf(am)).isEmpty());
  }

  @Test public void nativeCountdownDeliveryPreviews() throws Exception {
    LocalDate day=NOW.atZone(Schedule.ZONE).toLocalDate();
    Instant adhan=ZonedDateTime.of(day,schedule.days.get(day)[2],Schedule.ZONE).toInstant();
    for(boolean ar:new boolean[]{false,true}){
      configure(ar,ar,true,1f,"normal",false);qualifiers(ar,false,"xxxhdpi");createHost();
      direct(false,schedule,adhan.plusSeconds(120),160,160);
      assertProfile(schedule,adhan.plusSeconds(120),ar,"countdown preview");
      Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
      File folder=new File(System.getProperty("qa.output","screenshots"),"countdown-v2.5.1");folder.mkdirs();
      try(FileOutputStream out=new FileOutputStream(new File(folder,(ar?"ar":"en")+"-compact-active.png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
  }

  @Test @Config(shadows=ControlledManager.class) public void globalBoundarySnapshotAndPartialLifecycleKeepAllInstancesFresh() throws Exception {
    resetControlledManager();
    LocalDate day=NOW.atZone(Schedule.ZONE).toLocalDate();
    Instant adhan=ZonedDateTime.of(day,schedule.days.get(day)[4],Schedule.ZONE).toInstant();
    Instant target=adhan.plusSeconds(300);
    PrayerWidget.updateAll(context,target.minusMillis(1));
    AlarmManager am=context.getSystemService(AlarmManager.class);
    assertEquals("Shared pre-expiry snapshot retains its boundary alarm",1,minuteAlarms(Shadows.shadowOf(am)).size());
    assertEquals(target.toEpochMilli(),minuteAlarms(Shadows.shadowOf(am)).get(0).triggerAtTime);
    assertTrue(ControlledManager.updated.containsAll(Arrays.asList(101,102,201,202)));
    ControlledManager.updated.clear();PrayerWidget.updateAll(context,target.plusMillis(1));
    assertEquals("Post-expiry refresh schedules only the next target",1,minuteAlarms(Shadows.shadowOf(am)).size());
    assertTrue("New display refresh is in the future",minuteAlarms(Shadows.shadowOf(am)).get(0).triggerAtTime>target.toEpochMilli());
    assertTrue("Expiry refresh covers every remaining instance",ControlledManager.updated.containsAll(Arrays.asList(101,102,201,202)));
    for(int callback=0;callback<5;callback++){
      ControlledManager.updated.clear();PrayerWidgetCompact provider=new PrayerWidgetCompact();
      switch(callback){
        case 0:provider.onDeleted(context,new int[]{999});break;
        case 1:provider.onDisabled(context);break;
        case 2:provider.onEnabled(context);break;
        case 3:provider.onUpdate(context,AppWidgetManager.getInstance(context),new int[]{101});break;
        default:provider.onAppWidgetOptionsChanged(context,AppWidgetManager.getInstance(context),101,options(177,213));
      }
      assertTrue("Callback cannot cancel an overdue global refresh while leaving other widgets stale: "+callback,ControlledManager.updated.containsAll(Arrays.asList(101,102,201,202)));
    }
  }

  @Test @Config(sdk={26,29,35})
  public void deliveredFinalMinuteGuardStopsTimerButWithheldGuardDocumentsAndroidLimit() throws Exception {
    LocalDate day=NOW.atZone(Schedule.ZONE).toLocalDate();
    Instant adhan=ZonedDateTime.of(day,schedule.days.get(day)[2],Schedule.ZONE).toInstant();
    configure(false,false,false,1f,"normal",false);
    assertTrue("US locale uses native Western digits",PrayerWidget.liveTimer(context));
    direct(false,schedule,adhan.minusMillis(60_001),160,160);
    assertTrue("Live seconds retained until the guard",timer() instanceof Chronometer);
    reapply(schedule,adhan.minusSeconds(60));
    assertFalse("Delivered guard replaces live seconds with an absolute target",timer() instanceof Chronometer);
    String absolute=timerLabel();
    assertTrue(absolute.contains("at "+AppPreferences.time(context,adhan.atZone(Schedule.ZONE).toLocalTime())));
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(62));
    assertEquals("Static target does not run negative when expiry delivery is late",absolute,timerLabel());
    reapply(schedule,adhan.plusSeconds(2));
    assertTrue("Delivered adhan refresh selects estimated iqamah",timerLabel().contains("Iqamah"));
    direct(false,schedule,adhan.minusMillis(60_001),160,160);
    Chronometer old=(Chronometer)timer();
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(63));
    assertTrue("Withheld refresh leaves an expired live base",old.getBase()<android.os.SystemClock.elapsedRealtime());
    // Robolectric's unattached RemoteViews host does not guarantee Chronometer visibility
    // callbacks. Invoke the framework's next tick formatter at the advanced clock time.
    Method tick=Chronometer.class.getDeclaredMethod("updateText",long.class);tick.setAccessible(true);
    tick.invoke(old,android.os.SystemClock.elapsedRealtime());
    assertTrue("Documented Android limit: stock next-tick text is negative: "+old.getText(),old.getText().toString().matches(".*[-−].*"));
    reapply(schedule,adhan.plusSeconds(3));
    assertFalse("Delivered refresh replaces negative native display",((TextView)timer()).getText().toString().matches(".*[-−].*"));
  }

  @Test @Config(shadows=ControlledManager.class)
  public void guardBoundarySchedulingIsExactAndNeverReschedulesPastGuard() throws Exception {
    resetControlledManager();
    Instant target=ZonedDateTime.of(NOW.atZone(Schedule.ZONE).toLocalDate(),schedule.days.get(NOW.atZone(Schedule.ZONE).toLocalDate())[2],Schedule.ZONE).toInstant();
    AlarmManager alarms=context.getSystemService(AlarmManager.class);
    for(boolean eastern:new boolean[]{false,true}){
      AppPreferences.prefs(context).edit().putBoolean("eastern_digits",eastern).commit();
      for(long remaining:new long[]{60_001,60_000,59_999,1}){
        Instant now=target.minusMillis(remaining);
        PrayerWidget.scheduleMinute(context,now);
        List<ShadowAlarmManager.ScheduledAlarm> ticks=minuteAlarms(Shadows.shadowOf(alarms));
        assertEquals(1,ticks.size());
        assertEquals(remaining>60_000?target.minusSeconds(60).toEpochMilli():target.toEpochMilli(),ticks.get(0).triggerAtTime);
        assertTrue(ticks.get(0).triggerAtTime>now.toEpochMilli());
        assertEquals(AlarmManager.RTC,ticks.get(0).type);assertFalse(ticks.get(0).allowWhileIdle);assertEquals(0,ticks.get(0).interval);
      }
    }
  }

  @Test public void finalMinuteClockLabelsFitBothLanguagesNumeralsAndMeridiems() throws Exception {
    LocalDate day=NOW.atZone(Schedule.ZONE).toLocalDate();int profiles=0;
    for(boolean ar:new boolean[]{false,true})for(boolean eastern:new boolean[]{false,true})
      for(boolean twelve:new boolean[]{false,true})for(float scale:new float[]{.85f,1f,2f})
        for(int index:Schedule.PRAYERS){
          configure(ar,eastern,twelve,scale,"large",false);
          Instant adhan=ZonedDateTime.of(day,schedule.days.get(day)[index],Schedule.ZONE).toInstant();
          for(boolean iqamah:new boolean[]{false,true}){
            Instant target=iqamah?adhan.plusSeconds(AppPreferences.iqamahMinutes(context)[index]*60L):adhan;
            Instant now=target.minusSeconds(30);
            direct(false,schedule,now,160,160);assertTimer(now,index,iqamah,target,ar);
            assertProfile(schedule,now,ar,"final-minute ar="+ar+" east="+eastern+" 12h="+twelve+" scale="+scale+" index="+index);
            assertTrue("Spoken header includes target clock",timer().getContentDescription().toString().contains(AppPreferences.time(context,target.atZone(Schedule.ZONE).toLocalTime())));
            assertTrue("Spoken header includes target date",timer().getContentDescription().toString().contains(AppPreferences.date(context,target.atZone(Schedule.ZONE).toLocalDate(),"d MMM")));
            profiles++;
          }
        }
    System.out.println("PASS final-minute clock label profiles: "+profiles);
  }

  @Test public void systemDigitScriptsNeverOverrideTheSavedNumerals() throws Exception {
    for(String system:new String[]{"en-US","ar-AE","fa-IR","bn-BD"})
      for(boolean ar:new boolean[]{false,true})for(boolean eastern:new boolean[]{false,true}){
        configure(ar,eastern,true,1f,"normal",false);Locale.setDefault(Locale.forLanguageTag(system));
        direct(false,schedule,NOW,160,160);
        assertNotNull("Timer remains visible with system locale "+system,timer());
        assertNumerals(((TextView)timer()).getText().toString(),eastern);
        if(timer() instanceof Chronometer){
          String sample=android.text.format.DateUtils.formatElapsedTime(65);
          for(char digit:sample.toCharArray())if(Character.isDigit(digit))
            assertTrue("Native timer used only for the exact saved digit script",eastern?digit>='٠'&&digit<='٩':digit>='0'&&digit<='9');
        }
        assertProfile(schedule,NOW,ar,"system digit script "+system+" eastern="+eastern);
      }
  }

}
