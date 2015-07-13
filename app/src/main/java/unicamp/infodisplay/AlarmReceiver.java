package unicamp.infodisplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Jessica on 09-Jun-15.
 */
public class AlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;

    // Triggered by the Alarm periodically (starts the service to run task)
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, ContentUpdater.class);
        i.putExtra("dbKey", intent.getStringExtra("dbKey"));
        i.putExtra("dbSecret", intent.getStringExtra("dbSecret"));
        context.startService(i);
    }
}