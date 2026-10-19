package ae.prayertimes.abudhabi;

import static org.junit.Assert.*;

import android.app.*;
import android.content.DialogInterface;
import android.graphics.*;
import android.os.*;
import android.text.Layout;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class NativeUiTest {
  public static class FixedActivity extends MainActivity {
    static Instant time = Instant.parse("2026-10-17T08:00:00Z");

    @Override
    protected Instant now() {
      return time;
    }
  }

  private static final String[] EN = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
  private static final String[] AR = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
  private ActivityController<FixedActivity> controller;
  private FixedActivity a;
  private int w, h;
  private boolean simulateInsets;

  @After
  public void tearDown() {
    stop();
  }

  private void stop() {
    if (controller != null) {
      controller.pause().stop().destroy();
      controller = null;
      a = null;
    }
  }

  private void launch(boolean ar, int width, int height, float scale, String mode, String accent) {
    stop();
    w = width;
    h = height;
    RuntimeEnvironment.setQualifiers(
        (ar ? "ar" : "en")
            + "-rUS-w"
            + w
            + "dp-h"
            + h
            + "dp-"
            + (w > h ? "land" : "port")
            + "-mdpi");
    RuntimeEnvironment.setFontScale(scale);
    AppPreferences.prefs(RuntimeEnvironment.getApplication())
        .edit()
        .clear()
        .putBoolean("arabic", ar)
        .putString("appearance_mode", mode)
        .putString("accent", accent)
        .commit();
    controller = Robolectric.buildActivity(FixedActivity.class).create().start().resume().visible();
    a = controller.get();
    layout();
  }

  private View layout() {
    View v = a.getWindow().getDecorView();
    if (simulateInsets && Build.VERSION.SDK_INT >= 35) {
      ViewGroup c = a.findViewById(android.R.id.content);
      c.getChildAt(0)
          .dispatchApplyWindowInsets(
              new WindowInsets.Builder()
                  .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, 24, 0, 0))
                  .setInsets(WindowInsets.Type.navigationBars(), Insets.of(0, 0, 0, 24))
                  .build());
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle();
    for (int i = 0; i < 3; i++) {
      v.measure(
          View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
      v.layout(0, 0, w, h);
    }
    return v;
  }

  private List<View> all(View v) {
    List<View> out = new ArrayList<>();
    out.add(v);
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) out.addAll(all(g.getChildAt(i)));
    }
    return out;
  }

  private View tag(String value) {
    return layout().findViewWithTag(value);
  }

  private TextView text(String exact) {
    for (View v : all(layout()))
      if (v instanceof TextView && exact.contentEquals(((TextView) v).getText()))
        return (TextView) v;
    return null;
  }

  private View desc(String value) {
    for (View v : all(layout()))
      if (v.getContentDescription() != null && v.getContentDescription().toString().equals(value))
        return v;
    return null;
  }

  private void verify(boolean ar, String label) throws Exception {
    View root = layout();
    screenshot(label, root);
    Switch toggle = (Switch) root.findViewWithTag("alerts_toggle");
    if (label.contains("figma")) {
      assertEquals(52, toggle.getWidth());
      assertEquals(48, toggle.getHeight());
      android.view.accessibility.AccessibilityNodeInfo info = toggle.createAccessibilityNodeInfo();
      assertEquals("android.widget.Switch", info.getClassName().toString());
      assertTrue(info.isCheckable());
      assertEquals(toggle.isChecked(), info.isChecked());
      System.out.println(
          "PASS native switch geometry/accessibility " + label + " checked=" + toggle.isChecked());
    }
    LocalDate selected = (LocalDate) field("selected");
    LocalTime[] official = AlertScheduler.schedule(a).days.get(selected);
    int ordinal = 0;
    for (int i : Schedule.PRAYERS) {
      assertEquals(
          label + ": prayer name " + i,
          (ar ? AR : EN)[ordinal++],
          ((TextView) root.findViewWithTag("prayer_name_" + i)).getText().toString());
      assertEquals(
          label + ": official prayer time " + i,
          AppPreferences.time(a, official[i]),
          ((TextView) root.findViewWithTag("prayer_time_" + i)).getText().toString());
    }
    for (String key :
        new String[] {
          "hero",
          "countdown",
          "countdown_units",
          "source_footer",
          "date_picker",
          "previous_day",
          "next_day",
          "today",
          "language",
          "settings",
          "alerts_toggle"
        }) {
      View v = root.findViewWithTag(key);
      assertNotNull(label + ": missing key element " + key, v);
      Rect r = new Rect();
      assertTrue(label + ": invisible key element " + key, v.getGlobalVisibleRect(r));
      assertEquals(label + ": clipped key height " + key, v.getHeight(), r.height());
      assertEquals(label + ": clipped key width " + key, v.getWidth(), r.width());
    }
    for (int i : Schedule.PRAYERS) {
      View row = root.findViewWithTag("prayer_" + i);
      assertNotNull(label + ": missing row " + i, row);
      for (String key : new String[] {"prayer_name_" + i, "prayer_time_" + i}) {
        TextView v = root.findViewWithTag(key);
        assertNotNull(label + ": missing label " + key, v);
        Rect r = new Rect();
        assertTrue(label + ": invisible " + key, v.getGlobalVisibleRect(r));
        assertEquals(label + ": clipped " + key, v.getHeight(), r.height());
        assertEquals(label + ": clipped width " + key, v.getWidth(), r.width());
      }
    }
    View badge = root.findViewWithTag("active_badge");
    if (badge != null) {
      Rect r = new Rect();
      assertTrue(label + ": badge invisible", badge.getGlobalVisibleRect(r));
      assertEquals(label + ": badge clipped vertically", badge.getHeight(), r.height());
      assertEquals(label + ": badge clipped horizontally", badge.getWidth(), r.width());
    }
    for (View v : all(root))
      assertFalse(
          label + ": scrolling container",
          v instanceof ScrollView || v instanceof HorizontalScrollView);
    for (String name : ar ? AR : EN) {
      TextView v = text(name);
      assertNotNull(label + ": prayer missing " + name, v);
      assertTrue(label + ": zero prayer height " + name, v.getHeight() > 0);
      Rect r = new Rect();
      assertTrue(label + ": invisible prayer " + name, v.getGlobalVisibleRect(r));
      assertEquals(label + ": clipped prayer height " + name, v.getHeight(), r.height());
    }
    List<String> textIssues = new ArrayList<>();
    for (View v : all(root))
      if (v instanceof TextView && v.getVisibility() == View.VISIBLE) {
        TextView t = (TextView) v;
        String s = t.getText().toString();
        if (s.isEmpty()) continue;
        Layout l = t.getLayout();
        assertNotNull(label + ": no layout for " + s, l);
        int availW = t.getWidth() - t.getCompoundPaddingLeft() - t.getCompoundPaddingRight(),
            availH = t.getHeight() - t.getCompoundPaddingTop() - t.getCompoundPaddingBottom();
        assertTrue(label + ": text has no available space: " + s, availW > 0 && availH > 0);
        if (l.getHeight() > availH + 2)
          textIssues.add("vertical " + t.getTag() + " [" + s + "] " + l.getHeight() + ">" + availH);
        for (int n = 0; n < l.getLineCount(); n++) {
          if (l.getEllipsisCount(n) > 0) textIssues.add("ellipsis " + t.getTag() + " [" + s + "]");
          if (l.getLineWidth(n) > availW + 2)
            textIssues.add(
                "horizontal " + t.getTag() + " [" + s + "] " + l.getLineWidth(n) + ">" + availW);
        }
      }
    assertTrue(label + ": text clipping " + textIssues, textIssues.isEmpty());
    screenshot(label, root);
  }

  private void screenshot(String label, View root) throws Exception {
    Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(b));
    File dir = new File(System.getProperty("qa.output"));
    dir.mkdirs();
    try (FileOutputStream f = new FileOutputStream(new File(dir, label + ".png"))) {
      b.compress(Bitmap.CompressFormat.PNG, 100, f);
    }
    b.recycle();
  }

  private void invoke(String name) throws Exception {
    Method m = MainActivity.class.getDeclaredMethod(name);
    m.setAccessible(true);
    m.invoke(a);
    layout();
  }

  private Object field(String name) throws Exception {
    Field f = MainActivity.class.getDeclaredField(name);
    f.setAccessible(true);
    return f.get(a);
  }

  @Test
  public void layoutProfiles() throws Exception {
    FixedActivity.time = Instant.parse("2026-10-17T08:00:00Z");
    Object[][] profiles = {
      {360, 800, 1f, "light", "green"},
      {412, 892, 1f, "light", "green"},
      {320, 480, 1f, "light", "green"},
      {320, 480, 2f, "dark", "navy"},
      {360, 640, 1.3f, "dark", "green"},
      {640, 360, 1f, "light", "sand"},
      {640, 360, 2f, "dark", "sand"},
      {412, 915, 1f, "light", "navy"},
      {393, 852, 1f, "dark", "green"}
    };
    List<String> failures = new ArrayList<>();
    for (boolean ar : new boolean[] {false, true})
      for (Object[] p : profiles) {
        String label =
            (ar ? "ar" : "en") + "-" + p[0] + "x" + p[1] + "-font" + p[2] + "-" + p[3] + "-" + p[4];
        launch(ar, (int) p[0], (int) p[1], (float) p[2], (String) p[3], (String) p[4]);
        try {
          verify(ar, label);
          System.out.println("PASS profile " + label);
        } catch (AssertionError failure) {
          failures.add(failure.getMessage());
          System.out.println("FAIL profile " + failure.getMessage());
        }
      }
    assertTrue(failures.toString(), failures.isEmpty());
  }

  @Test
  public void phaseAndDateBoundaries() throws Exception {
    FixedActivity.time = Instant.parse("2026-10-17T08:00:00Z");
    for (boolean ar : new boolean[] {false, true}) {
      launch(ar, 360, 800, 1f, "light", "green");
      Schedule schedule = AlertScheduler.schedule(a);
      LocalDate day = LocalDate.of(2026, 10, 17);
      LocalTime[] times = schedule.days.get(day);
      for (int p : Schedule.PRAYERS) {
        Instant adhan = day.atTime(times[p]).atZone(Schedule.ZONE).toInstant();
        int offset = AppPreferences.iqamahMinutes(a)[p];
        for (long delta : new long[] {-1, 0, offset * 60L - 1, offset * 60L}) {
          FixedActivity.time = adhan.plusSeconds(delta);
          invoke("updateClock");
          PrayerPhase expected = AppPreferences.phase(a, schedule, FixedActivity.time);
          TextView phase = (TextView) field("phaseLabel");
          if (expected != null) {
            int ordinal = 0;
            while (Schedule.PRAYERS[ordinal] != expected.index) ordinal++;
            assertEquals(
                (ar ? AR : EN)[ordinal], ((TextView) tag("next_name")).getText().toString());
            assertEquals(
                AppPreferences.time(a, expected.targetAt.toLocalTime()),
                ((TextView) tag("next_time")).getText().toString());
          }
          assertEquals(
              expected != null && expected.iqamah,
              phase
                  .getText()
                  .toString()
                  .toLowerCase(java.util.Locale.ROOT)
                  .contains(ar ? "الإقامة" : "iqamah"));
          verify(ar, (ar ? "ar" : "en") + "-phase-" + p + "-" + delta);
        }
      }
      FixedActivity.time = Instant.parse("2026-10-17T19:59:59Z");
      invoke("updateClock");
      assertEquals(LocalDate.of(2026, 10, 17), field("selected"));
      FixedActivity.time = Instant.parse("2026-10-17T20:00:00Z");
      invoke("updateClock");
      assertEquals(LocalDate.of(2026, 10, 18), field("selected"));
      View prev = desc(ar ? "اليوم السابق" : "Previous day");
      assertNotNull(prev);
      prev.performClick();
      assertEquals(LocalDate.of(2026, 10, 17), field("selected"));
      View today = desc(ar ? "العودة إلى اليوم" : "Return to today");
      assertNotNull(today);
      today.performClick();
      assertEquals(LocalDate.of(2026, 10, 18), field("selected"));
      System.out.println("PASS phase transitions and date controls " + (ar ? "Arabic" : "English"));
    }
  }

  @Test
  public void settingsAndRecreation() throws Exception {
    FixedActivity.time = Instant.parse("2026-10-17T08:00:00Z");
    for (boolean ar : new boolean[] {false, true}) {
      launch(ar, 360, 800, 1f, "light", "green");
      View menu = desc(ar ? "الإعدادات والمزيد" : "Settings and more");
      assertNotNull(menu);
      menu.performClick();
      AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
      assertNotNull(dialog);
      assertTrue(dialog.isShowing());
      assertTrue(dialog.getListView().getAdapter().getCount() >= 9);
      dialog.dismiss();
      for (String method :
          new String[] {
            "appearanceSettings",
            "displaySettings",
            "widgetSettings",
            "reminderSettings",
            "iqamahSettings",
            "about"
          }) {
        invoke(method);
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(method, dialog);
        assertTrue(method, dialog.isShowing());
        dialog.dismiss();
      }
      AppPreferences.prefs(a)
          .edit()
          .putString("appearance_mode", "dark")
          .putString("accent", "navy")
          .putBoolean("twelve_hour", true)
          .putBoolean("eastern_digits", ar)
          .commit();
      controller.recreate();
      a = controller.get();
      verify(ar, (ar ? "ar" : "en") + "-settings-recreation");
      assertTrue(AppPreferences.palette(a).dark);
      assertTrue(AppPreferences.twelve(a));
      assertEquals(ar, AlertScheduler.arabic(a));
      tag("language").performClick();
      assertEquals(!ar, AlertScheduler.arabic(a));
      tag("language").performClick();
      assertEquals(ar, AlertScheduler.arabic(a));
      System.out.println("PASS settings and recreation " + (ar ? "Arabic" : "English"));
    }
  }

  @Test
  @Config(sdk = {26, 29})
  public void olderAndroidStartup() throws Exception {
    FixedActivity.time = Instant.parse("2026-10-17T08:00:00Z");
    for (boolean ar : new boolean[] {false, true}) {
      launch(ar, 360, 800, 1f, "light", "green");
      verify(ar, (ar ? "ar" : "en") + "-startup-api" + Build.VERSION.SDK_INT);
      System.out.println(
          "PASS startup API " + Build.VERSION.SDK_INT + " " + (ar ? "Arabic" : "English"));
    }
  }

  @Test
  public void expiryAndDisplayPreferences() throws Exception {
    FixedActivity.time = Instant.parse("2026-10-17T08:00:00Z");
    for (boolean ar : new boolean[] {false, true}) {
      launch(ar, 360, 800, 1f, "light", "green");
      AppPreferences.prefs(a)
          .edit()
          .putBoolean("countdown_seconds", false)
          .putBoolean("eastern_digits", ar)
          .putBoolean("twelve_hour", true)
          .commit();
      invoke("render");
      assertEquals(2, ((ViewGroup) tag("countdown_units")).getChildCount());
      String timer = ((TextView) tag("countdown")).getText().toString();
      assertEquals(1, timer.chars().filter(c -> c == ':').count());
      verify(ar, (ar ? "ar" : "en") + "-two-unit-countdown");
      FixedActivity.time = Instant.parse("2027-12-31T19:59:59Z");
      invoke("updateClock");
      assertEquals(LocalDate.of(2027, 12, 31), field("selected"));
      assertFalse(tag("next_day").isEnabled());
      FixedActivity.time = Instant.parse("2027-12-31T20:00:00Z");
      invoke("updateClock");
      assertNull(tag("prayer_0"));
      assertEquals(
          ar ? "غير متاح" : "Unavailable", ((TextView) tag("next_name")).getText().toString());
      screenshot((ar ? "ar" : "en") + "-expiry", layout());
      tag("previous_day").performClick();
      assertEquals(LocalDate.of(2027, 12, 31), field("selected"));
      verify(ar, (ar ? "ar" : "en") + "-expiry-browse-last-day");
      System.out.println("PASS expiry and numeral/unit preferences " + (ar ? "Arabic" : "English"));
      FixedActivity.time = Instant.parse("2026-10-17T08:00:00Z");
    }
  }

  @Test
  public void figmaComparisonAndSystemInsets() throws Exception {
    FixedActivity.time = Instant.parse("2026-10-17T08:00:00Z");
    for (boolean ar : new boolean[] {false, true}) {
      launch(ar, 412, 892, 1f, "light", "green");
      AppPreferences.prefs(a)
          .edit()
          .putBoolean("eastern_digits", ar)
          .putBoolean("twelve_hour", true)
          .commit();
      invoke("render");
      verify(ar, (ar ? "ar" : "en") + "-figma-412x892");
      AppPreferences.prefs(a).edit().putBoolean(AlertScheduler.ENABLED, true).commit();
      invoke("render");
      assertTrue(((Switch) tag("alerts_toggle")).isChecked());
      verify(ar, (ar ? "ar" : "en") + "-figma-alert-on-412x892");
      tag("alerts_toggle").performClick();
      assertFalse(AlertScheduler.enabled(a));
      assertFalse(((Switch) tag("alerts_toggle")).isChecked());
      simulateInsets = true;
      verify(ar, (ar ? "ar" : "en") + "-system-insets-412x892");
      simulateInsets = false;
      launch(ar, 320, 480, 1f, "light", "green");
      simulateInsets = true;
      verify(ar, (ar ? "ar" : "en") + "-system-insets-320x480");
      simulateInsets = false;
    }
  }

  private void chooseDialogItem(int index) {
    AlertDialog d = ShadowAlertDialog.getLatestAlertDialog();
    assertNotNull(d);
    ListView list = d.getListView();
    assertNotNull(list);
    list.performItemClick(list.getAdapter().getView(index, null, list), index, index);
    Shadows.shadowOf(Looper.getMainLooper()).idle();
  }

  @Test
  public void calendarAndAppearanceInteractions() throws Exception {
    FixedActivity.time = Instant.parse("2026-10-17T08:00:00Z");
    for (boolean ar : new boolean[] {false, true}) {
      launch(ar, 360, 800, 1f, "light", "green");
      tag("date_picker").performClick();
      DatePickerDialog picker = (DatePickerDialog) ShadowDialog.getLatestDialog();
      assertTrue(picker.isShowing());
      picker.getDatePicker().updateDate(2027, 11, 31);
      picker.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
      layout();
      assertEquals(LocalDate.of(2027, 12, 31), field("selected"));
      assertFalse(tag("next_day").isEnabled());
      tag("today").performClick();
      assertEquals(LocalDate.of(2026, 10, 17), field("selected"));
      invoke("appearanceSettings");
      chooseDialogItem(0);
      chooseDialogItem(2);
      assertEquals("dark", AppPreferences.prefs(a).getString("appearance_mode", ""));
      ShadowAlertDialog.getLatestAlertDialog().dismiss();
      assertTrue(AppPreferences.palette(a).dark);
      invoke("appearanceSettings");
      chooseDialogItem(1);
      chooseDialogItem(1);
      assertEquals("navy", AppPreferences.prefs(a).getString("accent", ""));
      ShadowAlertDialog.getLatestAlertDialog().dismiss();
      verify(ar, (ar ? "ar" : "en") + "-actual-settings-dark-navy");
      invoke("appearanceSettings");
      chooseDialogItem(1);
      chooseDialogItem(2);
      assertEquals("sand", AppPreferences.prefs(a).getString("accent", ""));
      ShadowAlertDialog.getLatestAlertDialog().dismiss();
      verify(ar, (ar ? "ar" : "en") + "-actual-settings-dark-sand");
      System.out.println(
          "PASS actual calendar and appearance choices " + (ar ? "Arabic" : "English"));
    }
  }

  @Test
  public void narrowIqamahLayouts() throws Exception {
    for (boolean ar : new boolean[] {false, true}) {
      for (int[] size : new int[][] {{320, 480}, {360, 640}, {412, 892}}) {
        FixedActivity.time = Instant.parse("2026-10-17T08:10:00Z");
        launch(ar, size[0], size[1], 1f, "light", "green");
        simulateInsets = size[0] == 320;
        verify(ar, (ar ? "ar" : "en") + "-iqamah-" + size[0] + "x" + size[1]);
        simulateInsets = false;
      }
    }
  }
}
