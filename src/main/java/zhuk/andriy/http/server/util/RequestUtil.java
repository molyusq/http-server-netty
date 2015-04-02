package zhuk.andriy.http.server.util;

import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Andrew on 01.04.15.
 */
public class RequestUtil {
    private int totalRequestCount;
    private ConcurrentHashMap<String, RequestCountInfo> requestsPerIPList;

    public RequestUtil() {
        setTotalRequestCount(0);
        requestsPerIPList = new ConcurrentHashMap<String, RequestCountInfo>();
    }


    public int getTotalRequestCount() {
        return totalRequestCount;
    }

    public void incrementTotalRequestCount() {
        this.totalRequestCount++;
    }

    public int getUniqueRequestCount() {
        return requestsPerIPList.size();
    }

    public void setTotalRequestCount(int totalRequestCount) {
        this.totalRequestCount = totalRequestCount;
    }

    public void addRequestInfo(String address) {
        requestsPerIPList.put(address, new RequestCountInfo());
    }

    public void updateRequestInfo(String address) {
        RequestCountInfo requestCountInfo = requestsPerIPList.get(address);
        requestCountInfo.incrementCount();
        requestCountInfo.setDate(new GregorianCalendar().getTime());
        requestsPerIPList.put(address, requestCountInfo);
    }

    public void setRequestInfo(String address) {
        if(requestsPerIPList.keySet().contains(address)) {
            updateRequestInfo(address);
        } else addRequestInfo(address);
    }

    public void deleteRequestInfo(String address) {
        requestsPerIPList.remove(address);
    }

    public ConcurrentHashMap.KeySetView<String, RequestCountInfo> getAllAddresses() {
        return requestsPerIPList.keySet();
    }

    public RequestCountInfo getInfo(String address) {
        return requestsPerIPList.get(address);
    }
}
