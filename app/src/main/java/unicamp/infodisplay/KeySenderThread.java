package unicamp.infodisplay;

import android.app.Instrumentation;

/**
 * Created by Jessica on 13-Jun-15.
 */
public class KeySenderThread extends Thread{

    private Instrumentation m_Instrumentation = new Instrumentation();
    private int repetitions;
    private int key;

    public KeySenderThread(int repeatTimes, int keyCode){
        repetitions = repeatTimes;
        key = keyCode;
    }

    @Override
    public void run(){
        int counter = 0;
        while(counter++ < repetitions) {
            m_Instrumentation.sendKeyDownUpSync(key);
            try { sleep(180); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

}