package ae.prayertimes.abudhabi;
import android.content.*;
public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){ AlertScheduler.received(context,intent); }
}
