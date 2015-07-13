package unicamp.infodisplay;

import android.util.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Jessica on 10-Jul-15.
 */
public class DocType {
    private String  name;
    private Integer padding_bottom;
    private Float deltas_per_page;
    private ArrayList<Integer> deltaList;
    private Iterator<Integer> deltaIterator;

    public DocType(JsonReader reader) throws IOException {
        deltaList = new ArrayList<Integer>();
        reader.beginObject();
        while (reader.hasNext()) {
            String str = reader.nextName();
            if (str.equals("padding_bottom")) {
                padding_bottom = reader.nextInt();
            } else if (str.equals("name")) {
                name = reader.nextString();
            } else if (str.equals("scroll")) {
                readIntArray(reader);
            }  else if (str.equals("deltas_per_page")) {
                deltas_per_page = ((float) reader.nextDouble());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

    private void readIntArray(JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            deltaList.add(reader.nextInt());
        }
        reader.endArray();
        deltaIterator = deltaList.iterator();
    }

    public Integer getPadding_bottom(){
        return padding_bottom;
    }

    public Integer currentDelta(){
        Integer delta;
        if(deltaIterator.hasNext())
            delta = deltaIterator.next();
        else {
            // restart
            resetDeltaIndex();
            if(deltaIterator.hasNext())
                delta = deltaIterator.next();
            else // empty
                delta = null;
        }
        return delta;
    }

    public void resetDeltaIndex(){
        deltaIterator = deltaList.iterator();
    }

    public Integer getRepetitions(int numPages){
        return Math.round(numPages * deltas_per_page);
    }

    @Override
    public String toString(){
        return name;
    }



}
