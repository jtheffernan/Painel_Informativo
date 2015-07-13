package unicamp.infodisplay;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/**
 * Created by Jessica on 13-Jun-15.
 */
public class JSInterface {
    private WebView mParent;

    public JSInterface(WebView parent){
        mParent = parent;
    };

    @SuppressWarnings("unused")
    @JavascriptInterface
    public void getDocAttributes(String htmlInfo)
    {
        //Log.i("info", htmlInfo);

        int start = htmlInfo.indexOf("padding-bottom: ");
        int end;
        float padding;

        if(start != -1) {
            start += 15;
            end = htmlInfo.indexOf("%", start);
            padding = Float.valueOf(htmlInfo.substring(start, end));
            //Log.i("padding",htmlInfo.substring(start, end));
        }else{
            start = htmlInfo.indexOf("height: ") + 8;
            end = htmlInfo.indexOf("px",start) ;
            padding = Float.valueOf(htmlInfo.substring(start, end));
            //Log.i("width",htmlInfo.substring(start, end));

            start = htmlInfo.indexOf("width: ") + 7;
            end = htmlInfo.indexOf("px",start);
            padding /= Float.valueOf(htmlInfo.substring(start, end));
            padding *= 100;
        //Log.i("height",htmlInfo.substring(start, end));

        }

        start = htmlInfo.indexOf("Page ") + 5;
        int numPages = Integer.valueOf(htmlInfo.substring(start).split(" ")[2]);
        //Log.i("numpages",htmlInfo.substring(start).split(" ")[2]);

        mParent.setId(generateId(padding, numPages));
    }


    @JavascriptInterface
    int generateId(float padding, int numPages){
        int factorx10 = Math.round(padding/10);
        Log.d("factor h/w", String.valueOf(factorx10));
        return ( (factorx10 << 16) | InfoDisplay.docTypesMap.getDocType(factorx10).getRepetitions(numPages) ) ;
    }
}

