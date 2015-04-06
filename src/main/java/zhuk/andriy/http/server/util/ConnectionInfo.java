package zhuk.andriy.http.server.util;


import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Andrew on 02.04.15.
 */
public class ConnectionInfo {
    private long sentBytes;
    private long readBytes;
    private long speed;
    private String address;
    private String uri;
    private Date date;

    public ConnectionInfo(String address) {
        this.setAddress(address);
        this.setDate();
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Date getDate() {
        return date;
    }

    public void setDate() {
        this.date = new GregorianCalendar().getTime();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getSentBytes() {
        return sentBytes;
    }

    public void setSentBytes(long sentBytes) {
        this.sentBytes = sentBytes;
    }

    public long getReadBytes() {
        return readBytes;
    }

    public void setReadBytes(long readBytes) {
        this.readBytes = readBytes;
    }

    public long getSpeed() {
        return speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }
}
