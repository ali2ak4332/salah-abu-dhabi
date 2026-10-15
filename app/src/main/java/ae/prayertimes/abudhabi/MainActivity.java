package ae.prayertimes.abudhabi;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.Manifest;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends Activity {
    private int BG, INK, MUTED, GOLD;
    private AppPreferences.Palette palette;
    private boolean configuringReminders;
    private Schedule schedule;
    private LocalDate selected, renderedToday;
    private boolean arabic, followToday = true;
    private LinearLayout content;
    private static final int NOTIFICATIONS_REQUEST = 12;
    private TextView nextName, countdown, nextTime, phaseLabel;
    private long lastNext = -2;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String[] EN = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private final String[] AR = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};
    private final Runnable ticker = new Runnable() {
        public void run() { updateClock(); handler.postDelayed(this, 1000); }
    };
    private String t(String en, String ar) { return arabic ? ar : en; }
    protected Instant now(){return Instant.now();}
    private LocalDate today() { return now().atZone(Schedule.ZONE).toLocalDate(); }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    @Override public void onCreate(Bundle state) {
        setTheme(AppPreferences.palette(this).dark?R.style.AppThemeDark:R.style.AppTheme);
        super.onCreate(state);
        applyPalette();
        configuringReminders=state!=null&&state.getBoolean("reminder_setup",false);
        arabic = AlertScheduler.prefs(this).getBoolean("arabic", Locale.getDefault().getLanguage().equals("ar"));
        try {
            schedule = AlertScheduler.schedule(this);
            AlertScheduler.createChannel(this);
            selected = today();
            if (state != null) { selected = LocalDate.parse(state.getString("date", selected.toString())); followToday = state.getBoolean("follow", true); }
            render();
        } catch (Exception e) {
            TextView error = new TextView(this); error.setText("Unable to read the saved timetable. Please reinstall the app."); error.setPadding(32,64,32,32); setContentView(error);
        }
    }
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); if(selected!=null)state.putString("date", selected.toString()); state.putBoolean("follow", followToday);state.putBoolean("reminder_setup",configuringReminders); }
    @Override protected void onResume() { super.onResume(); if(schedule!=null){AlertScheduler.reschedule(this);PrayerWidget.updateAll(this);render();handler.removeCallbacks(ticker);handler.post(ticker);} }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }
    private GradientDrawable background(int color, int radius) { GradientDrawable d = new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d; }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this);v.setText(AppPreferences.digits(this,s));v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.START);v.setFontFeatureSettings("tnum");
        if(bold)v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        return v;
    }
    private void space(LinearLayout parent, int size) { View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(size))); }
    private LinearLayout vertical() { LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v; }
    private LinearLayout horizontal() { LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v; }
    private Button button(String label, Runnable action) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(INK);b.setTextSize(14);b.setMaxLines(1);autosize(b,8,14);b.setMinHeight(dp(48));b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);b.setBackground(new RippleDrawable(ColorStateList.valueOf((palette.accent&0x00ffffff)|0x22000000),background(palette.surface,14),null));b.setOnClickListener(v->action.run());return b;
    }
    /** All main-screen children are bounded by the visible app window; no scrolling container. */
    private void render() {
        applyPalette();
        if(followToday)selected=today();
        renderedToday=today();lastNext=-2;
        FrameLayout viewport=new FrameLayout(this);viewport.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=35)viewport.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        boolean compact=!landscape&&getResources().getConfiguration().screenHeightDp<600;
        int gap=compact?4:6;
        content=landscape?horizontal():vertical();content.setPadding(dp(12),dp(6),dp(12),dp(6));content.setLayoutDirection(arabic?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        viewport.addView(content,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout overview=content, timetable=content;
        if(landscape){
            overview=vertical();timetable=vertical();
            content.addView(overview,new LinearLayout.LayoutParams(0,-1,.45f));
            LinearLayout.LayoutParams timetableParams=new LinearLayout.LayoutParams(0,-1,.55f);timetableParams.setMarginStart(dp(10));content.addView(timetable,timetableParams);
        }
        LinearLayout top=horizontal();LinearLayout title=vertical();
        TextView brand=fitText(t("Salah","الصلاة"),20,10,INK,true);title.addView(brand,new LinearLayout.LayoutParams(-1,0,1.3f));
        title.addView(fitText(t("Abu Dhabi · v1.6","أبوظبي · v1.6"),10,8,MUTED,false),new LinearLayout.LayoutParams(-1,0,1));
        top.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        Button language=button(arabic?"English":"العربية",()->{arabic=!arabic;AlertScheduler.prefs(this).edit().putBoolean("arabic",arabic).apply();PrayerWidget.updateAll(this);render();});autosize(language,8,12);top.addView(language,new LinearLayout.LayoutParams(dp(70),-1));
        Button menu=button("⋮",()->homeSettings());autosize(menu,12,25);menu.setContentDescription(t("Settings and more","الإعدادات والمزيد"));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(48),-1);mp.setMarginStart(dp(4));top.addView(menu,mp);overview.addView(top,new LinearLayout.LayoutParams(-1,dp(48)));space(overview,gap);
        LinearLayout hero=vertical();hero.setBackground(background(palette.hero,17));hero.setPadding(dp(14),dp(7),dp(14),dp(7));
        LinearLayout heroMain=horizontal();LinearLayout nameBox=vertical();phaseLabel=fitText(t("NEXT ADHAN","الأذان القادم"),10,8,GOLD,true);nameBox.addView(phaseLabel,new LinearLayout.LayoutParams(-1,0,1));
        nextName=fitText("",23,12,palette.heroText,true);nameBox.addView(nextName,new LinearLayout.LayoutParams(-1,0,2));heroMain.addView(nameBox,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout timerBox=vertical();TextView timeLabel=fitText(t("TIME REMAINING","الوقت المتبقي"),10,8,GOLD,true);timeLabel.setGravity(Gravity.CENTER);timerBox.addView(timeLabel,new LinearLayout.LayoutParams(-1,0,1));
        countdown=fitText("",27,12,palette.heroText,true);countdown.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);countdown.setGravity(Gravity.CENTER);timerBox.addView(countdown,new LinearLayout.LayoutParams(-1,0,2));heroMain.addView(timerBox,new LinearLayout.LayoutParams(0,-1,1.3f));hero.addView(heroMain,new LinearLayout.LayoutParams(-1,0,1));
        nextTime=fitText("",11,8,GOLD,false);hero.addView(nextTime,new LinearLayout.LayoutParams(-1,dp(17)));overview.addView(hero,new LinearLayout.LayoutParams(-1,dp(compact?72:82)));if(!landscape)space(overview,gap);
        LinearLayout controls=horizontal();
        Button prev=button(arabic?"›":"‹",()->{selected=schedule.clamp(selected.minusDays(1));followToday=false;render();});prev.setContentDescription(t("Previous day","اليوم السابق"));prev.setEnabled(selected.isAfter(schedule.days.firstKey()));prev.setAlpha(prev.isEnabled()?1f:.35f);
        Button date=button(AppPreferences.date(this,selected,"d MMM yyyy"),()->pickDate());date.setHorizontallyScrolling(false);autosize(date,9,13);date.setContentDescription(t("Choose date: ","اختيار التاريخ: ")+selected.toString());
        Button todayButton=button(t("Today","اليوم"),()->{selected=today();followToday=true;render();});autosize(todayButton,8,12);
        Button next=button(arabic?"‹":"›",()->{selected=schedule.clamp(selected.plusDays(1));followToday=false;render();});next.setContentDescription(t("Next day","اليوم التالي"));next.setEnabled(selected.isBefore(schedule.days.lastKey()));next.setAlpha(next.isEnabled()?1f:.35f);
        controls.addView(prev,new LinearLayout.LayoutParams(dp(48),-1));LinearLayout.LayoutParams dateParams=new LinearLayout.LayoutParams(0,-1,1);dateParams.setMargins(dp(4),0,dp(4),0);controls.addView(date,dateParams);controls.addView(todayButton,new LinearLayout.LayoutParams(dp(54),-1));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(dp(48),-1);np.setMarginStart(dp(4));controls.addView(next,np);timetable.addView(controls,new LinearLayout.LayoutParams(-1,dp(48)));space(timetable,gap);
        LocalTime[] times=schedule.days.get(selected);PrayerPhase upcoming=AppPreferences.phase(this,schedule,now());
        LinearLayout grid=vertical();
        if(times==null){TextView missing=text(t("No timetable for this date. Choose 11 Oct 2026–10 Oct 2027 from the calendar.","لا يوجد جدول لهذا التاريخ. اختر من التقويم تاريخاً بين ١١ أكتوبر ٢٠٢٦ و١٠ أكتوبر ٢٠٢٧."),17,INK,true);missing.setGravity(Gravity.CENTER);grid.addView(missing,new LinearLayout.LayoutParams(-1,-1));}
        else {
            for(int row=0;row<Schedule.PRAYERS.length;row++){
                int i=Schedule.PRAYERS[row];boolean highlighted=upcoming!=null&&selected.equals(upcoming.adhanAt.toLocalDate())&&i==upcoming.index;
                LinearLayout tile=horizontal();tile.setPadding(dp(12),dp(compact?2:4),dp(12),dp(compact?2:4));
                GradientDrawable tileBg=background(highlighted?palette.tint:palette.surface,14);if(highlighted)tileBg.setStroke(dp(1),palette.border);tile.setBackground(tileBg);
                TextView name=fitText(arabic?AR[i]:EN[i],19,8,INK,true);name.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);tile.addView(name,new LinearLayout.LayoutParams(0,-1,1));
                TextView prayerTime=fitText(AppPreferences.time(this,times[i]),28,12,INK,true);prayerTime.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);prayerTime.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);tile.addView(prayerTime,new LinearLayout.LayoutParams(0,-1,1.3f));
                tile.setContentDescription((arabic?AR[i]:EN[i])+t(" adhan "," أذان ")+AppPreferences.time(this,times[i])+(highlighted?(upcoming.iqamah?t(", estimated iqamah countdown", "، عد تنازلي للإقامة التقديرية"):t(", next prayer", "، الصلاة القادمة")):""));
                LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,0,1);if(row>0)rowParams.topMargin=dp(compact?4:6);grid.addView(tile,rowParams);
            }
        }
        timetable.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        if(landscape)overview.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));else space(overview,gap);
        addAlertCard(overview);space(overview,3);
        TextView coverage=fitText(times==null?t("UAE time","توقيت الإمارات"):t("Sunrise · ","الشروق · ")+AppPreferences.time(this,times[1])+t(" · UAE time"," · توقيت الإمارات"),10,8,MUTED,false);coverage.setGravity(Gravity.CENTER);overview.addView(coverage,new LinearLayout.LayoutParams(-1,dp(compact?14:16)));
        setContentView(viewport);viewport.requestApplyInsets();updateClockOnly();
    }
    private void autosize(TextView view,int minimumDp,int maximumSp){
        // Keep the preferred size accessible, but allow text to fit its fixed visible slot.
        int maximumPx=Math.round(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,maximumSp,getResources().getDisplayMetrics()));
        view.setAutoSizeTextTypeUniformWithConfiguration(dp(minimumDp),Math.max(dp(minimumDp)+1,maximumPx),Math.max(1,dp(1)),android.util.TypedValue.COMPLEX_UNIT_PX);
    }
    private TextView fitText(String value,int maximum,int minimum,int color,boolean bold){
        TextView v=text(value,maximum,color,bold);v.setIncludeFontPadding(false);v.setMaxLines(1);autosize(v,minimum,maximum);v.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);return v;
    }
    private void pickDate(){
        LocalDate base=schedule.clamp(selected);
        DatePickerDialog dialog=new DatePickerDialog(dialogContext(),(view,y,m,d)->{selected=LocalDate.of(y,m+1,d);followToday=false;render();},base.getYear(),base.getMonthValue()-1,base.getDayOfMonth());
        // DatePicker uses the device zone; date bounds represent calendar dates there.
        dialog.getDatePicker().setMinDate(schedule.days.firstKey().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.getDatePicker().setMaxDate(schedule.days.lastKey().atTime(23,59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());dialog.show();
    }
    private long phaseKey(PrayerPhase phase){return phase==null?-1:phase.targetAt.toEpochSecond()*16+phase.index*2+(phase.iqamah?1:0);}
    private void updateClock(){
        if(!today().equals(renderedToday)){if(followToday)selected=today();render();return;}
        PrayerPhase phase=AppPreferences.phase(this,schedule,now());long key=phaseKey(phase);
        if(lastNext!=-2&&key!=lastNext){render();return;}updateClockOnly();
    }
    private void updateClockOnly(){
        if(nextName==null)return;Instant instant=now();PrayerPhase phase=AppPreferences.phase(this,schedule,instant);
        lastNext=phaseKey(phase);
        if(phase==null){phaseLabel.setText(t("SAVED TIMETABLE","الجدول المحفوظ"));nextName.setText(t("Timetable unavailable","الجدول غير متاح"));countdown.setText("—");nextTime.setText(AppPreferences.digits(this,t("Saved period: 11 Oct 2026 – 10 Oct 2027","الفترة المحفوظة: ١١ أكتوبر ٢٠٢٦ – ١٠ أكتوبر ٢٠٢٧")));return;}
        phaseLabel.setText(phase.iqamah?t("EST. IQAMAH","الإقامة التقديرية"):t("NEXT ADHAN","الأذان القادم"));
        nextName.setText(arabic?AR[phase.index]:EN[phase.index]);long sec=Math.max(0,(Duration.between(instant,phase.targetAt.toInstant()).toMillis()+999)/1000);
        countdown.setText(AppPreferences.countdown(this,sec));
        nextTime.setText(phase.iqamah?t("Adhan ","الأذان ")+AppPreferences.time(this,phase.adhanAt.toLocalTime())+t(" · Est. iqamah "," · الإقامة التقديرية ")+AppPreferences.time(this,phase.targetAt.toLocalTime())
            :(phase.targetAt.toLocalDate().isAfter(today())?t("Tomorrow","غداً"):t("Today","اليوم"))+" · "+AppPreferences.time(this,phase.targetAt.toLocalTime())+" · "+t("UAE time","توقيت الإمارات"));
    }
    private void addAlertCard(LinearLayout parent){
        LinearLayout card=horizontal();card.setPadding(dp(10),0,dp(8),0);card.setBackground(background(palette.tint,14));
        LinearLayout label=vertical();label.addView(fitText(t("Prayer chime","تنبيه الصلاة"),13,10,INK,true),new LinearLayout.LayoutParams(-1,0,1));
        String status;
        if(!AlertScheduler.enabled(this))status=t("Off · tap switch to enable","متوقف · اضغط المفتاح للتفعيل");
        else if(!AlertScheduler.notificationsAllowed(this))status=t("Allow notifications · tap here","اسمح بالإشعارات · اضغط هنا");
        else if(AlertScheduler.prefs(this).getBoolean("schedule_error",false))status=t("Setup needs attention · tap here","راجع الإعدادات · اضغط هنا");
        else if(schedule.next(now())==null)status=t("No upcoming saved prayer","لا صلاة قادمة في الجدول");
        else if(!AlertScheduler.precise(this))status=t("Allow precise timing · tap here","اسمح بالتوقيت الدقيق · اضغط هنا");
        else if(!AlertScheduler.soundEnabled(this))status=t("On · phone sound is muted","مفعّل · صوت الهاتف مكتوم");
        else status=t("On · five daily prayers","مفعّل · الصلوات الخمس");
        label.addView(fitText(status,10,8,MUTED,false),new LinearLayout.LayoutParams(-1,0,1));label.setPadding(0,dp(5),0,dp(5));card.addView(label,new LinearLayout.LayoutParams(0,-1,1));
        label.setOnClickListener(v->alertSettings());label.setContentDescription(t("Prayer alert settings. ","إعدادات تنبيهات الصلاة. ")+status);
        Switch toggle=new Switch(this);toggle.setContentDescription(t("Enable prayer chime alerts","تفعيل تنبيهات الصلاة"));toggle.setMinHeight(dp(48));toggle.setMinimumWidth(dp(48));toggle.setChecked(AlertScheduler.enabled(this));toggle.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{palette.accent,palette.muted}));
        toggle.setOnCheckedChangeListener((button,on)->{AlertScheduler.prefs(this).edit().putBoolean(AlertScheduler.ENABLED,on).apply();AlertScheduler.reschedule(this);if(on)configureAlerts();else render();});card.addView(toggle,new LinearLayout.LayoutParams(-2,-1));parent.addView(card,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private void testChime(){boolean sent=AlertScheduler.test(this);Toast.makeText(this,sent?t("Test sent. Check notification volume if silent.","أُرسل الاختبار. تحقق من صوت الإشعارات إن لم تسمعه."):t("Allow notifications in Settings first.","اسمح بالإشعارات في الإعدادات أولاً."),Toast.LENGTH_LONG).show();}
    private void homeSettings(){
        dialog().setTitle(t("Settings & more","الإعدادات والمزيد")).setItems(new String[]{
            t("Appearance","المظهر"),t("Time & numerals","الوقت والأرقام"),t("Widget style","مظهر الأدوات"),t("Advance reminders","التذكير قبل الصلاة"),t("Estimated iqamah","الإقامة التقديرية"),
            t("Prayer alert settings","إعدادات تنبيهات الصلاة"),t("Test chime","تجربة النغمة"),t("Add a home-screen widget","إضافة أداة للشاشة الرئيسية"),t("Source & app information","المصدر ومعلومات التطبيق")},
            (d,w)->{if(w==0)appearanceSettings();else if(w==1)displaySettings();else if(w==2)widgetSettings();else if(w==3)reminderSettings();else if(w==4)iqamahSettings();else if(w==5)alertSettings();else if(w==6)testChime();else if(w==7)chooseWidget();else about();}).show();
    }
    private void applyPalette(){
        palette=AppPreferences.palette(this);BG=palette.bg;INK=palette.text;MUTED=palette.muted;GOLD=palette.heroMuted;
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(palette.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    private Context dialogContext(){
        int style;
        String family=AppPreferences.prefs(this).getString("accent","green");
        if("navy".equals(family))style=palette.dark?R.style.DialogNavyDark:R.style.DialogNavyLight;
        else if("sand".equals(family))style=palette.dark?R.style.DialogSandDark:R.style.DialogSandLight;
        else style=palette.dark?R.style.DialogGreenDark:R.style.DialogGreenLight;
        android.content.res.Configuration configuration=new android.content.res.Configuration(getResources().getConfiguration());
        configuration.setLocale(arabic?new Locale("ar"):Locale.ENGLISH);
        ContextThemeWrapper themed=new ContextThemeWrapper(this,style);
        themed.applyOverrideConfiguration(configuration);return themed;
    }
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(dialogContext());}
    private String prefLabel(String key,String fallback,String[] values,String[] labels){
        String current=AppPreferences.prefs(this).getString(key,fallback);
        for(int i=0;i<values.length;i++)if(values[i].equals(current))return labels[i];
        return labels[0];
    }
    private void preferencesChanged(){PrayerWidget.updateAll(this);render();}
    private void choice(String title,String key,String fallback,String[] values,String[] labels,Runnable back){
        String current=AppPreferences.prefs(this).getString(key,fallback);int checked=0;
        for(int i=0;i<values.length;i++)if(values[i].equals(current))checked=i;
        dialog().setTitle(title).setSingleChoiceItems(labels,checked,(d,w)->{AppPreferences.prefs(this).edit().putString(key,values[w]).apply();d.dismiss();preferencesChanged();back.run();}).setNegativeButton(t("Back","رجوع"),(d,w)->back.run()).show();
    }
    private void boolChoice(String title,String key,boolean fallback,String falseLabel,String trueLabel,Runnable back){
        dialog().setTitle(title).setSingleChoiceItems(new String[]{falseLabel,trueLabel},AppPreferences.prefs(this).getBoolean(key,fallback)?1:0,(d,w)->{AppPreferences.prefs(this).edit().putBoolean(key,w==1).apply();d.dismiss();preferencesChanged();back.run();}).setNegativeButton(t("Back","رجوع"),(d,w)->back.run()).show();
    }
    private void appearanceSettings(){
        String[] modes={"system","light","dark"},modeLabels={t("Follow phone","حسب الهاتف"),t("Light","فاتح"),t("Dark","داكن")};
        String[] colors={"green","navy","sand"},colorLabels={t("Green","أخضر"),t("Navy","كحلي"),t("Sand","رملي")};
        dialog().setTitle(t("Appearance","المظهر")).setItems(new String[]{t("Display mode · ","وضع العرض · ")+prefLabel("appearance_mode","system",modes,modeLabels),t("Colour theme · ","اللون · ")+prefLabel("accent","green",colors,colorLabels)},
            (d,w)->{if(w==0)choice(t("Display mode","وضع العرض"),"appearance_mode","system",modes,modeLabels,()->appearanceSettings());else choice(t("Colour theme","اللون"),"accent","green",colors,colorLabels,()->appearanceSettings());}).setNegativeButton(t("Back","رجوع"),(d,w)->homeSettings()).show();
    }
    private void displaySettings(){
        dialog().setTitle(t("Time & numerals","الوقت والأرقام")).setItems(new String[]{
            t("Time format · ","تنسيق الوقت · ")+(AppPreferences.twelve(this)?t("12-hour","١٢ ساعة"):t("24-hour","٢٤ ساعة")),
            t("Numerals · ","الأرقام · ")+(AppPreferences.eastern(this)?"١٢٣":"123"),
            t("Countdown seconds · ","ثواني العد التنازلي · ")+(AppPreferences.prefs(this).getBoolean("countdown_seconds",true)?t("Show","إظهار"):t("Hide","إخفاء"))},
            (d,w)->{if(w==0)boolChoice(t("Time format","تنسيق الوقت"),"twelve_hour",false,t("24-hour","٢٤ ساعة"),t("12-hour","١٢ ساعة"),()->displaySettings());
            else if(w==1)boolChoice(t("Numerals","الأرقام"),"eastern_digits",false,"123","١٢٣",()->displaySettings());
            else boolChoice(t("Countdown seconds","ثواني العد التنازلي"),"countdown_seconds",true,t("Hide","إخفاء"),t("Show","إظهار"),()->displaySettings());}).setNegativeButton(t("Back","رجوع"),(d,w)->homeSettings()).show();
    }
    private void widgetSettings(){
        String[] modes={"match","light","dark"},modeLabels={t("Match app","مثل التطبيق"),t("Light","فاتح"),t("Dark","داكن")};
        String[] colors={"match","green","navy","sand"},colorLabels={t("Match app","مثل التطبيق"),t("Green","أخضر"),t("Navy","كحلي"),t("Sand","رملي")};
        String[] sizes={"small","normal","large"},sizeLabels={t("Small","صغير"),t("Normal","عادي"),t("Large","كبير")};
        dialog().setTitle(t("Widget style · both sizes","مظهر الأدوات · كلا الحجمين")).setItems(new String[]{
            t("Background · ","الخلفية · ")+prefLabel("widget_mode","dark",modes,modeLabels),t("Colour · ","اللون · ")+prefLabel("widget_accent","match",colors,colorLabels),
            t("Opacity · ","التعتيم · ")+AppPreferences.digits(this,AppPreferences.prefs(this).getInt("widget_opacity",100)+"%"),t("Text size · ","حجم النص · ")+prefLabel("widget_text","normal",sizes,sizeLabels)},
            (d,w)->{if(w==0)choice(t("Widget background","خلفية الأداة"),"widget_mode","dark",modes,modeLabels,()->widgetSettings());
            else if(w==1)choice(t("Widget colour","لون الأداة"),"widget_accent","match",colors,colorLabels,()->widgetSettings());
            else if(w==2){int[] opacities={100,85,65,0};int selectedOpacity=AppPreferences.prefs(this).getInt("widget_opacity",100),selectedIndex=0;for(int i=0;i<opacities.length;i++)if(opacities[i]==selectedOpacity)selectedIndex=i;
                dialog().setTitle(t("Background opacity","تعتيم الخلفية")).setSingleChoiceItems(new String[]{"100%","85%","65%",t("Transparent","شفاف")},selectedIndex,(dlg,k)->{AppPreferences.prefs(this).edit().putInt("widget_opacity",opacities[k]).apply();dlg.dismiss();preferencesChanged();widgetSettings();}).setNegativeButton(t("Back","رجوع"),(dlg,k)->widgetSettings()).show();}
            else choice(t("Widget text size","حجم نص الأداة"),"widget_text","normal",sizes,sizeLabels,()->widgetSettings());}).setNegativeButton(t("Back","رجوع"),(d,w)->homeSettings()).show();
    }
    private void reminderSettings(){
        int minutes=AppPreferences.prefs(this).getInt("reminder_minutes",0),mask=AppPreferences.prefs(this).getInt("reminder_mask",61);
        int count=0;for(int i:Schedule.PRAYERS)if((mask&(1<<i))!=0)count++;
        String status=minutes==0?t("Off","متوقف"):AppPreferences.digits(this,minutes+t(" minutes before"," دقيقة قبل الصلاة"));
        dialog().setTitle(t("Advance reminders","التذكير قبل الصلاة")).setItems(new String[]{
            t("Reminder time · ","موعد التذكير · ")+status,t("Selected prayers · ","الصلوات المختارة · ")+AppPreferences.digits(this,String.valueOf(count)),
            t("Notification sound & permissions","صوت الإشعارات والأذونات"),t("How reminders work","كيف يعمل التذكير")},(d,w)->{
            if(w==0){int[] leads={0,5,10,15};int checked=0;for(int i=0;i<leads.length;i++)if(leads[i]==minutes)checked=i;
                dialog().setTitle(t("Remind me","ذكّرني")).setSingleChoiceItems(new String[]{t("Off","إيقاف"),t("5 minutes before","قبل ٥ دقائق"),t("10 minutes before","قبل ١٠ دقائق"),t("15 minutes before","قبل ١٥ دقيقة")},checked,(dlg,k)->{AppPreferences.prefs(this).edit().putInt("reminder_minutes",leads[k]).apply();dlg.dismiss();AlertScheduler.reschedule(this);render();if(leads[k]>0)configureAlerts(true);else reminderSettings();}).setNegativeButton(t("Back","رجوع"),(dlg,k)->reminderSettings()).show();
            }else if(w==1){boolean[] selectedPrayers=new boolean[5];String[] names=new String[5];for(int i=0;i<5;i++){int index=Schedule.PRAYERS[i];selectedPrayers[i]=(mask&(1<<index))!=0;names[i]=arabic?AR[index]:EN[index];}
                dialog().setTitle(t("Reminder prayers","صلوات التذكير")).setMultiChoiceItems(names,selectedPrayers,(dlg,k,on)->selectedPrayers[k]=on).setPositiveButton(t("Save","حفظ"),(dlg,k)->{int chosen=0;for(int i=0;i<5;i++)if(selectedPrayers[i])chosen|=1<<Schedule.PRAYERS[i];AppPreferences.prefs(this).edit().putInt("reminder_mask",chosen).apply();AlertScheduler.reschedule(this);render();reminderSettings();}).setNegativeButton(t("Cancel","إلغاء"),(dlg,k)->reminderSettings()).show();
            }else if(w==2)notificationSettings(true);
            else dialog().setTitle(t("Advance reminders","التذكير قبل الصلاة")).setMessage(t("Optional reminders arrive before your selected prayers. They are separate from the prayer-time chime and do not change Awqaf times.\n\nAllow notifications and precise timing. Battery saving may delay or skip advance reminders. Reminders more than 2 minutes late, or at/after the prayer time, are skipped.\n\nSelecting no prayers pauses reminders. Sound follows the reminder notification settings and Do Not Disturb.","يُرسل تذكير اختياري قبل الصلوات التي تختارها، وهو مستقل عن تنبيه دخول الوقت ولا يغيّر مواقيت الأوقاف.\n\nاسمح بالإشعارات والتوقيت الدقيق. قد يؤخر توفير البطارية التذكير أو يمنعه. يُتجاوز التذكير إذا تأخر أكثر من دقيقتين، أو وصل عند دخول الوقت أو بعده.\n\nعدم اختيار أي صلاة يوقف التذكير مؤقتاً. يتبع الصوت إعدادات إشعار التذكير وعدم الإزعاج.")).setPositiveButton(t("Back","رجوع"),(dlg,k)->reminderSettings()).show();
        }).setNegativeButton(t("Back","رجوع"),(d,w)->homeSettings()).show();
    }
    private void iqamahChanged(){AlertScheduler.reschedule(this);PrayerWidget.updateAll(this);render();}
    private String iqamahInterval(int minutes){return minutes==0?t("Off","متوقف"):AppPreferences.digits(this,minutes+t(" min after adhan"," دقيقة بعد الأذان"));}
    private void iqamahSettings(){
        int[] minutes=AppPreferences.iqamahMinutes(this);String[] items=new String[8];
        items[0]=t("Countdown · ","العد التنازلي · ")+(AppPreferences.prefs(this).getBoolean("iqamah_enabled",true)?t("On","مفعّل"):t("Off","متوقف"));
        for(int i=0;i<Schedule.PRAYERS.length;i++){int index=Schedule.PRAYERS[i];items[i+1]=(arabic?AR[index]:EN[index])+" · "+iqamahInterval(minutes[index]);}
        items[6]=t("About these estimates","عن هذه التقديرات");
        items[7]=t("Read 2009 source · Al Bayan","قراءة مصدر ٢٠٠٩ · البيان");
        dialog().setTitle(t("Estimated iqamah","الإقامة التقديرية")).setItems(items,(d,w)->{
            if(w==0)dialog().setTitle(t("Iqamah countdown","العد التنازلي للإقامة")).setSingleChoiceItems(new String[]{t("Off","إيقاف"),t("On","تفعيل")},AppPreferences.prefs(this).getBoolean("iqamah_enabled",true)?1:0,(dlg,k)->{AppPreferences.prefs(this).edit().putBoolean("iqamah_enabled",k==1).apply();dlg.dismiss();iqamahChanged();iqamahSettings();}).setNegativeButton(t("Back","رجوع"),(dlg,k)->iqamahSettings()).show();
            else if(w<=5)iqamahOffset(Schedule.PRAYERS[w-1]);
            else if(w==6)iqamahInformation();else openIqamahSource();
        }).setNegativeButton(t("Back","رجوع"),(d,w)->homeSettings()).show();
    }
    private void iqamahOffset(int index){
        Context themed=dialogContext();LinearLayout box=new LinearLayout(themed);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(24),dp(8),dp(24),0);
        TextView hint=new TextView(themed);hint.setText(t("Minutes after adhan (0 = off). Use your mosque's interval.","دقائق بعد الأذان (٠ = إيقاف). اتبع مدة مسجدك."));hint.setTextColor(INK);box.addView(hint);
        NumberPicker picker=new NumberPicker(themed);picker.setMinValue(0);picker.setMaxValue(60);picker.setWrapSelectorWheel(false);picker.setFormatter(value->value==0?t("Off","متوقف"):AppPreferences.digits(this,String.valueOf(value)));picker.setValue(AppPreferences.iqamahMinutes(this)[index]);box.addView(picker,new LinearLayout.LayoutParams(-1,dp(150)));
        ScrollView scroll=new ScrollView(themed);scroll.addView(box);
        dialog().setTitle((arabic?AR[index]:EN[index])+t(" · interval"," · المدة")).setView(scroll).setPositiveButton(t("Save","حفظ"),(d,w)->{picker.clearFocus();AppPreferences.prefs(this).edit().putInt("iqamah_offset_"+index,picker.getValue()).apply();iqamahChanged();iqamahSettings();}).setNegativeButton(t("Cancel","إلغاء"),(d,w)->iqamahSettings()).show();
    }
    private void iqamahInformation(){
        dialog().setTitle(t("Estimated, mosque-adjustable","تقديرات قابلة للتعديل حسب المسجد")).setMessage(t(
            "The defaults come from an Awqaf official's statement reported by Al Bayan on 28 January 2009: Fajr 25, Dhuhr/Asr/Isha 20, and Maghrib 5 minutes. These intervals could not be confirmed in a current Awqaf publication. Follow your mosque and adjust each interval here.\n\nThe app counts from the saved adhan time to an estimated iqamah, then shows the next adhan. Friday Dhuhr is excluded because Jumuah has a separate schedule. Adhan times and chime alerts stay unchanged; there is no extra iqamah sound.\n\nBoth widgets show the estimated time and a live countdown with seconds. Its digits follow the phone's locale. Android may delay the switch at zero; a minus sign means the estimate has passed. Tap the widget to refresh.",
            "تستند المدد الافتراضية إلى تصريح لمسؤول في الأوقاف نقلته صحيفة البيان في ٢٨ يناير ٢٠٠٩: الفجر ٢٥ دقيقة، والظهر والعصر والعشاء ٢٠ دقيقة، والمغرب ٥ دقائق. لم نتمكن من تأكيد هذه المدد في منشور حديث للأوقاف. اتبع مسجدك وعدّل مدة كل صلاة هنا.\n\nيبدأ العد من موعد الأذان المحفوظ حتى الإقامة التقديرية، ثم يظهر الأذان القادم. يُستثنى ظهر الجمعة لأن صلاة الجمعة لها جدول مستقل. لا تتغير مواقيت الأذان أو تنبيهاته، ولا يصدر صوت إضافي للإقامة.\n\nتعرض الأداتان الموعد التقديري وعداً تنازلياً بالثواني، وتتبع أرقامه لغة الهاتف. قد يؤخر أندرويد التحديث عند الصفر؛ علامة السالب تعني مرور الموعد التقديري. اضغط الأداة للتحديث."))
            .setPositiveButton(t("Back","رجوع"),(d,w)->iqamahSettings()).show();
    }
    private void openIqamahSource(){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.albayan.ae/across-the-uae/2009-01-28-1.397198")));}catch(Exception e){Toast.makeText(this,t("Install a browser to open the source.","ثبّت متصفحاً لفتح المصدر."),Toast.LENGTH_LONG).show();}}
    private void configureAlerts(){configureAlerts(false);}
    private void configureAlerts(boolean reminders){
        configuringReminders=reminders;
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATIONS_REQUEST);return;}
        if(!(reminders?AlertScheduler.reminderNotificationsAllowed(this):AlertScheduler.notificationsAllowed(this))){notificationSettings(reminders);return;}
        if(!AlertScheduler.precise(this))dialog().setTitle(t("Prayer-time accuracy","دقة توقيت الصلاة")).setMessage(reminders?t("Allow Alarms & reminders for advance reminder timing. Battery saving may still delay or skip reminders; they are never delivered at or after the prayer time.","اسمح بالمنبهات والتذكيرات لضبط موعد التذكير. قد يؤخر توفير البطارية التذكير أو يمنعه؛ ولن يُرسل عند دخول وقت الصلاة أو بعده."):t("Allow Alarms & reminders so the chime can arrive at the saved prayer time while the app is closed. Without this, Android may delay it; alerts over 15 minutes late are skipped.","اسمح بالمنبهات والتذكيرات ليصل التنبيه في موعد الصلاة المحفوظ والتطبيق مغلق. دون ذلك قد يؤخره أندرويد، وتُتجاوز التنبيهات المتأخرة أكثر من ١٥ دقيقة.")).setPositiveButton(t("Allow precise timing","السماح بالتوقيت الدقيق"),(d,w)->preciseSettings()).setNegativeButton(t("Later","لاحقاً"),(d,w)->render()).show();
        AlertScheduler.reschedule(this);render();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==NOTIFICATIONS_REQUEST){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED&&AlertScheduler.anyAlertsEnabled(this))configureAlerts(configuringReminders);else{AlertScheduler.reschedule(this);render();}}}
    private void notificationSettings(){notificationSettings(false);}
    private void notificationSettings(boolean reminders){
        Intent i=new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,reminders?AlertScheduler.REMINDER_CHANNEL:AlertScheduler.CHANNEL);
        if(!getSystemService(NotificationManager.class).areNotificationsEnabled())i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());
        openSettings(i);
    }
    private void preciseSettings(){if(Build.VERSION.SDK_INT>=31)openSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));else Toast.makeText(this,t("Precise timing is already available.","التوقيت الدقيق متاح بالفعل."),Toast.LENGTH_SHORT).show();}
    private void openSettings(Intent i){try{startActivity(i);}catch(ActivityNotFoundException e){Toast.makeText(this,t("Open this app's settings on your phone.","افتح إعدادات هذا التطبيق على هاتفك."),Toast.LENGTH_LONG).show();}}
    private void alertSettings(){
        String[] items={t("Finish alert setup","إكمال إعداد التنبيهات"),t("Notification sound & permissions","صوت الإشعارات والأذونات"),t("Precise timing","التوقيت الدقيق"),t("How background alerts work","كيف تعمل التنبيهات في الخلفية")};
        dialog().setTitle(t("Prayer alerts","تنبيهات الصلاة")).setItems(items,(d,w)->{if(w==0){AlertScheduler.prefs(this).edit().putBoolean(AlertScheduler.ENABLED,true).apply();configureAlerts();}else if(w==1)notificationSettings();else if(w==2)preciseSettings();else dialog().setTitle(t("Background alerts","التنبيهات في الخلفية")).setMessage(t("The five prayers use a short notification chime, not full adhan. Sunrise is excluded. Sound follows your notification volume, channel settings and Do Not Disturb.\n\nAllow notifications and precise alarms. Schedules restore after a reboot (after unlocking). Force-stop cancels alerts until you reopen the app. Some phones also restrict background activity; use this app's battery settings if alerts are delayed.\n\nTimes stay fixed to Abu Dhabi. Timetable coverage ends 10 October 2027.","تستخدم الصلوات الخمس نغمة إشعار قصيرة دون أذان كامل، ولا يُرسل تنبيه للشروق. يتبع الصوت مستوى صوت الإشعارات وإعدادات القناة وعدم الإزعاج.\n\nاسمح بالإشعارات والمنبهات الدقيقة. تُستعاد الجدولة بعد إعادة التشغيل وفتح قفل الهاتف. الإيقاف الإجباري يلغي التنبيهات حتى تفتح التطبيق مجدداً. بعض الهواتف تقيد العمل في الخلفية؛ راجع إعدادات بطارية التطبيق إذا تأخرت التنبيهات.\n\nالمواقيت ثابتة لأبوظبي. ينتهي الجدول في ١٠ أكتوبر ٢٠٢٧.")).setPositiveButton(t("Got it","حسناً"),null).show();}).show();
    }
    private void chooseWidget(){dialog().setTitle(t("Add a widget","إضافة أداة")).setItems(new String[]{t("2 × 2 · Next prayer","٢ × ٢ · الصلاة القادمة"),t("2 × 3 · Today's five prayers","٢ × ٣ · صلوات اليوم الخمس")},(d,w)->{
        AppWidgetManager manager=getSystemService(AppWidgetManager.class);
        if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(this,w==0?PrayerWidget.class:PrayerWidgetTall.class),null,null);
        else dialog().setMessage(t("Long-press an empty area of your home screen → Widgets → Salah Abu Dhabi. Choose 2×2 or 2×3, then resize if needed. Sizes vary with your launcher's grid.","اضغط مطولاً على مساحة فارغة بالشاشة الرئيسية ← الأدوات ← Salah Abu Dhabi. اختر ٢×٢ أو ٢×٣ ثم عدّل الحجم عند الحاجة. تختلف الأبعاد حسب شبكة مشغّل الشاشة.")).setPositiveButton(t("Got it","حسناً"),null).show();
    }).show();}
    private void about(){dialog().setTitle(t("Salah v1.6 · Source & information","الصلاة v1.6 · المصدر والمعلومات")).setMessage(t(
        "Times: UAE General Authority of Islamic Affairs, Endowments & Zakat (Awqaf), Abu Dhabi. Retrieved 11 October 2026.\n\nAn independent personal app, not an official Awqaf app. Uses the 365-day timetable compiled in this conversation; no extrapolated adhan dates. Iqamah countdowns use editable estimated intervals; see Estimated iqamah settings.\n\nAdhan times, not iqamah. Friday Dhuhr is not a separate Jumuah schedule. Sunrise is not counted as the next prayer.\n\nWorks offline. No accounts, tracking, location access or automatic updates. Optional prayer notifications play a short chime and respect phone sound and Do Not Disturb settings. Open the app after a force-stop to restore alerts. There is no full adhan audio. A new timetable is needed after 10 October 2027.",
        "المواقيت: الهيئة العامة للشؤون الإسلامية والأوقاف والزكاة، أبوظبي. جُلبت في ١١ أكتوبر ٢٠٢٦.\n\nتطبيق شخصي مستقل، وليس تطبيقاً رسمياً للهيئة. يعتمد على جدول ٣٦٥ يوماً المُعدّ في هذه المحادثة، دون تقدير مواقيت أذان إضافية. يستخدم عد الإقامة فترات تقديرية قابلة للتعديل؛ راجع إعدادات الإقامة التقديرية.\n\nمواقيت الأذان وليست الإقامة. وقت الظهر يوم الجمعة ليس جدولاً مستقلاً لصلاة الجمعة. لا يُحسب الشروق ضمن الصلاة القادمة.\n\nيعمل دون إنترنت أو حسابات أو تتبع أو تحديد موقع أو تحديث تلقائي. تنبيهات الصلاة الاختيارية تصدر نغمة قصيرة وتحترم إعدادات الصوت وعدم الإزعاج. افتح التطبيق بعد إيقافه إجبارياً لاستعادة التنبيهات. لا يوجد أذان صوتي كامل. يلزم جدول جديد بعد ١٠ أكتوبر ٢٠٢٧."
    )).setPositiveButton(t("Close","إغلاق"),(d,w)->{}).setNeutralButton(t("Awqaf website","موقع الهيئة"),(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.awqaf.gov.ae/prayer-times")));}catch(ActivityNotFoundException e){Toast.makeText(this,"https://www.awqaf.gov.ae/prayer-times",Toast.LENGTH_LONG).show();}}).show();}
}
