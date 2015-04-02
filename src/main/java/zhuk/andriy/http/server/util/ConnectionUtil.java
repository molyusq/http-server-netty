package zhuk.andriy.http.server.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Andrew on 01.04.15.
 */
public class ConnectionUtil {
    private ConcurrentHashMap<String, ConnectionInfo> connections = new ConcurrentHashMap<String, ConnectionInfo>();

    public void addConnection(String address, String uri) {
            ConnectionInfo connectionInfo = new ConnectionInfo(uri);
            connections.put(address, connectionInfo);
    }

    public int getConnectionQuantity() {
        return connections.size();
    }

    public ConcurrentHashMap.KeySetView<String, ConnectionInfo> getAllIPs() {
        return connections.keySet();
    }

    public ConnectionInfo getConnectionInfo(String address) {
        return connections.get(address);
    }
}
