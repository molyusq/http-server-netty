package zhuk.andriy.http.server.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Andrew on 01.04.15.
 */
public class RedirectUtil {
    private ConcurrentHashMap<String, Integer> redirectionList = new ConcurrentHashMap<String, Integer>();

    public void setRedirectionItem(String url) {
        if(redirectionList.keySet().contains(url)) {
            int count = redirectionList.get(url);
            redirectionList.put(url, ++count);
        } else redirectionList.put(url, 1);
    }

    public ConcurrentHashMap.KeySetView<String, Integer> getAllURLs() {
        return redirectionList.keySet();
    }

    public int getRedirectionCountByURL(String url) {
        return redirectionList.get(url);
    }
}
