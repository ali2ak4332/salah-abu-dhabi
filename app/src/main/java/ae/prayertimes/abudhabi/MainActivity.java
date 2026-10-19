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
import java.util.*;

public class MainActivity extends Activity {
    private int BG, INK, MUTED, GOLD;
    private AppPreferences.Palette palette;
    private String configuringChannel=AlertScheduler.CHANNEL;
    private AlertDialog widgetDialog;
    private String widgetPage;
    private Schedule schedule;
    private LocalDate selected, renderedToday;
    private boolean arabic, followToday = true;
    private LinearLayout content;
    private static final int NOTIFICATIONS_REQUEST = 12;
    private TextView nextName, countdown, nextTime, phaseLabel, phaseNote;
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
        if(state!=null)configuringChannel=state.getString("notification_setup_channel",state.getBoolean("reminder_setup",false)?AlertScheduler.REMINDER_CHANNEL:AlertScheduler.CHANNEL);
        arabic = AlertScheduler.prefs(this).getBoolean("arabic", Locale.getDefault().getLanguage().equals("ar"));
        try {
            schedule = AlertScheduler.schedule(this);
            AlertScheduler.createChannel(this);
            selected = today();
            if (state != null) { selected = LocalDate.parse(state.getString("date", selected.toString())); followToday = state.getBoolean("follow", true); }
            render();
            if(state!=null){
                if(getIntent()!=null)getIntent().removeExtra(PrayerWidget.OPEN_WIDGET_SETTINGS);
                if(state.getString("widget_page")!=null)showWidgetPage(state.getString("widget_page"));
            }
            else openWidgetSettingsIntent(getIntent());
        } catch (Exception e) {
            TextView error = new TextView(this); error.setText("Unable to read the saved timetable. Please reinstall the app."); error.setPadding(32,64,32,32); setContentView(error);
        }
    }
    private void openWidgetSettingsIntent(Intent intent){
        if(intent!=null&&intent.getBooleanExtra(PrayerWidget.OPEN_WIDGET_SETTINGS,false)){
            intent.removeExtra(PrayerWidget.OPEN_WIDGET_SETTINGS);
            widgetSettings();
        }
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openWidgetSettingsIntent(intent);}
    @Override protected void onSaveInstanceState(Bundle state) { super.onSaveInstanceState(state); if(selected!=null)state.putString("date", selected.toString()); state.putBoolean("follow", followToday);state.putString("notification_setup_channel",configuringChannel);if(widgetPage!=null)state.putString("widget_page",widgetPage); }
    @Override protected void onResume() { super.onResume(); if(schedule!=null){AlertScheduler.reschedule(this);PrayerWidget.updateAll(this);render();handler.removeCallbacks(ticker);handler.post(ticker);} }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }
    private GradientDrawable background(int color, int radius) { GradientDrawable d = new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d; }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this);v.setText(AppPreferences.digits(this,s));v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.START);v.setFontFeatureSettings("tnum");
        v.setTypeface(appTypeface(bold));
        v.setTextDirection(arabic?View.TEXT_DIRECTION_RTL:View.TEXT_DIRECTION_FIRST_STRONG_LTR);
        return v;
    }
    /** Use the platform's proven Arabic/Latin fallback chain in the recovery build. */
    private Typeface appTypeface(boolean medium){
        return Typeface.create(medium?"sans-serif-medium":"sans-serif",arabic&&medium?Typeface.BOLD:Typeface.NORMAL);
    }
    private void space(LinearLayout parent, int size) { View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(size))); }
    private LinearLayout vertical() { LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v; }
    private LinearLayout horizontal() { LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v; }
    private Button button(String label, Runnable action) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTypeface(appTypeface(true));b.setTextColor(INK);b.setTextSize(14);b.setMaxLines(1);autosize(b,8,14);b.setMinHeight(dp(48));b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);b.setBackground(new RippleDrawable(ColorStateList.valueOf((palette.accent&0x00ffffff)|0x22000000),background(palette.surface,14),null));b.setOnClickListener(v->action.run());return b;
    }
    private GradientDrawable outlined(int color,int radius,int line){
        GradientDrawable bg=background(color,radius);bg.setStroke(dp(1),line);return bg;
    }
    /** Figma's exported artwork is bundled locally at its original intrinsic proportions. */
    private android.graphics.drawable.Drawable designIcon(int resource,int color){
        android.graphics.drawable.Drawable icon=getDrawable(resource).mutate();icon.setTint(color);return icon;
    }
    private ImageView designImage(int resource,int color){
        ImageView image=new ImageView(this);image.setImageDrawable(designIcon(resource,color));image.setScaleType(ImageView.ScaleType.CENTER);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return image;
    }
    private ImageButton designButton(int resource,String description,Runnable action){
        ImageButton button=new ImageButton(this);button.setImageDrawable(designIcon(resource,INK));button.setScaleType(ImageView.ScaleType.CENTER);button.setPadding(0,0,0,0);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf((palette.accent&0x00ffffff)|0x22000000),background(Color.TRANSPARENT,16),null));
        button.setContentDescription(description);button.setTooltipText(description);button.setMinimumWidth(dp(48));button.setMinimumHeight(dp(48));button.setOnClickListener(v->action.run());return button;
    }
    private void plainButton(Button button){
        button.setBackground(new RippleDrawable(ColorStateList.valueOf((palette.accent&0x00ffffff)|0x22000000),background(Color.TRANSPARENT,16),null));
    }
    /** Responsive native layout: fixed touch targets, flexible hero and schedule, no scrolling. */
    private void render() {
        applyPalette();
        if(followToday)selected=today();
        renderedToday=today();lastNext=-2;
        PrayerPhase upcoming=AppPreferences.phase(this,schedule,now());
        boolean estimate=upcoming!=null&&upcoming.iqamah;
        FrameLayout viewport=new FrameLayout(this);viewport.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=35)viewport.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int height=getResources().getConfiguration().screenHeightDp;
        boolean compact=!landscape&&height<700;
        boolean tiny=!landscape&&height<540;
        int gap=landscape?8:(tiny?4:(compact?8:16)),edge=compact?14:20;
        content=landscape?horizontal():vertical();content.setTag("home_content");content.setPadding(dp(edge),dp(8),dp(edge),dp(8));content.setLayoutDirection(arabic?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        viewport.addView(content,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout overview=content,timetable=content;
        if(landscape){
            overview=vertical();timetable=vertical();
            content.addView(overview,new LinearLayout.LayoutParams(0,-1,.45f));
            LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-1,.55f);right.setMarginStart(dp(12));content.addView(timetable,right);
        }
        LinearLayout top=horizontal();top.setTag("home_header");LinearLayout title=vertical();
        TextView brand=fitText(t("Salah Abu Dhabi","صلاة أبوظبي"),24,14,INK,true);title.addView(brand,new LinearLayout.LayoutParams(-1,0,1.7f));
        title.addView(fitText(t("Abu Dhabi times · UAE time","مواقيت أبوظبي · توقيت الإمارات"),11,8,MUTED,false),new LinearLayout.LayoutParams(-1,0,1));
        top.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        Button language=button(arabic?"EN":"عربي",()->{arabic=!arabic;AlertScheduler.prefs(this).edit().putBoolean("arabic",arabic).apply();AlertScheduler.createChannel(this);PrayerWidget.updateAll(this);render();});
        plainButton(language);language.setTag("language");language.setTextColor(MUTED);language.setContentDescription(t("Switch to Arabic","التبديل إلى الإنجليزية"));autosize(language,10,13);top.addView(language,new LinearLayout.LayoutParams(dp(48),-1));
        ImageButton menu=designButton(R.drawable.figma_more,t("Settings and more","الإعدادات والمزيد"),()->homeSettings());menu.setTag("settings");top.addView(menu,new LinearLayout.LayoutParams(dp(48),-1));
        overview.addView(top,new LinearLayout.LayoutParams(-1,dp(compact||landscape?48:60)));space(overview,gap);
        LinearLayout hero=vertical();hero.setTag("hero");
        int estimateBg=palette.dark?0xff3c3424:0xfff5edd9;
        int estimateInk=palette.dark?0xffe8ce93:0xff745821;
        int heroInk=estimate?INK:palette.heroText,heroMuted=estimate?estimateInk:0xffdce8de;
        hero.setBackground(estimate?outlined(estimateBg,24,GOLD):background(palette.hero,24));
        hero.setPadding(dp(compact||landscape?16:22),dp(tiny||landscape?8:16),dp(compact||landscape?16:22),dp(tiny||landscape?8:16));
        LinearLayout state=horizontal();state.addView(designImage(estimate?R.drawable.figma_estimate_clock:R.drawable.figma_phase_dot,estimate?estimateInk:GOLD),new LinearLayout.LayoutParams(dp(estimate?16:6),-1));
        phaseLabel=fitText("",13,8,heroMuted,estimate);LinearLayout.LayoutParams phaseParams=new LinearLayout.LayoutParams(0,-1,1);phaseParams.setMarginStart(dp(8));state.addView(phaseLabel,phaseParams);hero.addView(state,new LinearLayout.LayoutParams(-1,0,tiny?30:24));
        LinearLayout prayer=horizontal();nextName=fitText("",32,tiny?11:15,heroInk,true);nextName.setTag("next_name");prayer.addView(nextName,new LinearLayout.LayoutParams(0,-1,1));
        nextTime=fitText("",24,12,heroInk,false);nextTime.setTag("next_time");nextTime.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);prayer.addView(nextTime,new LinearLayout.LayoutParams(0,-1,1));hero.addView(prayer,new LinearLayout.LayoutParams(-1,0,44));
        LinearLayout timer=vertical();timer.setGravity(Gravity.CENTER_HORIZONTAL);
        countdown=fitText("",46,20,estimate?estimateInk:GOLD,true);countdown.setTag("countdown");countdown.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);countdown.setTextDirection(View.TEXT_DIRECTION_LTR);countdown.setGravity(Gravity.CENTER);timer.addView(countdown,new LinearLayout.LayoutParams(-1,0,62));
        LinearLayout units=new LinearLayout(this){
            @Override protected void onMeasure(int widthSpec,int heightSpec){super.onMeasure(View.MeasureSpec.makeMeasureSpec(Math.min(dp(244),View.MeasureSpec.getSize(widthSpec)),View.MeasureSpec.EXACTLY),heightSpec);}
        };units.setOrientation(LinearLayout.HORIZONTAL);units.setGravity(Gravity.CENTER_VERTICAL);units.setTag("countdown_units");units.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        String[] unitLabels=AppPreferences.prefs(this).getBoolean("countdown_seconds",true)?new String[]{t("hours","ساعة"),t("minutes","دقيقة"),t("seconds","ثانية")}:new String[]{t("hours","ساعة"),t("minutes","دقيقة")};
        for(String unit:unitLabels){TextView label=fitText(unit,10,7,estimate?MUTED:heroMuted,false);label.setGravity(Gravity.CENTER);units.addView(label,new LinearLayout.LayoutParams(0,-1,1));}
        timer.addView(units,new LinearLayout.LayoutParams(-1,0,18));hero.addView(timer,new LinearLayout.LayoutParams(-1,0,80));
        phaseNote=fitText("",12,8,heroMuted,false);phaseNote.setTag("phase_note");phaseNote.setGravity(Gravity.CENTER);hero.addView(phaseNote,new LinearLayout.LayoutParams(-1,0,tiny?30:22));
        overview.addView(hero,new LinearLayout.LayoutParams(-1,0,landscape?1:(tiny?230:212)));space(overview,gap);
        LinearLayout controls=horizontal();controls.setTag("date_navigation");controls.setPadding(dp(4),0,dp(4),0);controls.setBackground(outlined(palette.surface,16,palette.border));
        ImageButton prev=designButton(arabic?R.drawable.figma_chevron_right:R.drawable.figma_chevron_left,t("Previous day","اليوم السابق"),()->{selected=schedule.clamp(selected.minusDays(1));followToday=false;render();});prev.setTag("previous_day");prev.setEnabled(selected.isAfter(schedule.days.firstKey()));prev.setAlpha(prev.isEnabled()?1f:.35f);
        Button date=button(AppPreferences.date(this,selected,t("EEE, d MMM","EEE، d MMM")),()->pickDate());date.setTag("date_picker");plainButton(date);date.setHorizontallyScrolling(false);autosize(date,9,14);
        date.setCompoundDrawablePadding(dp(8));android.graphics.drawable.Drawable calendar=designIcon(R.drawable.figma_calendar,MUTED);calendar.setBounds(0,0,calendar.getIntrinsicWidth(),calendar.getIntrinsicHeight());date.setCompoundDrawablesRelative(calendar,null,null,null);date.setContentDescription(t("Choose date: ","اختيار التاريخ: ")+AppPreferences.date(this,selected,"EEEE d MMMM yyyy"));
        Button todayButton=button(t("Today","اليوم"),()->{selected=today();followToday=true;render();});todayButton.setTag("today");plainButton(todayButton);todayButton.setTextColor(palette.accent);todayButton.setContentDescription(t("Return to today","العودة إلى اليوم"));
        ImageButton next=designButton(arabic?R.drawable.figma_chevron_left:R.drawable.figma_chevron_right,t("Next day","اليوم التالي"),()->{selected=schedule.clamp(selected.plusDays(1));followToday=false;render();});next.setTag("next_day");next.setEnabled(selected.isBefore(schedule.days.lastKey()));next.setAlpha(next.isEnabled()?1f:.35f);
        controls.addView(prev,new LinearLayout.LayoutParams(dp(48),-1));controls.addView(date,new LinearLayout.LayoutParams(0,-1,1));controls.addView(todayButton,new LinearLayout.LayoutParams(dp(52),-1));controls.addView(next,new LinearLayout.LayoutParams(dp(48),-1));
        timetable.addView(controls,new LinearLayout.LayoutParams(-1,dp(compact||landscape?48:56)));space(timetable,gap);
        LocalTime[] times=schedule.days.get(selected);
        LinearLayout grid=vertical();grid.setTag("schedule_grid");grid.setBackground(outlined(palette.surface,20,palette.border));grid.setClipToOutline(true);grid.setPadding(dp(1),dp(1),dp(1),dp(1));
        LinearLayout heading=horizontal();heading.setPadding(dp(18),0,dp(18),0);
        TextView scheduleTitle=fitText(selected.equals(today())?t("Today's times","مواقيت اليوم"):t("Prayer times","مواقيت الصلاة"),12,8,MUTED,true);heading.addView(scheduleTitle,new LinearLayout.LayoutParams(0,-1,1));
        TextView adhanLabel=fitText(t("Adhan","الأذان"),10,8,MUTED,false);adhanLabel.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);heading.addView(adhanLabel,new LinearLayout.LayoutParams(0,-1,1));grid.addView(heading,new LinearLayout.LayoutParams(-1,dp(tiny?20:30)));
        if(times==null){
            TextView missing=text(t("No saved timetable for this date. Choose a date in the saved period.","لا يوجد جدول محفوظ لهذا التاريخ. اختر تاريخاً ضمن الفترة المحفوظة."),16,INK,true);missing.setGravity(Gravity.CENTER);missing.setPadding(dp(18),0,dp(18),0);grid.addView(missing,new LinearLayout.LayoutParams(-1,0,1));
        }else{
            for(int i:Schedule.PRAYERS){
                boolean highlighted=upcoming!=null&&selected.equals(upcoming.adhanAt.toLocalDate())&&i==upcoming.index;
                int rowInk=highlighted?(palette.dark?palette.accent:palette.hero):INK;
                LinearLayout row=horizontal();row.setTag("prayer_"+i);row.setPadding(dp(18),0,dp(18),0);row.setBackgroundColor(highlighted?palette.tint:Color.TRANSPARENT);
                LinearLayout label=horizontal();TextView name=fitText(arabic?AR[i]:EN[i],18,10,rowInk,highlighted);name.setTag("prayer_name_"+i);label.addView(name,new LinearLayout.LayoutParams(-2,-1));
                if(highlighted){TextView badge=new TextView(this){
                    @Override protected void onMeasure(int widthSpec,int heightSpec){super.onMeasure(widthSpec,View.MeasureSpec.makeMeasureSpec(Math.min(dp(26),View.MeasureSpec.getSize(heightSpec)),View.MeasureSpec.EXACTLY));}
                };badge.setText(estimate?t("Iqamah","الإقامة"):t("Next","القادمة"));badge.setTextColor(palette.heroText);badge.setTypeface(appTypeface(true));badge.setIncludeFontPadding(false);badge.setMaxLines(1);autosize(badge,7,11);badge.setTag("active_badge");badge.setGravity(Gravity.CENTER);badge.setBackground(background(palette.hero,13));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(tiny?54:56),-1);bp.setMarginStart(dp(12));label.addView(badge,bp);}
                row.addView(label,new LinearLayout.LayoutParams(0,-1,1.4f));
                TextView prayerTime=fitText(AppPreferences.time(this,times[i]),18,10,rowInk,highlighted);prayerTime.setTag("prayer_time_"+i);prayerTime.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);row.addView(prayerTime,new LinearLayout.LayoutParams(0,-1,1));
                row.setContentDescription((arabic?AR[i]:EN[i])+t(" adhan "," أذان ")+AppPreferences.time(this,times[i])+(highlighted?(estimate?t(", estimated iqamah countdown","، عد تنازلي للإقامة التقديرية"):t(", next prayer","، الصلاة القادمة")):""));grid.addView(row,new LinearLayout.LayoutParams(-1,0,1));
            }
            LinearLayout sunrise=horizontal();sunrise.setTag("sunrise");sunrise.setBackgroundColor(BG);sunrise.setPadding(dp(18),0,dp(18),0);
            sunrise.addView(designImage(R.drawable.figma_sunrise,palette.highlight),new LinearLayout.LayoutParams(dp(20),-1));TextView sunLabel=fitText(t("Sunrise","الشروق"),13,8,MUTED,false);LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(0,-1,1);sl.setMarginStart(dp(8));sunrise.addView(sunLabel,sl);
            TextView sunTime=fitText(AppPreferences.time(this,times[1]),14,9,MUTED,false);sunTime.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);sunrise.addView(sunTime,new LinearLayout.LayoutParams(0,-1,1));grid.addView(sunrise,new LinearLayout.LayoutParams(-1,dp(tiny?25:40)));
        }
        timetable.addView(grid,new LinearLayout.LayoutParams(-1,0,landscape?1:310));
        if(!landscape)space(overview,gap);
        addAlertCard(overview,tiny?48:(compact||landscape?56:68));space(overview,tiny?2:(compact?4:16));
        TextView coverage=fitText(t("Awqaf adhan times · Independent app","مواقيت أذان الأوقاف · تطبيق مستقل"),11,8,MUTED,false);coverage.setTag("source_footer");coverage.setGravity(Gravity.CENTER);overview.addView(coverage,new LinearLayout.LayoutParams(-1,dp(tiny?14:(compact||landscape?16:22))));
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
        if(phase==null){phaseLabel.setText(t("SAVED TIMETABLE","الجدول المحفوظ"));nextName.setText(t("Unavailable","غير متاح"));countdown.setText("—");nextTime.setText("");phaseNote.setText(t("Saved through ","محفوظ حتى ")+AppPreferences.date(this,schedule.days.lastKey(),"d MMM yyyy"));return;}
        phaseLabel.setText(phase.iqamah?t("Estimated iqamah","الإقامة التقديرية"):t("Next adhan","الأذان القادم"));
        nextName.setText(arabic?AR[phase.index]:EN[phase.index]);long sec=Math.max(0,(Duration.between(instant,phase.targetAt.toInstant()).toMillis()+999)/1000);
        countdown.setText(AppPreferences.countdown(this,sec));
        nextTime.setText(AppPreferences.time(this,phase.targetAt.toLocalTime()));
        phaseNote.setText(phase.iqamah?t("Adjustable estimate · Follow your mosque","تقدير قابل للتعديل · اتبع موعد مسجدك"):
            (phase.targetAt.toLocalDate().isAfter(today())?t("Tomorrow · ","غداً · "):"")+t("Until ","متبقي على أذان ")+(arabic?AR[phase.index]:EN[phase.index])+t(" adhan",""));
        nextTime.setContentDescription((phase.iqamah?t("Estimated iqamah ","الإقامة التقديرية "):t("Adhan ","الأذان "))+AppPreferences.time(this,phase.targetAt.toLocalTime()));
    }
    private void addAlertCard(LinearLayout parent,int height){
        LinearLayout card=horizontal();card.setTag("alert_card");card.setPadding(dp(14),0,dp(14),0);card.setBackground(outlined(palette.surface,18,palette.border));
        card.addView(designImage(R.drawable.figma_bell,palette.dark?palette.accent:palette.hero),new LinearLayout.LayoutParams(dp(22),-1));
        LinearLayout label=vertical();label.addView(fitText(t("Prayer alerts","تنبيهات الصلاة"),15,10,INK,true),new LinearLayout.LayoutParams(-1,0,1.2f));
        String status;
        if(!AlertScheduler.enabled(this))status=t("Short chime at adhan · Off","نغمة قصيرة عند الأذان · متوقفة");
        else if(!AlertScheduler.notificationsAllowed(this))status=t("Allow notifications · tap here","اسمح بالإشعارات · اضغط هنا");
        else if(AlertScheduler.prefs(this).getBoolean("schedule_error",false))status=t("Setup needs attention · tap here","راجع الإعدادات · اضغط هنا");
        else if(schedule.next(now())==null)status=t("No upcoming saved prayer","لا صلاة قادمة في الجدول");
        else if(!AlertScheduler.precise(this))status=t("Allow precise timing · tap here","اسمح بالتوقيت الدقيق · اضغط هنا");
        else if(!AlertScheduler.soundEnabled(this))status=t("On · phone sound is muted","مفعّل · صوت الهاتف مكتوم");
        else status=t("Short chime at adhan","نغمة قصيرة عند الأذان");
        label.addView(fitText(status,11,8,MUTED,false),new LinearLayout.LayoutParams(-1,0,1));label.setPadding(0,dp(7),0,dp(7));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMarginStart(dp(12));lp.setMarginEnd(dp(12));card.addView(label,lp);
        label.setOnClickListener(v->alertSettings());label.setContentDescription(t("Prayer alert settings. ","إعدادات تنبيهات الصلاة. ")+status);
        // The design's Material switch maps to Android's native Switch semantics.
        // Local shaped drawables preserve the 52 x 32 design and the 48dp touch area.
        Switch toggle=new DesignSwitch(this,palette.hero,palette.tint,MUTED);toggle.setTag("alerts_toggle");toggle.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        toggle.setContentDescription(t("Enable prayer chime alerts","تفعيل تنبيهات الصلاة"));toggle.setChecked(AlertScheduler.enabled(this));
        toggle.setOnCheckedChangeListener((button,onValue)->{AlertScheduler.prefs(this).edit().putBoolean(AlertScheduler.ENABLED,onValue).apply();AlertScheduler.reschedule(this);if(onValue)configureAlerts();else render();});card.addView(toggle,new LinearLayout.LayoutParams(dp(52),dp(48)));parent.addView(card,new LinearLayout.LayoutParams(-1,dp(height)));
    }
    private void testChime(){boolean sent=AlertScheduler.test(this);Toast.makeText(this,sent?t("Test sent. Check notification volume if silent.","أُرسل الاختبار. تحقق من صوت الإشعارات إن لم تسمعه."):t("Allow notifications in Settings first.","اسمح بالإشعارات في الإعدادات أولاً."),Toast.LENGTH_LONG).show();}
    private void homeSettings(){
        dialog().setTitle(t("Settings & more","الإعدادات والمزيد")).setItems(new String[]{
            t("Appearance","المظهر"),t("Time & numerals","الوقت والأرقام"),t("Widget style","مظهر الأدوات"),t("Advance reminders","التذكير قبل الصلاة"),t("Estimated iqamah","الإقامة التقديرية"),
            t("Prayer alert settings","إعدادات تنبيهات الصلاة"),t("Sunrise","الشروق"),t("Test chime","تجربة النغمة"),t("Add a home-screen widget","إضافة أداة للشاشة الرئيسية"),t("Source & app information","المصدر ومعلومات التطبيق")},
            (d,w)->{if(w==0)appearanceSettings();else if(w==1)displaySettings();else if(w==2)widgetSettings();else if(w==3)reminderSettings();else if(w==4)iqamahSettings();else if(w==5)alertSettings();else if(w==6)sunriseSettings();else if(w==7)testChime();else if(w==8)chooseWidget();else about();}).show();
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
    private void widgetSettings(){showWidgetPage("root");}
    private void showWidgetPage(String page){
        if(widgetDialog!=null&&widgetDialog.isShowing())widgetDialog.dismiss();
        widgetPage=page;
        String[] modes={"match","light","dark"},modeLabels={t("Match app","مثل التطبيق"),t("Light","فاتح"),t("Dark","داكن")};
        String[] colors={"match","green","navy","sand"},colorLabels={t("Match app","مثل التطبيق"),t("Green","أخضر"),t("Navy","كحلي"),t("Sand","رملي")};
        String[] sizes={"small","normal","large"},sizeLabels={t("Small","صغير"),t("Normal","عادي"),t("Large","كبير")};
        AlertDialog.Builder builder=dialog();
        if("root".equals(page)){
            builder.setTitle(t("Widget settings · all widgets","إعدادات الأدوات · جميع الأدوات")).setItems(new String[]{
                t("Background · ","الخلفية · ")+prefLabel("widget_mode","dark",modes,modeLabels),
                t("Colour · ","اللون · ")+prefLabel("widget_accent","match",colors,colorLabels),
                t("Opacity · ","التعتيم · ")+AppPreferences.digits(this,AppPreferences.prefs(this).getInt("widget_opacity",100)+"%"),
                t("Text size · ","حجم النص · ")+prefLabel("widget_text","normal",sizes,sizeLabels),
                t("Widget help","المساعدة")},
                (d,w)->showWidgetPage(new String[]{"mode","accent","opacity","text","help"}[w]))
                .setNegativeButton(t("Back","رجوع"),(d,w)->homeSettings());
        }else if("help".equals(page)){
            builder.setTitle(t("Two simple widgets","أداتان بسيطتان")).setMessage(t(
                "Compact (2 × 2) shows five prayer rows. Horizontal (4 × 1) shows five columns. Both show today’s saved Abu Dhabi adhan times and follow your language, numerals, time format and colours. The next adhan is highlighted; during an estimated-iqamah interval the called prayer stays highlighted in amber. Horizontal names are smaller than their times, with a readable minimum.\n\nCompact’s header names the next adhan or estimated iqamah. It uses live seconds when Android’s native digits match your numeral choice; otherwise it shows rounded-up H:MM minute snapshots. In the final minute it switches to the target’s clock time. The date appears when space permits. Main-screen seconds settings do not change the widget timer.\n\nAndroid may delay widget refreshes during sleep or battery restrictions. The final-minute fallback reduces overruns, but if Android delays that refresh too, an old live timer can pass zero until the launcher updates. Open the app to refresh. Display-only refreshes never wake the phone or consume the prayer notification’s idle-alarm quota.\n\nTap anywhere to open the app. The same Compact and Horizontal widgets remain installed after this update. Launcher grid sizes vary; the minimum sizes are 110 × 110dp for Compact and 250 × 40dp for Horizontal. Very small cells may omit the header. Resize larger for easier reading, and use a readable wallpaper with transparent backgrounds.",
                "تعرض المدمجة (٢ × ٢) الصلوات الخمس في صفوف، والأفقية (٤ × ١) في أعمدة. تعرض الأداتان مواقيت أذان اليوم المحفوظة لأبوظبي وتتبعان اللغة والأرقام وتنسيق الوقت والألوان المختارة. يُميّز الأذان القادم، وتبقى الصلاة الحالية مميزة باللون الكهرماني أثناء فترة الإقامة التقديرية. تظهر أسماء الصلوات في الأفقية أصغر من مواقيتها مع حد أدنى مقروء.\n\nيسمّي رأس الأداة المدمجة الأذان القادم أو الإقامة التقديرية. يعرض الثواني مباشرة عندما تطابق أرقام أندرويد نوع الأرقام المختار؛ وإلا يعرض الساعات والدقائق مقربة إلى الأعلى. في الدقيقة الأخيرة يتحول إلى موعد الهدف بصيغة الساعة. يظهر التاريخ حين تسمح المساحة. لا يغيّر إعداد ثواني الشاشة الرئيسية عداد الأداة.\n\nقد يؤخر أندرويد تحديث الأدوات أثناء السكون أو تقييد البطارية. يقلل عرض الموعد في الدقيقة الأخيرة من تجاوز الصفر، لكن إن تأخر هذا التحديث أيضاً فقد يتجاوز العداد القديم الصفر حتى تُحدّث الشاشة الرئيسية الأداة. افتح التطبيق للتحديث. لا توقظ تحديثات العرض الهاتف ولا تستهلك حصة تنبيهات الصلاة أثناء السكون.\n\nاضغط أي مكان لفتح التطبيق. تبقى الأداتان المدمجة والأفقية المثبتتان بعد هذا التحديث. تختلف شبكة الشاشة؛ الحد الأدنى ١١٠ × ١١٠ للمدمجة و٢٥٠ × ٤٠ للأفقية. قد يُخفى الرأس في الخلايا الصغيرة جداً. كبّر الأداة لقراءة أسهل واختر خلفية واضحة عند استخدام الشفافية.")).setPositiveButton(t("Back","رجوع"),(d,w)->widgetSettings());
        }else if("opacity".equals(page)){
            int[] values={100,85,65,0};int selected=AppPreferences.prefs(this).getInt("widget_opacity",100),checked=0;
            for(int n=0;n<values.length;n++)if(values[n]==selected)checked=n;
            builder.setTitle(t("Background opacity","تعتيم الخلفية")).setSingleChoiceItems(new String[]{"100%","85%","65%",t("Transparent","شفاف")},checked,
                (d,w)->{AppPreferences.prefs(this).edit().putInt("widget_opacity",values[w]).apply();preferencesChanged();widgetSettings();})
                .setNegativeButton(t("Back","رجوع"),(d,w)->widgetSettings());
        }else if("seconds".equals(page)){
            builder.setTitle(t("Countdown seconds","ثواني العد التنازلي")).setSingleChoiceItems(new String[]{t("Hide","إخفاء"),t("Show","إظهار")},AppPreferences.prefs(this).getBoolean("countdown_seconds",true)?1:0,
                (d,w)->{AppPreferences.prefs(this).edit().putBoolean("countdown_seconds",w==1).apply();preferencesChanged();widgetSettings();})
                .setNegativeButton(t("Back","رجوع"),(d,w)->widgetSettings());
        }else{
            final String key,fallback;final String[] values,labels;final String title;
            if("mode".equals(page)){key="widget_mode";fallback="dark";values=modes;labels=modeLabels;title=t("Widget background","خلفية الأداة");}
            else if("accent".equals(page)){key="widget_accent";fallback="match";values=colors;labels=colorLabels;title=t("Widget colour","لون الأداة");}
            else{key="widget_text";fallback="normal";values=sizes;labels=sizeLabels;title=t("Widget text size","حجم نص الأداة");}
            int checked=0;String selected=AppPreferences.prefs(this).getString(key,fallback);for(int n=0;n<values.length;n++)if(values[n].equals(selected))checked=n;
            builder.setTitle(title).setSingleChoiceItems(labels,checked,(d,w)->{AppPreferences.prefs(this).edit().putString(key,values[w]).apply();preferencesChanged();widgetSettings();})
                .setNegativeButton(t("Back","رجوع"),(d,w)->widgetSettings());
        }
        final AlertDialog shown=builder.create();widgetDialog=shown;
        shown.setOnDismissListener(d->{if(widgetDialog==shown){widgetDialog=null;widgetPage=null;}});
        shown.show();
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
            "The defaults come from an Awqaf official's statement reported by Al Bayan on 28 January 2009: Fajr 25, Dhuhr/Asr/Isha 20, and Maghrib 5 minutes. These intervals could not be confirmed in a current Awqaf publication. Follow your mosque and adjust each interval here.\n\nThe app counts from the saved adhan time to an estimated iqamah, then shows the next adhan. Friday Dhuhr is excluded because Jumuah has a separate schedule. Adhan times and chime alerts stay unchanged; there is no extra iqamah sound.\n\nThe main screen shows the estimated-iqamah countdown during the selected interval. Compact counts down to estimated iqamah during this interval and to the next adhan otherwise. In the final minute it shows the target clock time. Horizontal remains a five-prayer timetable and highlights the current estimated-iqamah phase. Android may delay widget refreshes, including the final-minute fallback; see Widget help. Countdown seconds under Time & numerals apply to the main screen.",
            "تستند المدد الافتراضية إلى تصريح لمسؤول في الأوقاف نقلته صحيفة البيان في ٢٨ يناير ٢٠٠٩: الفجر ٢٥ دقيقة، والظهر والعصر والعشاء ٢٠ دقيقة، والمغرب ٥ دقائق. لم نتمكن من تأكيد هذه المدد في منشور حديث للأوقاف. اتبع مسجدك وعدّل مدة كل صلاة هنا.\n\nيبدأ العد من موعد الأذان المحفوظ حتى الإقامة التقديرية، ثم يظهر الأذان القادم. يُستثنى ظهر الجمعة لأن صلاة الجمعة لها جدول مستقل. لا تتغير مواقيت الأذان أو تنبيهاته، ولا يصدر صوت إضافي للإقامة.\n\nتعرض الشاشة الرئيسية عد الإقامة التقديرية خلال الفترة المحددة. تعد المدمجة إلى الإقامة التقديرية خلال هذه الفترة وإلى الأذان القادم فيما عداها، وتعرض موعد الهدف بصيغة الساعة في الدقيقة الأخيرة. تبقى الأفقية جدولاً للصلوات الخمس مع تمييز فترة الإقامة التقديرية الحالية. قد يؤخر أندرويد تحديث الأدوات، بما في ذلك التحول في الدقيقة الأخيرة؛ راجع مساعدة الأدوات. يخص إعداد الثواني ضمن الوقت والأرقام الشاشة الرئيسية."))
            .setPositiveButton(t("Back","رجوع"),(d,w)->iqamahSettings()).show();
    }
    private void openIqamahSource(){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.albayan.ae/across-the-uae/2009-01-28-1.397198")));}catch(Exception e){Toast.makeText(this,t("Install a browser to open the source.","ثبّت متصفحاً لفتح المصدر."),Toast.LENGTH_LONG).show();}}
    private void sunriseSettings(){
        boolean enabled=AlertScheduler.sunriseEnabled(this);
        String status=!enabled?t("Off","إيقاف"):!AlertScheduler.sunriseNotificationsAllowed(this)?t("On · allow notifications","مفعّل · اسمح بالإشعارات"):AlertScheduler.prefs(this).getBoolean("sunrise_schedule_error",false)?t("On · check setup","مفعّل · راجع الإعدادات"):now().isAfter(schedule.days.lastKey().atTime(schedule.days.lastEntry().getValue()[1]).atZone(Schedule.ZONE).toInstant())?t("On · saved timetable ended","مفعّل · انتهى الجدول المحفوظ"):t("On","تشغيل");
        dialog().setTitle(t("Sunrise","الشروق")).setItems(new String[]{
            t("Sunrise notification · ","تنبيه الشروق · ")+status,
            t("Notification sound & permissions","صوت الإشعارات والأذونات"),
            t("Precise timing","التوقيت الدقيق"),t("About sunrise notifications","حول تنبيهات الشروق")},(d,w)->{
            if(w==0)dialog().setTitle(t("Sunrise notification","تنبيه الشروق")).setSingleChoiceItems(new String[]{t("Off","إيقاف"),t("On","تشغيل")},enabled?1:0,(dlg,k)->{
                AppPreferences.prefs(this).edit().putBoolean(AlertScheduler.SUNRISE_ENABLED,k==1).apply();
                dlg.dismiss();AlertScheduler.reschedule(this);render();
                if(k==1)configureAlerts(AlertScheduler.SUNRISE_CHANNEL);else sunriseSettings();
            }).setNegativeButton(t("Back","رجوع"),(dlg,k)->sunriseSettings()).show();
            else if(w==1){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)configureAlerts(AlertScheduler.SUNRISE_CHANNEL);else notificationSettings(AlertScheduler.SUNRISE_CHANNEL);}
            else if(w==2)preciseSettings();
            else dialog().setTitle(t("Sunrise","الشروق")).setMessage(t(
                "A short chime at the official saved Awqaf sunrise time for Abu Dhabi. Sunrise is separate from the five prayers and estimated iqamah. It has its own on/off setting and notification sound category.\n\nEnabled by default for this update, and delivered only when Android notifications are allowed. This does not change your prayer alert or advance reminder settings. Sound respects notification volume and Do Not Disturb.\n\nAllow precise timing for delivery while the app is closed. Android may still delay alerts; sunrise alerts more than 15 minutes late are skipped. Schedules restore after reboot/unlock, app updates and clock changes. Open the app after a force-stop. Times remain fixed to Abu Dhabi and stop after the saved timetable ends on 31 December 2027.",
                "نغمة قصيرة عند موعد الشروق الرسمي المحفوظ من الأوقاف لأبوظبي. الشروق مستقل عن الصلوات الخمس والإقامة التقديرية، وله إعداد تشغيل وإيقاف وفئة صوت إشعار خاصة.\n\nمفعّل افتراضياً في هذا التحديث، ولا يُرسل إلا عند السماح بإشعارات أندرويد. لا يغيّر إعدادات تنبيهات الصلاة أو التذكير المسبق. يحترم الصوت مستوى صوت الإشعارات وعدم الإزعاج.\n\nاسمح بالتوقيت الدقيق لوصول التنبيه والتطبيق مغلق. قد يؤخر أندرويد التنبيه؛ ويُتجاوز تنبيه الشروق إذا تأخر أكثر من ١٥ دقيقة. تُستعاد الجدولة بعد إعادة التشغيل وفتح القفل وتحديث التطبيق وتغيير الساعة. افتح التطبيق بعد إيقافه إجبارياً. تبقى المواقيت ثابتة لأبوظبي وتتوقف بعد انتهاء الجدول المحفوظ في ٣١ ديسمبر ٢٠٢٧."
            )).setPositiveButton(t("Back","رجوع"),(dlg,k)->sunriseSettings()).show();
        }).show();
    }
    private void configureAlerts(){configureAlerts(AlertScheduler.CHANNEL);}
    private void configureAlerts(boolean reminders){configureAlerts(reminders?AlertScheduler.REMINDER_CHANNEL:AlertScheduler.CHANNEL);}
    private void configureAlerts(String channel){
        configuringChannel=channel;
        boolean reminders=AlertScheduler.REMINDER_CHANNEL.equals(channel),sunrise=AlertScheduler.SUNRISE_CHANNEL.equals(channel);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATIONS_REQUEST);return;}
        if(!(sunrise?AlertScheduler.sunriseNotificationsAllowed(this):reminders?AlertScheduler.reminderNotificationsAllowed(this):AlertScheduler.notificationsAllowed(this))){notificationSettings(channel);return;}
        if(!AlertScheduler.precise(this))dialog().setTitle(sunrise?t("Sunrise timing","توقيت الشروق"):t("Prayer-time accuracy","دقة توقيت الصلاة")).setMessage(sunrise?t("Allow Alarms & reminders so the sunrise chime can arrive at the official saved Abu Dhabi sunrise time while the app is closed. Without this, Android may delay it; alerts over 15 minutes late are skipped.","اسمح بالمنبهات والتذكيرات ليصل تنبيه الشروق في موعده الرسمي المحفوظ لأبوظبي والتطبيق مغلق. دون ذلك قد يؤخره أندرويد، وتُتجاوز التنبيهات المتأخرة أكثر من ١٥ دقيقة."):reminders?t("Allow Alarms & reminders for advance reminder timing. Battery saving may still delay or skip reminders; they are never delivered at or after the prayer time.","اسمح بالمنبهات والتذكيرات لضبط موعد التذكير. قد يؤخر توفير البطارية التذكير أو يمنعه؛ ولن يُرسل عند دخول وقت الصلاة أو بعده."):t("Allow Alarms & reminders so the chime can arrive at the saved prayer time while the app is closed. Without this, Android may delay it; alerts over 15 minutes late are skipped.","اسمح بالمنبهات والتذكيرات ليصل التنبيه في موعد الصلاة المحفوظ والتطبيق مغلق. دون ذلك قد يؤخره أندرويد، وتُتجاوز التنبيهات المتأخرة أكثر من ١٥ دقيقة.")).setPositiveButton(t("Allow precise timing","السماح بالتوقيت الدقيق"),(d,w)->preciseSettings()).setNegativeButton(t("Later","لاحقاً"),(d,w)->render()).show();
        AlertScheduler.reschedule(this);render();
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==NOTIFICATIONS_REQUEST){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED&&AlertScheduler.anyAlertsEnabled(this))configureAlerts(configuringChannel);else{AlertScheduler.reschedule(this);render();}}}
    private void notificationSettings(){notificationSettings(AlertScheduler.CHANNEL);}
    private void notificationSettings(boolean reminders){notificationSettings(reminders?AlertScheduler.REMINDER_CHANNEL:AlertScheduler.CHANNEL);}
    private void notificationSettings(String channel){
        Intent i=new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,channel);
        if(!getSystemService(NotificationManager.class).areNotificationsEnabled())i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());
        openSettings(i);
    }
    private void preciseSettings(){if(Build.VERSION.SDK_INT>=31)openSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));else Toast.makeText(this,t("Precise timing is already available.","التوقيت الدقيق متاح بالفعل."),Toast.LENGTH_SHORT).show();}
    private void openSettings(Intent i){try{startActivity(i);}catch(ActivityNotFoundException e){Toast.makeText(this,t("Open this app's settings on your phone.","افتح إعدادات هذا التطبيق على هاتفك."),Toast.LENGTH_LONG).show();}}
    private void alertSettings(){
        String[] items={t("Finish alert setup","إكمال إعداد التنبيهات"),t("Notification sound & permissions","صوت الإشعارات والأذونات"),t("Precise timing","التوقيت الدقيق"),t("How background alerts work","كيف تعمل التنبيهات في الخلفية")};
        dialog().setTitle(t("Prayer alerts","تنبيهات الصلاة")).setItems(items,(d,w)->{if(w==0){AlertScheduler.prefs(this).edit().putBoolean(AlertScheduler.ENABLED,true).apply();configureAlerts();}else if(w==1)notificationSettings();else if(w==2)preciseSettings();else dialog().setTitle(t("Background alerts","التنبيهات في الخلفية")).setMessage(t("The five prayers use a short notification chime, not full adhan. Sunrise has its own setting under Sunrise. Sound follows your notification volume, channel settings and Do Not Disturb.\n\nAllow notifications and precise alarms. Schedules restore after a reboot (after unlocking). Force-stop cancels alerts until you reopen the app. Some phones also restrict background activity; use this app's battery settings if alerts are delayed.\n\nTimes stay fixed to Abu Dhabi. Timetable coverage ends 31 December 2027.","تستخدم الصلوات الخمس نغمة إشعار قصيرة دون أذان كامل، وللشروق إعداد مستقل ضمن الشروق. يتبع الصوت مستوى صوت الإشعارات وإعدادات القناة وعدم الإزعاج.\n\nاسمح بالإشعارات والمنبهات الدقيقة. تُستعاد الجدولة بعد إعادة التشغيل وفتح قفل الهاتف. الإيقاف الإجباري يلغي التنبيهات حتى تفتح التطبيق مجدداً. بعض الهواتف تقيد العمل في الخلفية؛ راجع إعدادات بطارية التطبيق إذا تأخرت التنبيهات.\n\nالمواقيت ثابتة لأبوظبي. ينتهي الجدول في ٣١ ديسمبر ٢٠٢٧.")).setPositiveButton(t("Got it","حسناً"),null).show();}).show();
    }
    private void chooseWidget(){dialog().setTitle(t("Add a widget","إضافة أداة")).setItems(new String[]{t("Compact · 2 × 2","مدمجة · ٢ × ٢"),t("Horizontal · 4 × 1","أفقية · ٤ × ١")},(d,w)->{
        AppWidgetManager manager=getSystemService(AppWidgetManager.class);
        if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(this,new Class<?>[]{PrayerWidgetCompact.class,PrayerWidgetStrip.class}[w]),null,null);
        else dialog().setMessage(t("Long-press an empty area of your home screen → Widgets → Salah Abu Dhabi. Choose Compact or Horizontal. Sizes vary with your launcher’s grid.","اضغط مطولاً على مساحة فارغة بالشاشة الرئيسية ← الأدوات ← Salah Abu Dhabi. اختر مدمجة أو أفقية. تختلف الأبعاد حسب شبكة الشاشة.")).setPositiveButton(t("Got it","حسناً"),null).show();
    }).show();}
    private void about(){dialog().setTitle(t("Salah v2.6 · Source & information","الصلاة v2.6 · المصدر والمعلومات")).setMessage(t(
        "Times: UAE General Authority of Islamic Affairs, Endowments & Zakat (Awqaf), Abu Dhabi. Retrieved 11 October 2026; extended from Awqaf on 16 October 2026.\n\nAn independent personal app, not an official Awqaf app. Uses the 447-day saved Awqaf timetable; no extrapolated adhan dates. Iqamah countdowns use editable estimated intervals; see Estimated iqamah settings.\n\nAdhan times, not iqamah. Friday Dhuhr is not a separate Jumuah schedule. Sunrise is not counted as the next prayer.\n\nWorks offline. No accounts, tracking, location access or automatic updates. Prayer and separate sunrise notifications play a short chime and respect phone sound and Do Not Disturb settings. Open the app after a force-stop to restore alerts. There is no full adhan audio. A new timetable is needed after 31 December 2027.",
        "المواقيت: الهيئة العامة للشؤون الإسلامية والأوقاف والزكاة، أبوظبي. جُلبت في ١١ أكتوبر ٢٠٢٦؛ وتم تمديدها من الأوقاف في ١٦ أكتوبر ٢٠٢٦.\n\nتطبيق شخصي مستقل، وليس تطبيقاً رسمياً للهيئة. يعتمد على جدول الأوقاف المحفوظ لـ٤٤٧ يوماً، دون تقدير مواقيت أذان إضافية. يستخدم عد الإقامة فترات تقديرية قابلة للتعديل؛ راجع إعدادات الإقامة التقديرية.\n\nمواقيت الأذان وليست الإقامة. وقت الظهر يوم الجمعة ليس جدولاً مستقلاً لصلاة الجمعة. لا يُحسب الشروق ضمن الصلاة القادمة.\n\nيعمل دون إنترنت أو حسابات أو تتبع أو تحديد موقع أو تحديث تلقائي. تنبيهات الصلاة والشروق المستقلة تصدر نغمة قصيرة وتحترم إعدادات الصوت وعدم الإزعاج. افتح التطبيق بعد إيقافه إجبارياً لاستعادة التنبيهات. لا يوجد أذان صوتي كامل. يلزم جدول جديد بعد ٣١ ديسمبر ٢٠٢٧."
    )).setPositiveButton(t("Close","إغلاق"),(d,w)->{}).setNeutralButton(t("Awqaf website","موقع الهيئة"),(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.awqaf.gov.ae/prayer-times")));}catch(ActivityNotFoundException e){Toast.makeText(this,"https://www.awqaf.gov.ae/prayer-times",Toast.LENGTH_LONG).show();}}).show();}
}
