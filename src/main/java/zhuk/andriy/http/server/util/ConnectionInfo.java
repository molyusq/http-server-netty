package zhuk.andriy.http.server.util;


import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Andrew on 02.04.15.
 */
public class ConnectionInfo {
    private String uri;
    private Date date;

    public ConnectionInfo(String uri) {
        this.setUri(uri);
        this.setDate(new GregorianCalendar().getTime());
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

    public void setDate(Date date) {
        this.date = date;
    }

}
