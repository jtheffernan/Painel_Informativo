/*
 * Copyright (c) 2010-11 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */


package unicamp.infodisplay;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Picture;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ViewSwitcher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NavigableMap;

public class InfoDisplay extends Activity {

    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    static private String ACESS_TOKEN_KEY;
    static private String ACESS_TOKEN_SECRET;
    static private String USER_PUBLIC_FOLDER;
    static private String USER_CODE;
    static private String PDF_VIEWER_PATH;

    static private SimpleDateFormat format;

    static private Integer FLIP_INTERVAL; // 40 seconds
    static private Integer CHECK_INTERVAL; // 10 minutes

//    final static int PPT_HEIGHT_WIDTH_FACTOR = 8;
//    final static int EXECUTIVE_HEIGHT_WIDTH_FACTOR = 14;

    private ViewFlipper mFlipper;
    private ViewSwitcher mSwitcher;
    private WebViewClient mWebClient;
    private JSInterface jsInterface;
    private String mCampusSliderName;

    Intent intent;

    // Define the callback for what to do when data is received
    private BroadcastReceiver contentReceiver;

    {
        contentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getIntExtra("resultCode", RESULT_CANCELED) == RESULT_OK) {
                    // Make a list of everything in it
                    newDocPaths.clear();
                    newDocDates.clear();
                    resetKeepFlags();
                    int indexAtFlipper = -1;
                    int indexEntries = 0;
                    int removals = 0;

                    ArrayList<String> dates = intent.getStringArrayListExtra("dates");

                    for (String name : intent.getStringArrayListExtra("paths")) {
                        Date lastMod = entryModified(dates.get(indexEntries++));

                        indexAtFlipper = -1;
                        for (int i = 0; i < mFlipper.getChildCount(); i++) {
                            if (mFlipper.getChildAt(i).getContentDescription().toString().contentEquals(name)) {
                                indexAtFlipper = i;
                                break;
                            }
                        }
                        if (indexAtFlipper != -1) { // jah esta no flipper
                            if (lastMod.after((Date) mFlipper.getChildAt(indexAtFlipper).getTag())) {
                                // modified after insertion
                                Log.i(name, "modified after insertion");
                                newDocPaths.add(name);
                                newDocDates.add(lastMod);
                            } else {
                                // nothinng to be done
                                Log.i(name, "kept");
                                flippingDocsKeep.set(indexAtFlipper, Boolean.TRUE);
                            }
                        } else { //new document
                            Log.i(name, "new");
                            newDocPaths.add(name);
                            newDocDates.add(lastMod);
                        }
                    }

                    for (int i = 0; i < flippingDocsKeep.size(); i++) {
                        if (!flippingDocsKeep.get(i)) {
                            Log.i(mFlipper.getChildAt(i - removals).getContentDescription().toString(), "removed");
                            mFlipper.removeViewAt(i - removals);
                            removals++;
                        }
                    }

                    Log.i(String.valueOf(removals), "removals");
                    Log.i(String.valueOf(newDocPaths.size()), "new entries");

                    for (int i = 0; i < newDocPaths.size(); i++) {
                        WebView mView = new WebView(InfoDisplay.this);
                        mView.getSettings().setJavaScriptEnabled(true);
                        mView.setWebViewClient(mWebClient);
                        mView.setContentDescription(newDocPaths.get(i));
                        // /* Register a new JavaScript interface called HTMLOUT */
                        jsInterface = new JSInterface(mView);
                        mView.addJavascriptInterface(jsInterface, "HTMLOUT");
                        mView.setTag(newDocDates.get(i));
                        mView.setPictureListener(new WebView.PictureListener() {
                            @Override
                            public void onNewPicture(WebView view, Picture picture) {
                                if ((mFlipper.getCurrentView().equals(view)) && (currentDocPage == 1)) {
                                    view.loadUrl("javascript:HTMLOUT.getDocAttributes(document.getElementsByClassName('ndfHFb-c4YZDc-cYSp0e-DARUcf')[0].getAttribute('style').concat(document.getElementsByTagName('img')[0].getAttribute('alt')));");
                                }
                            }
                        });
                        mView.loadUrl(PDF_VIEWER_PATH + USER_PUBLIC_FOLDER + USER_CODE + "/" + ContentUpdater.CONTENT_DIR + newDocPaths.get(i));// + "#zoom=page-width");
                        mFlipper.addView(mView);
                    }

                }

                if (mFlipper.getChildCount() == 0){
                    showToast("Nada a ser exibido.");
                }
                else if(!isRunning) {
                    isRunning = true;
                    mHandler.postDelayed(contentManager, FLIP_INTERVAL);
                    mSwitcher.setDisplayedChild(0);
                    mFlipper.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    private ArrayList<Boolean> flippingDocsKeep = new ArrayList<Boolean>();
    private ArrayList<String> newDocPaths = new ArrayList<String>();
    private ArrayList<Date> newDocDates = new ArrayList<Date>();

    static RangeKeyMap docTypesMap = new RangeKeyMap();

    private KeySenderThread keyThread;
    private Integer currentDocPage = 1;
    private Handler mHandler = new Handler();
    private  boolean isRunning;
    private Runnable contentManager;

    {
        contentManager = new Runnable() {
            public void run() {
                WebView currView = (WebView) mFlipper.getCurrentView();

                if (currView != null) {

                    Log.v(currView.getTitle(), String.valueOf(currentDocPage));

                    long eventTime = SystemClock.uptimeMillis() + 1;

                    // simulate click in the middle of screen
                    // avoid blue in google viewer zoom button

                    try {
                        // press
                        MotionEvent event = MotionEvent.obtain(eventTime - 1, eventTime,
                                MotionEvent.ACTION_DOWN, currView.getWidth() / 2, currView.getHeight() / 2, 0);
                        // Dispatch touch event to view
                        currView.dispatchTouchEvent(event);
                        // release finger, gesture is finished
                        eventTime = SystemClock.uptimeMillis();
                        event = MotionEvent.obtain(eventTime - 1, eventTime,
                                MotionEvent.ACTION_UP, currView.getWidth() / 2, currView.getHeight() / 2, 0);
                        // Dispatch touch event to view
                        currView.dispatchTouchEvent(event);//inst.sendPointerSync(event);

                    } catch (Exception ignored) {
                    }


                    if (getNumPages(currView.getId()) > currentDocPage) {
                        boolean pageDown = true;

                        if (keyThread != null) {
                            if (keyThread.isAlive()) {
                                pageDown = false;
                            }
                        }

                        if (pageDown) {
                            keyThread = new KeySenderThread(calculateRepetitions(currView.getId()), KeyEvent.KEYCODE_DPAD_DOWN);
                            keyThread.start();
                            currentDocPage++;
                        }

                    } else {
                        if (currentDocPage > 1) {
                            // back to first start
                            currentDocPage = 1;
                            Log.i("reloading", currView.getTitle());
                            currView.reload();
                            docTypesMap.getPreviousDocType().resetDeltaIndex();
                        }
                        //currView.invalidate();
/*
                        mFlipper.setOutAnimation(null);
                        mFlipper.setInAnimation(null);
                        mFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
                        mFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
                        */
                        mFlipper.showNext();

                        // simulate click in the middle of screen
                        // avoid blue in google viewer zoom button

                        try {
                            // press
                            MotionEvent event = MotionEvent.obtain(eventTime - 1, eventTime,
                                    MotionEvent.ACTION_DOWN, currView.getWidth() / 2, currView.getHeight() / 2, 0);
                            // Dispatch touch event to view
                            currView.dispatchTouchEvent(event);
                            // release finger, gesture is finished
                            eventTime = SystemClock.uptimeMillis();
                            event = MotionEvent.obtain(eventTime - 1, eventTime,
                                    MotionEvent.ACTION_UP, currView.getWidth() / 2, currView.getHeight() / 2, 0);
                            // Dispatch touch event to view
                            currView.dispatchTouchEvent(event);//inst.sendPointerSync(event);

                        } catch (Exception ignored) {
                        }
                        //mFlipper.getCurrentView().invalidate(); //requestFocus();

                    }

                }

                mHandler.postDelayed(this, FLIP_INTERVAL);
            }
        };
        mWebClient = new WebViewClient() {

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(description, String.valueOf(errorCode));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCampusSliderName = savedInstanceState.getString("mCampusSliderName");
        }

        setContentView(R.layout.main);

        mSwitcher = (ViewSwitcher)findViewById(R.id.viewSwitcher);
        mFlipper = (ViewFlipper)findViewById(R.id.webFlipper);
        mSwitcher.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        mSwitcher.setDisplayedChild(1);

        isRunning = false;
        getConfiguration();
        scheduleAlarm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for the particular broadcast based on ACTION string
        IntentFilter filter = new IntentFilter(ContentUpdater.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(contentReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener when the application is paused
        LocalBroadcastManager.getInstance(this).unregisterReceiver(contentReceiver);
    }

    private void scheduleAlarm() {
        // Construct an intent that will execute the AlarmReceiver
        intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        String [] stored = getKeys();
        if(stored == null){
            stored = new String[2];
            stored[0] = ACESS_TOKEN_KEY;
            stored[1] = ACESS_TOKEN_SECRET;
        }
        intent.putExtra("dbKey",stored[0]);
        intent.putExtra("dbSecret",stored[1]);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Setup periodic alarm every 10 minutes
        long firstMillis = System.currentTimeMillis(); // first run of alarm is immediate
        int intervalMillis = CHECK_INTERVAL; // 10 minutes
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, intervalMillis, pIntent);
    }

    public void cancelAlarm() {
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }

    private void resetKeepFlags(){
        flippingDocsKeep.clear();
        for (int i = 0; i < mFlipper.getChildCount(); i++) {
            flippingDocsKeep.add(Boolean.FALSE);
        }
    }

    private int getNumPages(int id){
        if(id < 1)
            return id;

        return (id & 0x00FF) ;
    }

    private int calculateRepetitions(int id){
        if(id < 1)
            return 0;

        int factor = (id >> 16) & 0x00FF;

        return  docTypesMap.getDocType(factor).currentDelta();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("mCampusSliderName", mCampusSliderName);
        super.onSaveInstanceState(outState);
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    private Date entryModified(String dateString){
        Date date;
        try {
            date = format.parse(dateString.substring(5,25));
        } catch (ParseException e) {
            date = null;
            Log.e("date conersion error", e.getMessage());
        }
        return date;
    }

    private void getConfiguration(){
        try {
            if(isExternalStorageReadable()) {
                readJson(new FileInputStream(Environment.getExternalStorageDirectory().getPath() + "/config.json"));
            }else
                showToast("Arquivo de configuracao nao encontrado.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public void readJson(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("ACESS_TOKEN_KEY")) {
                    ACESS_TOKEN_KEY = reader.nextString();
                } else if (name.equals("ACESS_TOKEN_SECRET")) {
                    ACESS_TOKEN_SECRET = reader.nextString();
                } else if (name.equals("USER_CODE")) {
                    USER_CODE = reader.nextString();
                } else if (name.equals("USER_PUBLIC_FOLDER")) {
                    USER_PUBLIC_FOLDER = reader.nextString();
                } else if (name.equals("PDF_VIEWER_PATH")) {
                    PDF_VIEWER_PATH = reader.nextString();
                } else if (name.equals("CONTENT_DIR")) {
                    ContentUpdater.CONTENT_DIR = reader.nextString();
                } else if (name.equals("DATE_FORMAT")) {
                    format = new SimpleDateFormat(reader.nextString());
                } else if (name.equals("FLIP_INTERVAL")) {
                    FLIP_INTERVAL = reader.nextInt();
                } else if (name.equals("CHECK_INTERVAL")) {
                    CHECK_INTERVAL = reader.nextInt();
                } else if (name.equals("APP_KEY")) {
                    ContentUpdater.APP_KEY = reader.nextString();
                } else if (name.equals("APP_SECRET")) {
                    ContentUpdater.APP_SECRET = reader.nextString();
                } else if (name.equals("DOC_TYPES")) {
                    getDocTypes(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        }
        finally{
            reader.close();
        }
    }

    public void getDocTypes(JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            DocType docType = new DocType(reader);
            docTypesMap.put(docType.getPadding_bottom(),docType);
        }
        reader.endArray();
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time.
     *
     * @return Array of [access_key, access_secret]
     */
    private String[] getKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
            String[] ret = new String[2];
            ret[0] = key;
            ret[1] = secret;
            return ret;
        } else {
            return null;
        }
    }

}
