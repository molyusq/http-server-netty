package zhuk.andriy.http.server.util;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by Andrew on 01.04.15.
 */
public class RequestCountInfo {
    private int count;
    private Date date;

    public RequestCountInfo(Date date) {
        setCount(1);
        setDate(date);
    }

    public RequestCountInfo() {
        this(new GregorianCalendar().getTime());
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void incrementCount() {
        this.count++;
    }
}
