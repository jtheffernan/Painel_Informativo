package unicamp.infodisplay;

import android.util.Log;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by Jessica on 10-Jul-15.
 */
public class RangeKeyMap extends TreeMap<Integer,DocType> {
    DocType lastDocType;

    public DocType getDocType(int key){

        int delta = key - floorKey(key);
        //Log.d("delta",String.valueOf(delta));

        if(delta > (ceilingKey(key) - key))
            lastDocType = ceilingEntry(key).getValue();
        else
            lastDocType = floorEntry(key).getValue();


        Log.v("key",String.valueOf(lastDocType.getPadding_bottom()));

        return  lastDocType;
    }

    public  DocType getPreviousDocType(){
        return lastDocType;
    }
}
