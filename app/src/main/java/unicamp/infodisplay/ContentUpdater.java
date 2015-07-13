package unicamp.infodisplay;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.support.v4.content.LocalBroadcastManager ;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;

import java.util.ArrayList;

/**
 * Created by Jessica on 09-Jun-15.
 */
public class ContentUpdater extends IntentService {

    // App key and secret assigned by Dropbox.
    static String APP_KEY;
    static String APP_SECRET;
    static String CONTENT_DIR;

    // Access type (full Dropbox instead of app folder).
    final static private Session.AccessType ACCESS_TYPE = Session.AccessType.DROPBOX;

    public static final String ACTION = "unicamp.ContentManager";

    public  ContentUpdater()
    {
        super("ContentManager");

    }

    private AndroidAuthSession buildSession(AppKeyPair stored) {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AccessTokenPair accessToken = new AccessTokenPair(stored.key, stored.secret);
        return new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        // get stored keys
        AndroidAuthSession session = buildSession(new AppKeyPair(intent.getStringExtra("dbKey"),intent.getStringExtra("dbSecret")));
        DropboxAPI<AndroidAuthSession> dropboxAPI = new DropboxAPI<AndroidAuthSession>(session);
        boolean error = false;
        ArrayList<String> docsPaths = new ArrayList<String>();
        ArrayList<String> docsDateStrings = new ArrayList<String>();

        try {
            // Get the metadata for a directory
            DropboxAPI.Entry directoryEntry = dropboxAPI.metadata("/Public/" + CONTENT_DIR, 1000, null, true, null);

            if ((directoryEntry.isDir) || (directoryEntry.contents != null) || !directoryEntry.contents.isEmpty()) {
                for (DropboxAPI.Entry entry : directoryEntry.contents) {
                    docsPaths.add(entry.fileName());
                    docsDateStrings.add(entry.modified);
                }
            }

        } catch (Exception ignored){
            return;
        }
        Log.i("Content Manager", "Service running");
        Intent in = new Intent(ACTION);
        in.putStringArrayListExtra("paths", docsPaths);
        in.putStringArrayListExtra("dates", docsDateStrings);
        in.putExtra("resultCode",Activity.RESULT_OK);

        LocalBroadcastManager.getInstance(this).sendBroadcast(in);
    }
}


